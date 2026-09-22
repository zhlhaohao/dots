package com.lianghao.dots.jsbridge.plugin;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.exifinterface.media.ExifInterface;

import com.lianghao.dots.jsbridge.BaseJSPlugin;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * pickPhotos：拉起系统相册选 1..maxCount 张照片，压缩后以 base64 数组一次性回传 H5。
 * 自 ../news 的 com.tencent.tbs.jsbridge.plugin.JsPickPhotos 移植，
 * 契约与错误码见 news 仓 docs/pickphotos_bridge.md（接口契约两仓一致）。
 * 压缩/base64 公共逻辑在 PhotoCodecUtil。
 *
 * 本仓适配差异（对照 news 版）：
 * - 拉起选择器由 activity.startActivityForResult 改为宿主
 *   WebViewHost.startPluginActivityForResult（本仓宿主用 ActivityResult API）；
 * - 结果转发经 WebViewHost.registerResultCallback 注册（语义同 news 宿主 resultCallbackMap）。
 *
 * 框架限制：一个 callbackId 只能回调一次（jsbridge.js 字典一次性消费），
 * 因此多张照片必须拼进同一次 success 回调，不做逐张推送。
 *
 * @author lianghao
 * @date 2026/8/20
 */
public class JsPickPhotos extends BaseJSPlugin {

	private static final String TAG = "JsPickPhotos";

	/** Activity 结果码 0x030A（0x0309=takePhoto；避开 FILE_CHOOSER=100、zxing=49374） */
	private static final int REQUEST_PICK_PHOTOS = 0x030A;

	private static final int DEFAULT_QUALITY = 70;
	// 多张叠加控总量：默认 1080（比 takePhoto 的 1280 少约 29% 像素）
	private static final int DEFAULT_MAX_WIDTH = 1080;
	private static final int DEFAULT_MAX_HEIGHT = 1080;
	private static final int LIMIT_MAX_WIDTH = 4096;
	private static final int LIMIT_MAX_HEIGHT = 4096;
	/** 无参 = 单选（与 takePhoto 语义对齐），多选是能力不是默认 */
	private static final int DEFAULT_MAX_COUNT = 1;
	private static final int LIMIT_MAX_COUNT = 9;
	/** 防 evaluateJavascript 超长的总预算（base64 字符数累计） */
	private static final long TOTAL_BASE64_BUDGET = 2_000_000L;

	// 状态字段：宿主 Activity 可能复用插件实例跨"页面会话"残留 —— jsCallNative 入口必须重置
	private String mCallbackId;
	private int mQuality;
	private int mMaxWidth;
	private int mMaxHeight;
	private int mMaxCount;
	private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

	@Override
	public void jsCallNative(String callbackId, String requestParams) {
		// ① 重置状态
		this.mCallbackId = callbackId;
		this.mQuality = DEFAULT_QUALITY;
		this.mMaxWidth = DEFAULT_MAX_WIDTH;
		this.mMaxHeight = DEFAULT_MAX_HEIGHT;
		this.mMaxCount = DEFAULT_MAX_COUNT;

		// ② 解析参数：非法值回退默认（选图本身可继续，不因参数笔误 fail）
		try {
			JSONObject p = new JSONObject(requestParams == null ? "{}" : requestParams);
			mQuality = PhotoCodecUtil.clamp(p.optInt("quality", DEFAULT_QUALITY), 10, 100);
			mMaxWidth = PhotoCodecUtil.clamp(p.optInt("maxWidth", DEFAULT_MAX_WIDTH), 0, LIMIT_MAX_WIDTH);
			mMaxHeight = PhotoCodecUtil.clamp(p.optInt("maxHeight", DEFAULT_MAX_HEIGHT), 0, LIMIT_MAX_HEIGHT);
			mMaxCount = PhotoCodecUtil.clamp(p.optInt("maxCount", DEFAULT_MAX_COUNT), 1, LIMIT_MAX_COUNT);
		} catch (Exception e) {
			Log.w(TAG, "bad params, use defaults: " + requestParams);
		}

		// ③ 零权限：系统选择器代选，返回的 content Uri 自带临时读授权，直接拉起
		startPicker();
	}

	private void startPicker() {
		try {
			Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
			intent.addCategory(Intent.CATEGORY_DEFAULT);
			intent.setType("image/*");
			if (mMaxCount > 1) {
				intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
			}
			// 不包 createChooser：image/* 直接落默认相册，包一层反而多一个弹窗

			getHost().registerResultCallback(REQUEST_PICK_PHOTOS, this);
			getHost().startPluginActivityForResult(intent, REQUEST_PICK_PHOTOS);
		} catch (ActivityNotFoundException e) {
			reportFail(mCallbackId, failJson(3, "no gallery app available"));
		} catch (Exception e) {
			Log.e(TAG, "startPicker failed", e);
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
			reportCancel(mCallbackId, cancelJson("user canceled"));
			return;
		}
		List<Uri> uris = collectUris(data);
		if (uris.isEmpty()) {
			reportFail(mCallbackId, failJson(4, "picker returned no photo"));
			return;
		}
		int pickedCount = uris.size();
		boolean truncated = false;
		if (pickedCount > mMaxCount) {
			uris = new ArrayList<>(uris.subList(0, mMaxCount));   // 截断取前 N 张，不静默丢弃（payload 带 truncated）
			truncated = true;
		}
		final List<Uri> uriList = uris;
		final int finalPicked = pickedCount;
		final boolean finalTruncated = truncated;
		mExecutor.execute(new Runnable() {
			@Override
			public void run() {
				processPhotos(uriList, finalPicked, finalTruncated);   // IO 后台；reportXxx 内部自回 UI 线程
			}
		});
	}

