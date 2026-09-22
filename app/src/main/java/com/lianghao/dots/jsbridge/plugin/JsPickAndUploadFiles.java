package com.lianghao.dots.jsbridge.plugin;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;
import android.webkit.CookieManager;

import androidx.annotation.Nullable;

import com.lianghao.dots.jsbridge.BaseJSPlugin;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.HttpUrl;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;

/**
 * pickAndUploadFiles：拉起系统文件选择器（多选），OkHttp 直接 multipart
 * 上传到 uploadUrl，聚合结果单次 success 回调。
 * 契约与错误码见 news 仓 docs/upload_bridge.md（冻结契约，不得偏离）。
 *
 * 与姊妹接口 pickPhotos 的刻意差异：全部文件失败也走 success 聚合回调
 * （unauthorized 标志和逐文件 code 必须到达 H5，fail 通道没有 results 结构）；
 * 插件级 fail 只用于"根本没走到上传"的场景。
 *
 * 框架限制：一个 callbackId 只能回调一次（jsbridge.js 字典一次性消费），
 * 多文件结果必须聚合进同一次 success 回调，无逐文件进度。
 *
 * 自 ../news 的 com.tencent.tbs.jsbridge.plugin.JsPickAndUploadFiles 移植。
 * 本仓适配差异（对照 news 版）：
 * - 包名/基类：com.tencent.tbs.jsbridge → com.lianghao.dots.jsbridge；
 * - 拉起选择器由 activity.startActivityForResult 改为宿主
 *   WebViewHost.startPluginActivityForResult，结果经 registerResultCallback
 *   注册转发（本仓宿主用 ActivityResult API）；
 * - Cookie 读取由 X5 com.tencent.smtt.sdk.CookieManager 改为系统
 *   android.webkit.CookieManager（news 注释自证：X5 关闭时包装器即系统 CookieManager，
 *   与页面共享同一 cookie 域，语义等价）。
 *
 * @author lianghao
 * @date 2026/8/24
 */
public class JsPickAndUploadFiles extends BaseJSPlugin {

	private static final String TAG = "JsPickAndUploadFiles";

	/** Activity 结果码 0x030B（0x0309=takePhoto、0x030A=pickPhotos；避开 FILE_CHOOSER=100、zxing=49374） */
	private static final int REQUEST_PICK_UPLOAD = 0x030B;

	private static final int DEFAULT_MAX_COUNT = 9;
	private static final int LIMIT_MAX_COUNT = 20;
	/** 与服务端 _MAX_UPLOAD_BYTES 对齐（doclens web_v2/api/files.py）：超限本地合成 CONTENT_TOO_LARGE，不发 HTTP */
	private static final long MAX_FILE_BYTES = 50L * 1024 * 1024;
	/** 会话 cookie 名：doclens web_v2/auth_gate.py COOKIE_NAME */
	private static final String DEFAULT_COOKIE_NAME = "cortex_auth";

	/** 服务端白名单对应的 MIME 尽力过滤（多数选择器忽略 EXTRA_MIME_TYPES，非硬约束） */
	private static final String[] FILTER_MIME_TYPES = new String[]{
			"text/*", "image/*",
			"application/pdf",
			"application/msword",
			"application/vnd.ms-powerpoint",
			"application/vnd.ms-excel",
			"application/vnd.ms-excel.sheet.macroenabled.12",
			"application/vnd.openxmlformats-officedocument.wordprocessingml.document",
			"application/vnd.openxmlformats-officedocument.presentationml.presentation",
			"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
			"application/rtf",
			"application/epub+zip"
	};

	/** 串行上传专用 client：禁 retry 防重传（POST 非幂等，断线重发会重复写盘）。
	 *  内网上传地址是自签名证书 → 用 trust-all client（与 WebView SSL 放行策略对齐，docs/adr/0004）；
	 *  公网地址仍走系统默认信任链。按 uploadUrl host 分流在 uploadOne 里做。 */
	private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
			.connectTimeout(15, TimeUnit.SECONDS)
			.writeTimeout(180, TimeUnit.SECONDS)   // 50MB 慢 WiFi 余量
			.readTimeout(60, TimeUnit.SECONDS)
			.retryOnConnectionFailure(false)
			.build();

	private static final OkHttpClient INTRANET_CLIENT = buildIntranetClient();

