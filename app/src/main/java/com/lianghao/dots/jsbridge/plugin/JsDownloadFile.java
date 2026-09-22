package com.lianghao.dots.jsbridge.plugin;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.CookieManager;

import com.lianghao.dots.jsbridge.BaseJSPlugin;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * downloadFile：OkHttp 流式下载服务器文件到系统 Downloads 目录 + 状态栏通知。
 * 契约与错误码同 ../news docs/download_bridge.md（冻结契约，不得偏离）。
 *
 * 无 cancel 态（全程无用户交互，不拉起任何 Activity）；
 * 一个 callbackId 只能回调一次（jsbridge.js 字典一次性消费）。
 *
 * 落盘分版本：API 29+ 走 MediaStore.Downloads（零权限、系统自动重名去重）；
 * API <29 走公共 Downloads 目录 File + 手动去重 + MediaScanner（保底，实际设备均 29+）。
 *
 * 移植自 ../news com.tencent.tbs.jsbridge.plugin.JsDownloadFile（2026-09-19）：
 * - X5 CookieManager → android.webkit.CookieManager（与 pickPhotos/pickAndUploadFiles 同）
 * - 下载地址为内网自签名时 OkHttp 按 host 分流 IntranetTrust（与 WebView SSL 放行策略对齐）
 */
public class JsDownloadFile extends BaseJSPlugin {

	private static final String TAG = "JsDownloadFile";

	private static final String DEFAULT_COOKIE_NAME = "cortex_auth";
	private static final String CHANNEL_ID = "downloads";
	private static final int NOTIFY_ID = 0x3001;

	/**
	 * 下载专用 client：读侧 120s 大文件慢网络余量。
	 * GET 幂等，retryOnConnectionFailure(true)——连接池 stale connection（服务端
	 * keep-alive 超时掐线）时 OkHttp 自动换新连接重试，实测模拟器偶发
	 * "unexpected end of stream" 靠它消除；与上传 client 的禁 retry（POST 非幂等）相反。
	 */
	private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
			.connectTimeout(15, TimeUnit.SECONDS)
			.writeTimeout(30, TimeUnit.SECONDS)
			.readTimeout(120, TimeUnit.SECONDS)
			.retryOnConnectionFailure(true)
			.build();

	private static final OkHttpClient INTRANET_CLIENT = buildIntranetClient();

	private static OkHttpClient buildIntranetClient() {
		javax.net.ssl.X509TrustManager tm = IntranetTrust.trustAllManager();
		javax.net.ssl.SSLSocketFactory sf = IntranetTrust.trustAllFactory(tm);
		OkHttpClient.Builder b = new OkHttpClient.Builder()
				.connectTimeout(15, TimeUnit.SECONDS)
				.writeTimeout(30, TimeUnit.SECONDS)
				.readTimeout(120, TimeUnit.SECONDS)
				.retryOnConnectionFailure(true);
		if (sf != null) {
			b.sslSocketFactory(sf, tm);
			b.hostnameVerifier((hostname, session) -> true); // host 已在调用点限定为内网地址
		}
		return b.build();
	}

	/** 按 host 分流：内网(RFC1918/loopback) → 信任自签名；否则 → 系统信任链。 */
	private static OkHttpClient clientFor(String url) {
		try {
			String host = HttpUrl.parse(url).host();
			return IntranetTrust.isPrivate(host) ? INTRANET_CLIENT : CLIENT;
		} catch (Exception e) {
			return CLIENT;
		}
	}

	// 状态字段：宿主 singleTask 保活会跨"页面会话"残留 —— jsCallNative 入口必须重置
	private String mCallbackId;
	private String mDownloadUrl;
	private String mFileName;        // 可空：Content-Disposition / URL 兜底
	private String mCookieHeader;
	private boolean mBusy;
	private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