	/** 多选解析：ClipData 优先遍历，getData() 单选兜底（与 news 版一致） */
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

	private void processPhotos(List<Uri> uris, int pickedCount, boolean truncated) {
		JSONArray photos = new JSONArray();
		int okCount = 0;
		long totalBase64 = 0;
		try {
			for (Uri uri : uris) {
				// 总量守卫：累计 base64 超预算即停止追加（只拦极端参数组合，默认参数不触发）
				if (totalBase64 >= TOTAL_BASE64_BUDGET) {
					photos.put(errItem("size budget exceeded"));
					continue;
				}
				try {
					JSONObject item = processOne(uri);
					totalBase64 += item.getLong("size") * 4L / 3L;   // 粗估 base64 字符数
					photos.put(item);
					okCount++;
				} catch (Throwable t) {
					Log.e(TAG, "processOne failed: " + uri, t);
					photos.put(errItem(PhotoCodecUtil.sanitize(String.valueOf(t.getMessage()))));
				}
			}

			if (okCount == 0) {
				reportFail(mCallbackId, failJson(5, "all photos failed"));
				return;
			}

			JSONObject rsp = new JSONObject();
			rsp.put("code", 0);
			rsp.put("count", okCount);                 // 成功张数
			rsp.put("pickedCount", pickedCount);       // 用户实际选择的张数（截断前）
			rsp.put("truncated", truncated);           // true = 选择超 maxCount 已截断
			rsp.put("photos", photos);
			Log.i(TAG, String.format("picked %d/%d ok, truncated=%b", okCount, pickedCount, truncated));
			reportSuccess(mCallbackId, rsp.toString());
		} catch (Throwable t) {
			Log.e(TAG, "processPhotos failed", t);
			reportFail(mCallbackId, failJson(5, PhotoCodecUtil.sanitize(String.valueOf(t.getMessage()))));
		}
	}

	/** 单张处理：content Uri 三次开流（bounds 预读 → EXIF → 解码），失败抛异常由调用方落 error 项 */
	private JSONObject processOne(Uri uri) throws Exception {
		// ① 边界预读 + inSampleSize 粗缩（内存红线同 takePhoto）
		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inJustDecodeBounds = true;
		InputStream boundsIn = getActivity().getContentResolver().openInputStream(uri);
		try {
			BitmapFactory.decodeStream(boundsIn, null, opts);
		} finally {
			close(boundsIn);
		}
		opts.inSampleSize = PhotoCodecUtil.calcInSampleSize(opts.outWidth, opts.outHeight, mMaxWidth, mMaxHeight);
		opts.inJustDecodeBounds = false;

		// ② 解码粗缩位图
		Bitmap bitmap;
		InputStream decodeIn = getActivity().getContentResolver().openInputStream(uri);
		try {
			bitmap = BitmapFactory.decodeStream(decodeIn, null, opts);
		} finally {
			close(decodeIn);
		}
		if (bitmap == null) {
			throw new Exception("decode failed");   // HEIC 老设备 / 损坏文件
		}

		// ③ EXIF 转正（相册里的相机横拍图有旋转）→ 适配框精缩
		bitmap = PhotoCodecUtil.applyRotation(bitmap, readExifRotation(uri));
		bitmap = PhotoCodecUtil.scaleToFit(bitmap, mMaxWidth, mMaxHeight);

		// ④ 压缩 + base64（NO_WRAP 铁律在 PhotoCodecUtil）
		PhotoCodecUtil.CompressResult compressed = PhotoCodecUtil.compressToBase64(bitmap, mQuality);

		JSONObject item = new JSONObject();
		item.put("base64", compressed.base64);
		item.put("mimeType", "image/jpeg");          // 统一重编码为 JPEG
		item.put("width", bitmap.getWidth());        // 压缩后尺寸
		item.put("height", bitmap.getHeight());
		item.put("size", compressed.sizeBytes);
		bitmap.recycle();
		return item;
	}

	/** content Uri 无文件路径 → 用 androidx ExifInterface 的 InputStream 重载 */
	private int readExifRotation(Uri uri) {
		InputStream in = null;
		try {
			in = getActivity().getContentResolver().openInputStream(uri);
			if (in == null) {
				return 0;
			}
			ExifInterface exif = new ExifInterface(in);
			int orientation = exif.getAttributeInt(
					ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
			switch (orientation) {
				case ExifInterface.ORIENTATION_ROTATE_90: return 90;
				case ExifInterface.ORIENTATION_ROTATE_180: return 180;
				case ExifInterface.ORIENTATION_ROTATE_270: return 270;
				default: return 0;
			}
		} catch (Exception e) {
			return 0;
		} finally {
			close(in);
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

	private JSONObject errItem(String message) {
		JSONObject j = new JSONObject();
		try {
			j.put("error", PhotoCodecUtil.sanitize(message));
		} catch (Exception ignore) {
		}
		return j;
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
}