	private static OkHttpClient buildIntranetClient() {
		X509TrustManager tm = IntranetTrust.trustAllManager();
		SSLSocketFactory sf = IntranetTrust.trustAllFactory(tm);
		OkHttpClient.Builder b = new OkHttpClient.Builder()
				.connectTimeout(15, TimeUnit.SECONDS)
				.writeTimeout(180, TimeUnit.SECONDS)
				.readTimeout(60, TimeUnit.SECONDS)
				.retryOnConnectionFailure(false);
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

	// 状态字段：宿主 Activity 可能复用插件实例跨"页面会话"残留 —— jsCallNative 入口必须重置
	private String mCallbackId;
	private String mUploadUrl;
	private String mDestDir;
	private String mCookieHeader;      // "cortex_auth=xxx" 或 null（无则不发 Cookie 头）
	private boolean mOverwrite;
	private int mMaxCount;
	private boolean mBusy;             // 上一次调用在途（等选择器/上传中）
	private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

	@Override
	public void jsCallNative(String callbackId, String requestParams) {
		// ① busy 检查（重置状态之前：先给旧 callbackId 回 fail 7，不能静默吞掉旧回调）
		if (mBusy && mCallbackId != null) {
			reportFail(mCallbackId, failJson(7, "superseded by new request"));
		}

		// ② 重置状态
		this.mCallbackId = callbackId;
		this.mUploadUrl = null;
		this.mDestDir = "";
		this.mCookieHeader = null;
		this.mOverwrite = false;
		this.mMaxCount = DEFAULT_MAX_COUNT;
		this.mBusy = true;

		// ③ 解析参数（uploadUrl 缺失即 fail 1，封装层不兜底——漏传是契约错误应当暴露）
		String cookieName = DEFAULT_COOKIE_NAME;
		try {
			JSONObject p = new JSONObject(requestParams == null ? "{}" : requestParams);
			mUploadUrl = p.optString("uploadUrl", "").trim();
			mDestDir = p.optString("destDir", "");
			mOverwrite = p.optBoolean("overwrite", false);
			mMaxCount = PhotoCodecUtil.clamp(p.optInt("maxCount", DEFAULT_MAX_COUNT), 1, LIMIT_MAX_COUNT);
			String cn = p.optString("cookieName", "").trim();
			if (!cn.isEmpty()) {
				cookieName = cn;
			}
		} catch (Exception e) {
			Log.w(TAG, "bad params: " + requestParams);
		}

		if (mUploadUrl == null
				|| (!mUploadUrl.startsWith("http://") && !mUploadUrl.startsWith("https://"))) {
			mBusy = false;
			reportFail(mCallbackId, failJson(1, "missing or invalid uploadUrl"));
			return;
		}

		// ④ 会话 cookie：系统 CookieManager 与 WebView 页面共享同一 cookie 域
		mCookieHeader = readCookieHeader(mUploadUrl, cookieName);

		// ⑤ 零权限：系统选择器代选，content Uri 自带临时读授权，直接拉起
		startPicker();
	}

	private void startPicker() {
		try {
			Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
			intent.addCategory(Intent.CATEGORY_DEFAULT);
			intent.setType("*/*");
			// 无条件多选（契约）：maxCount 语义靠截断实现，不由选择器 UI 决定
			intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
			intent.putExtra(Intent.EXTRA_MIME_TYPES, FILTER_MIME_TYPES);

			getHost().registerResultCallback(REQUEST_PICK_UPLOAD, this);
			getHost().startPluginActivityForResult(intent, REQUEST_PICK_UPLOAD);
		} catch (ActivityNotFoundException e) {
			mBusy = false;
			reportFail(mCallbackId, failJson(3, "no file picker app available"));
		} catch (Exception e) {
			Log.e(TAG, "startPicker failed", e);
			mBusy = false;
			reportFail(mCallbackId, failJson(4, PhotoCodecUtil.sanitize(e.getMessage())));
		}
	}

	/** 选择器结果。WebViewHost.onActivityResult 经注册表优先转发到这里。 */
	@Override
	public void resultCallback(int requestCode, int resultCode, @Nullable Intent data) {
		if (mCallbackId == null) {
			return;
		}
		if (resultCode != android.app.Activity.RESULT_OK) {
			mBusy = false;
			reportCancel(mCallbackId, cancelJson("user canceled"));
			return;
		}
		List<Uri> uris = collectUris(data);
		if (uris.isEmpty()) {
			mBusy = false;
			reportFail(mCallbackId, failJson(4, "picker returned no file"));
			return;
		}
		int pickedCount = uris.size();
		boolean truncated = false;
		if (pickedCount > mMaxCount) {
			uris = new ArrayList<>(uris.subList(0, mMaxCount));   // 截断取前 N 个（语义同 pickPhotos）
			truncated = true;
		}
		final List<Uri> uriList = uris;
		final int finalPicked = pickedCount;
		final boolean finalTruncated = truncated;
		mExecutor.execute(new Runnable() {
			@Override
			public void run() {
				uploadAll(uriList, finalPicked, finalTruncated);   // 网络/IO 后台；reportXxx 内部自回 UI 线程
			}
		});
	}

	/** 多选解析：ClipData 优先遍历，getData() 单选兜底（范本 JsPickPhotos） */
	private List<Uri> collectUris(@Nullable Intent data) {
		List<Uri> uris = new ArrayList<>();
		if (data == null) {
			return uris;
		}
		if (data.getClipData() != null) {
			int count = data.getClipData().getItemCount();
			for (int i = 0; i < count; i++) {
				uris.add(data.getClipData().getItemAt(i).getUri());
			}
		} else if (data.getData() != null) {
			uris.add(data.getData());
		}
		return uris;
	}

	// ---------------- 后台线程（executor） ----------------

	private void uploadAll(List<Uri> uris, int pickedCount, boolean truncated) {
		JSONArray results = new JSONArray();
		int uploadedCount = 0;
		int skippedCount = 0;
		int failedCount = 0;
		boolean unauthorized = false;
		try {
			// 串行逐文件（与 Web 端 files-view 行为一致，避免并行竞争 ALREADY_EXISTS）
			for (Uri uri : uris) {
				FileMeta meta = queryFileMeta(uri);
				JSONObject item;
				if (unauthorized) {
					// 401 已发生：剩余项不发 HTTP，直接补 UNAUTHORIZED 中止项
					item = errItem(meta.name, "UNAUTHORIZED", "session expired, aborted");
				} else if (meta.size > MAX_FILE_BYTES) {
					// 原生预检超限：不发起 HTTP，本地合成同码项（省 50MB 白传带宽）
					item = errItem(meta.name, "CONTENT_TOO_LARGE",
							"file exceeds 50MB limit: " + meta.size + " bytes");
				} else {
					item = uploadOne(uri, meta);
				}
				results.put(item);
				if (item.optBoolean("ok")) {
					uploadedCount++;
				} else if ("ALREADY_EXISTS".equals(item.optString("code"))) {
					skippedCount++;               // 重名跳过不算失败（与 Web 端语义对齐）
				} else {
					failedCount++;
					if ("UNAUTHORIZED".equals(item.optString("code"))) {
						unauthorized = true;       // 剩余文件立即中止
					}
				}
			}

			JSONObject rsp = new JSONObject();
			rsp.put("code", 0);
			rsp.put("pickedCount", pickedCount);
			rsp.put("truncated", truncated);
			rsp.put("uploadedCount", uploadedCount);
			rsp.put("skippedCount", skippedCount);
			rsp.put("failedCount", failedCount);
			rsp.put("unauthorized", unauthorized);
			rsp.put("results", results);
			Log.i(TAG, String.format("upload done: %d/%d ok, skipped=%d, failed=%d, unauthorized=%b",
					uploadedCount, pickedCount, skippedCount, failedCount, unauthorized));
			reportSuccess(mCallbackId, rsp.toString());
		} catch (Throwable t) {
			Log.e(TAG, "uploadAll failed", t);
			reportFail(mCallbackId, failJson(5, PhotoCodecUtil.sanitize(String.valueOf(t.getMessage()))));
		} finally {
			mBusy = false;
		}
	}

	/** 单文件上传：multipart 三字段（file/dest_dir/overwrite），结果落 ok 或 {code,detail} 项 */
	private JSONObject uploadOne(Uri uri, FileMeta meta) {
		Response response = null;
		try {
			RequestBody body = new MultipartBody.Builder()
					.setType(MultipartBody.FORM)
					.addFormDataPart("file", meta.name, streamBody(uri, meta.size))
					.addFormDataPart("dest_dir", mDestDir)
					.addFormDataPart("overwrite", mOverwrite ? "true" : "false")
					.build();

			Request.Builder rb = new Request.Builder().url(mUploadUrl).post(body);
			if (mCookieHeader != null) {
				rb.header("Cookie", mCookieHeader);
			}
			response = clientFor(mUploadUrl).newCall(rb.build()).execute();

			int status = response.code();
			if (status == 401) {
				return errItem(meta.name, "UNAUTHORIZED", "session expired");
			}
			String respBody = response.body() == null ? "" : response.body().string();
			if (status >= 200 && status < 300) {
				JSONObject r = parseJson(respBody);
				JSONObject item = new JSONObject();
				item.put("name", meta.name);
				item.put("ok", true);
				item.put("path", r.optString("path", ""));                       // 服务端返回的相对路径
				item.put("bytes_written", r.optLong("bytes_written", 0L));
				item.put("overwritten", r.optBoolean("overwritten", false));
				return item;
			}
			// 其他非 2xx：透传服务端 {code, detail}；解析失败合成 HTTP_<status>
			JSONObject r = parseJson(respBody);
			String code = r.optString("code", "");
			if (code.isEmpty()) {
				code = "HTTP_" + status;
			}
			Log.w(TAG, "uploadOne failed: " + meta.name + " -> " + status + " " + code + ": "
					+ r.optString("detail", "http " + status));
			return errItem(meta.name, code, r.optString("detail", "http " + status));
		} catch (java.io.IOException e) {
			// ContentResolver 读流失败（授权回收/文件被删）与网络失败分开报
			Throwable cause = e.getCause();
			if (cause instanceof android.content.ActivityNotFoundException
					|| (cause != null && cause.getMessage() != null
						&& cause.getMessage().contains("openInputStream"))) {
				return errItem(meta.name, "READ_FAILED", PhotoCodecUtil.sanitize(String.valueOf(cause.getMessage())));
			}
			return errItem(meta.name, "NETWORK_ERROR", PhotoCodecUtil.sanitize(String.valueOf(e.getMessage())));
		} catch (Throwable t) {
			Log.e(TAG, "uploadOne failed: " + meta.name, t);
			return errItem(meta.name, "READ_FAILED", PhotoCodecUtil.sanitize(String.valueOf(t.getMessage())));
		} finally {
			close(response);
		}
	}

	/** 流式 RequestBody：ContentResolver 直读，绝不把文件读进 byte[]（50MB 不 OOM） */
	private RequestBody streamBody(final Uri uri, final long sizeBytes) {
		return new RequestBody() {
			@Override
			public MediaType contentType() {
				// 服务端只看文件名后缀，不校验 MIME
				return MediaType.parse("application/octet-stream");
			}

			@Override
			public long contentLength() {
				// OpenableColumns.SIZE；-1 时 OkHttp 自动走 chunked（现代选择器均报 SIZE，不落盘临时文件）
				return sizeBytes;
			}

			@Override
			public void writeTo(BufferedSink sink) throws java.io.IOException {
				InputStream in = null;
				try {
					in = getActivity().getContentResolver().openInputStream(uri);
					if (in == null) {
						throw new java.io.IOException("openInputStream returned null: " + uri);
					}
					sink.writeAll(Okio.source(in));
				} finally {
					close(in);
				}
			}
		};
	}

	// ---------------- 私有工具 ----------------

	/** 文件元信息：DISPLAY_NAME/SIZE（Cursor），name 兜底 lastPathSegment，size 兜底 -1（chunked） */
	private FileMeta queryFileMeta(Uri uri) {
		FileMeta meta = new FileMeta();
		meta.name = uri.getLastPathSegment() == null ? "file" : uri.getLastPathSegment();
		meta.size = -1L;
		Cursor cursor = null;
		try {
			cursor = getActivity().getContentResolver().query(uri, null, null, null, null);
			if (cursor != null && cursor.moveToFirst()) {
				int nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
				if (nameIdx >= 0 && cursor.getString(nameIdx) != null) {
					meta.name = cursor.getString(nameIdx);
				}
				int sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE);
				if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
					meta.size = cursor.getLong(sizeIdx);
				}
			}
		} catch (Exception e) {
			Log.w(TAG, "queryFileMeta failed: " + uri);
		} finally {
			if (cursor != null) {
				try {
					cursor.close();
				} catch (Exception ignore) {
				}
			}
		}
		return meta;
	}

	/**
	 * 从系统 CookieManager 读会话 cookie（与 WebView 页面共享同一 cookie 域）。
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

	private JSONObject errItem(String name, String code, String detail) {
		JSONObject j = new JSONObject();
		try {
			j.put("name", PhotoCodecUtil.sanitize(name));
			j.put("ok", false);
			j.put("code", code);
			j.put("detail", PhotoCodecUtil.sanitize(detail));
		} catch (Exception ignore) {
		}
		return j;
	}

	private void close(Response response) {
		if (response != null) {
			try {
				response.close();
			} catch (Exception ignore) {
			}
		}
	}

	private void close(InputStream in) {
		if (in != null) {
			try {
				in.close();
			} catch (Exception ignore) {
			}
		}
	}

	private String failJson(int code, String message) {
		try {
			JSONObject j = new JSONObject();
			j.put("code", code);
			j.put("error", PhotoCodecUtil.sanitize(message));
			return j.toString();
		} catch (Exception e) {
			return "{\"code\":5,\"error\":\"json build failed\"}";
		}
	}

	private String cancelJson(String reason) {
		try {
			JSONObject j = new JSONObject();
			j.put("reason", PhotoCodecUtil.sanitize(reason));
			return j.toString();
		} catch (Exception e) {
			return "{\"reason\":\"user canceled\"}";
		}
	}

	/** 选择器返回的文件名/大小（SIZE=-1 表示未知，上传走 chunked） */
	private static class FileMeta {
		String name;
		long size;
	}
}