	@Override
	public void jsCallNative(String callbackId, String requestParams) {
		// ① busy 检查（重置状态之前：先给旧 callbackId 回 fail 7，不能静默吞掉旧回调）
		if (mBusy && mCallbackId != null) {
			reportFail(mCallbackId, failJson(7, "superseded by new request"));
		}

		// ② 重置状态
		this.mCallbackId = callbackId;
		this.mDownloadUrl = null;
		this.mFileName = "";
		this.mCookieHeader = null;
		this.mBusy = true;

		// ③ 解析参数（downloadUrl 缺失即 fail 1，封装层不兜底——漏传是契约错误应当暴露）
		String cookieName = DEFAULT_COOKIE_NAME;
		try {
			JSONObject p = new JSONObject(requestParams == null ? "{}" : requestParams);
			mDownloadUrl = p.optString("downloadUrl", "").trim();
			mFileName = p.optString("fileName", "").trim();
			String cn = p.optString("cookieName", "").trim();
			if (!cn.isEmpty()) {
				cookieName = cn;
			}
		} catch (Exception e) {
			Log.w(TAG, "bad params: " + requestParams);
		}

		if (mDownloadUrl == null
				|| (!mDownloadUrl.startsWith("http://") && !mDownloadUrl.startsWith("https://"))) {
			mBusy = false;
			reportFail(mCallbackId, failJson(1, "missing or invalid downloadUrl"));
			return;
		}

		// ④ 会话 cookie：系统 WebView CookieManager 与页面共享同一 cookie 域
		mCookieHeader = readCookieHeader(mDownloadUrl, cookieName);

		// ⑤ 网络/写盘全在后台线程（jsCallNative 运行在 UI 线程）
		mExecutor.execute(new Runnable() {
			@Override
			public void run() {
				doDownload();   // reportXxx 内部自回 UI 线程
			}
		});
	}

	// ---------------- 后台线程（executor） ----------------

	private void doDownload() {
		Response response = null;
		try {
			Request.Builder rb = new Request.Builder().url(mDownloadUrl).get();
			if (mCookieHeader != null) {
				rb.header("Cookie", mCookieHeader);
			}
			response = clientFor(mDownloadUrl).newCall(rb.build()).execute();

			int status = response.code();
			if (status == 401) {
				// 401 特殊语义：fail 通道携带 unauthorized 布尔标志（H5 据此跳登录页）
				reportFail(mCallbackId, failJson(5, "UNAUTHORIZED", status, true));
				return;
			}
			if (status < 200 || status >= 300) {
				String body = response.body() == null ? "" : response.body().string();
				JSONObject r = parseJson(body);
				String code = r.optString("code", "");
				String detail = code.isEmpty()
						? ("HTTP_" + status)
						: (code + ": " + r.optString("detail", ""));
				reportFail(mCallbackId, failJson(5, PhotoCodecUtil.sanitize(detail), status, false));
				return;
			}

			// 保存名：参数 fileName 优先 → Content-Disposition → URL 最后段
			String name = resolveFileName(response);

			SaveResult saved = saveToDownloads(response, name);

			boolean notified = notifyDownloaded(saved.finalName, saved.bytes);

			JSONObject rsp = new JSONObject();
			rsp.put("code", 0);
			rsp.put("name", PhotoCodecUtil.sanitize(saved.finalName));
			rsp.put("savedTo", PhotoCodecUtil.sanitize(saved.savedTo));
			rsp.put("bytes", saved.bytes);
			rsp.put("notified", notified);
			Log.i(TAG, String.format("downloaded: %s, %d bytes, notified=%b",
					saved.finalName, saved.bytes, notified));
			reportSuccess(mCallbackId, rsp.toString());
		} catch (java.io.IOException e) {
			Log.e(TAG, "download failed", e);
			// IOException 阶段无从细分网络/写盘（流 copy 中途断既可能是网络也可能是盘），
			// 按契约 §2.4 归 NETWORK_ERROR（最常见成因）
			reportFail(mCallbackId, failJson(5,
					PhotoCodecUtil.sanitize("NETWORK_ERROR: " + e.getMessage()), -1, false));
		} catch (Throwable t) {
			Log.e(TAG, "doDownload failed", t);
			reportFail(mCallbackId, failJson(4, PhotoCodecUtil.sanitize(String.valueOf(t.getMessage()))));
		} finally {
			close(response);
			mBusy = false;
		}
	}

	/**
	 * 保存名解析（契约 §4.3 顺序）：
	 * fileName 参数非空 → Content-Disposition filename*（RFC5987，支持中文）→
	 * 裸 filename="..." → URL path 最后段；全失败 download_<ts> 兜底。
	 * 结果剔除文件系统非法字符（\ / : * ? " < > |）。
	 */
	private String resolveFileName(Response response) {
		String name = mFileName;
		if (name == null || name.isEmpty()) {
			name = parseContentDisposition(response.header("Content-Disposition"));
		}
		if (name == null || name.isEmpty()) {
			try {
				String path = Uri.parse(mDownloadUrl).getPath();
				if (path != null) {
					int slash = path.lastIndexOf('/');
					name = slash >= 0 ? path.substring(slash + 1) : path;
				}
			} catch (Exception ignore) {
			}
		}
		if (name == null || name.isEmpty()) {
			name = "download_" + System.currentTimeMillis();
		}
		return sanitizeFileName(name);
	}

	/** Content-Disposition 解析：filename*=utf-8''...（RFC5987）优先，裸 filename="..." 兜底 */
	private String parseContentDisposition(String header) {
		if (header == null || header.isEmpty()) {
			return null;
		}
		try {
			int star = header.indexOf("filename*=");
			if (star >= 0) {
				String v = header.substring(star + "filename*=".length()).trim();
				// 形如 utf-8''%E6%96%87%E6%A1%A3.pdf 或 "utf-8''..." —— 剥 charset 前缀
				int q = v.indexOf("''");
				if (q >= 0) {
					v = v.substring(q + 2);
				}
				int semi = v.indexOf(';');
				if (semi >= 0) {
					v = v.substring(0, semi);
				}
				return URLDecoder.decode(v.replace("\"", ""), "UTF-8");
			}
			int plain = header.indexOf("filename=");
			if (plain >= 0) {
				String v = header.substring(plain + "filename=".length()).trim();
				int semi = v.indexOf(';');
				if (semi >= 0) {
					v = v.substring(0, semi);
				}
				return v.replace("\"", "");
			}
		} catch (Exception e) {
			Log.w(TAG, "parseContentDisposition failed: " + header);
		}
		return null;
	}

	/** 剔除 Windows/Android 文件名非法字符（服务端文件名本身合法，防御性清洗） */
	private String sanitizeFileName(String name) {
		String cleaned = name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
		return cleaned.isEmpty() ? "download_" + System.currentTimeMillis() : cleaned;
	}

	// ---------------- 落盘（分版本） ----------------

	/** 落盘结果：真实保存名（去重后）+ 展示路径 + 字节数 */
	private static class SaveResult {
		String finalName;
		String savedTo;
		long bytes;
	}

	private SaveResult saveToDownloads(Response response, String name) throws java.io.IOException {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			return saveViaMediaStore(response, name);   // API 29+ 主路径：零权限、系统去重
		}
		return saveViaFile(response, name);             // API <29 保底
	}

	/** API 29+：MediaStore.Downloads IS_PENDING 协议（写毕发布；重名系统自动追加 " (1)"） */
	private SaveResult saveViaMediaStore(Response response, String name) throws java.io.IOException {
		ContentValues cv = new ContentValues();
		cv.put(MediaStore.Downloads.DISPLAY_NAME, name);
		cv.put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream");
		cv.put(MediaStore.Downloads.IS_PENDING, 1);
		Uri outUri = getActivity().getContentResolver()
				.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
		if (outUri == null) {
			throw new java.io.IOException("WRITE_FAILED: MediaStore insert returned null");
		}

		long bytes = 0;
		OutputStream out = null;
		try {
			out = getActivity().getContentResolver().openOutputStream(outUri);
			if (out == null) {
				throw new java.io.IOException("WRITE_FAILED: openOutputStream returned null");
			}
			bytes = copyStream(response.body().byteStream(), out);
		} finally {
			close(out);
		}

		// 发布 + 查回系统去重后的真实 DISPLAY_NAME（savedTo 用 content Uri，H5 仅展示）
		ContentValues done = new ContentValues();
		done.put(MediaStore.Downloads.IS_PENDING, 0);
		getActivity().getContentResolver().update(outUri, done, null, null);

		SaveResult r = new SaveResult();
		r.finalName = queryDisplayName(outUri, name);
		r.savedTo = outUri.toString();
		r.bytes = bytes;
		return r;
	}

	/** API <29：公共 Downloads 目录 File 直写 + 手动 name (1).ext 去重 + MediaScanner 可见化 */
	private SaveResult saveViaFile(Response response, String name) throws java.io.IOException {
		File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
		if (!dir.exists() && !dir.mkdirs()) {
			throw new java.io.IOException("WRITE_FAILED: cannot create Downloads dir");
		}
		File target = uniqueFile(dir, name);

		long bytes = 0;
		InputStream in = null;
		OutputStream out = null;
		try {
			in = response.body().byteStream();
			out = new FileOutputStream(target);
			bytes = copyStream(in, out);
		} finally {
			close(in);
			close(out);
		}
		MediaScannerConnection.scanFile(getActivity(),
				new String[]{target.getAbsolutePath()}, null, null);

		SaveResult r = new SaveResult();
		r.finalName = target.getName();
		r.savedTo = target.getAbsolutePath();
		r.bytes = bytes;
		return r;
	}

	/** 8KB 循环 copy：绝不 body().bytes() 整读（50MB 文档不 OOM） */
	private long copyStream(InputStream in, OutputStream out) throws java.io.IOException {
		byte[] buf = new byte[8192];
		long total = 0;
		int n;
		while ((n = in.read(buf)) > 0) {
			out.write(buf, 0, n);
			total += n;
		}
		return total;
	}

	/** 重名去重：name (1).ext、name (2).ext …（MediaStore 路径不需要，系统自动处理） */
	private File uniqueFile(File dir, String name) {
		File f = new File(dir, name);
		if (!f.exists()) {
			return f;
		}
		String base = name;
		String ext = "";
		int dot = name.lastIndexOf('.');
		if (dot > 0) {
			base = name.substring(0, dot);
			ext = name.substring(dot);
		}
		for (int i = 1; i < 1000; i++) {
			f = new File(dir, base + " (" + i + ")" + ext);
			if (!f.exists()) {
				return f;
			}
		}
		return new File(dir, base + "_" + System.currentTimeMillis() + ext);
	}

	/** 查回 MediaStore 去重后的真实 DISPLAY_NAME（查不到回退原名，不影响下载本体） */
	private String queryDisplayName(Uri uri, String fallback) {
		android.database.Cursor c = null;
		try {
			c = getActivity().getContentResolver()
					.query(uri, null, null, null, null);
			if (c != null && c.moveToFirst()) {
				int idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
				if (idx >= 0 && c.getString(idx) != null) {
					return c.getString(idx);
				}
			}
		} catch (Exception e) {
			Log.w(TAG, "queryDisplayName failed", e);
		} finally {
			if (c != null) {
				try {
					c.close();
				} catch (Exception ignore) {
				}
			}
		}
		return fallback;
	}

	// ---------------- 通知 ----------------

	/** 状态栏通知（渠道 "downloads" IMPORTANCE_LOW）；失败仅返回 false，不算下载失败 */
	private boolean notifyDownloaded(String name, long bytes) {
		try {
			Context ctx = getActivity();
			NotificationManager nm =
					(NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
			if (nm == null) {
				return false;
			}
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				nm.createNotificationChannel(new NotificationChannel(
						CHANNEL_ID, "文件下载", NotificationManager.IMPORTANCE_LOW));
			}
			Notification.Builder b = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
					? new Notification.Builder(ctx, CHANNEL_ID)
					: new Notification.Builder(ctx);
			b.setSmallIcon(android.R.drawable.stat_sys_download_done)
					.setContentTitle("下载完成")
					.setContentText(name + " (" + bytes + " B) 已保存到下载目录")
					.setAutoCancel(true);
			nm.notify(NOTIFY_ID, b.build());
			return true;
		} catch (Throwable t) {
			// 通知权限未授予（API 33+ 未声明 POST_NOTIFICATIONS）等场景：notified:false，文件仍保存成功
			Log.w(TAG, "notify failed: " + t);
			return false;
		}
	}

	// ---------------- 私有工具 ----------------

	/**
	 * 从系统 WebView CookieManager 读会话 cookie（与页面共享同一 cookie 域）。
	 * 命中 → "name=value"；null/未命中 → null（不发 Cookie 头）。
	 */
	private String readCookieHeader(String url, String cookieName) {
		try {
			String all = CookieManager.getInstance().getCookie(url);
			if (all == null || all.isEmpty()) {
				return null;
			}
			String prefix = cookieName.toLowerCase() + "=";
			String[] parts = all.split("; ");
			for (String part : parts) {
				if (part.toLowerCase().startsWith(prefix)) {
					return part;
				}
			}
		} catch (Exception e) {
			Log.w(TAG, "readCookieHeader failed", e);
		}
		return null;
	}

	private static JSONObject parseJson(String s) {
		try {
			return new JSONObject(s == null ? "" : s);
		} catch (Exception e) {
			return new JSONObject();
		}
	}

	private void close(Response response) {
		if (response != null) {
			try {
				response.close();
			} catch (Exception ignore) {
			}
		}
	}

	private void close(java.io.Closeable c) {
		if (c != null) {
			try {
				c.close();
			} catch (Exception ignore) {
			}
		}
	}

	private String failJson(int code, String message) {
		return failJson(code, message, -1, false);
	}

	/** fail payload：附 httpStatus（服务端有响应时）与 unauthorized（401 特殊语义） */
	private String failJson(int code, String message, int httpStatus, boolean unauthorized) {
		try {
			JSONObject j = new JSONObject();
			j.put("code", code);
			j.put("error", PhotoCodecUtil.sanitize(message));
			j.put("detail", PhotoCodecUtil.sanitize(message));
			if (httpStatus > 0) {
				j.put("httpStatus", httpStatus);
			}
			if (unauthorized) {
				j.put("unauthorized", true);
			}
			return j.toString();
		} catch (Exception e) {
			return "{\"code\":5,\"error\":\"json build failed\"}";
		}
	}
}
