package com.lianghao.myapp.jsbridge.plugin;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;

import com.lianghao.myapp.jsbridge.BaseJSPlugin;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * takePhoto：拉起系统相机仅拍照（不录像），压缩后以 base64 回传 H5。
 * 自 ../news 的 com.tencent.tbs.jsbridge.plugin.JsTakePhoto 移植，
 * 契约与错误码见 news 仓 docs/camera_bridge.md（接口契约两仓一致）。
 * 压缩/base64 公共逻辑在 PhotoCodecUtil。
 *
 * 本仓适配差异（对照 news 版）：
 * - 权限申请由 EasyPermissions 改为宿主 WebViewHost.requestRuntimePermissions
 *   （ActivityResult API 通道，本仓宿主未引入 EasyPermissions）；
 * - 拉起相机由 activity.startActivityForResult 改为宿主
 *   WebViewHost.startPluginActivityForResult（本仓宿主用 ActivityResult API）；
 * - FileProvider authorities 改 com.lianghao.myapp.fileprovider。
 *
 * @author lianghao
 * @date 2026/8/20
 */
public class JsTakePhoto extends BaseJSPlugin {

	private static final String TAG = "JsTakePhoto";

	/** Activity 结果码 0x0309（与 news 侧对齐：避开 FILE_CHOOSER=100、0x0300-0x0308；pickPhotos 用 0x030A） */
	private static final int REQUEST_TAKE_PHOTO = 0x0309;
	/** 运行时权限码 0x0019（与 news 侧对齐：避开 0x0010-0x0018） */
	private static final int REQUEST_TAKE_PHOTO_PERMISSION = 0x0019;

	/** 本仓 FileProvider authorities（news 侧为 com.chaychan.news.fileprovider） */
	private static final String PROVIDER_AUTHORITIES = "com.lianghao.myapp.fileprovider";
	private static final String PHOTO_DIR_NAME = "jsbridge_photo";

	private static final int DEFAULT_QUALITY = 70;
	private static final int DEFAULT_MAX_WIDTH = 1280;
	private static final int DEFAULT_MAX_HEIGHT = 1280;
	/** 防 evaluateJavascript 超长：压缩后 base64 必须留在安全区（约 <500KB） */
	private static final int LIMIT_MAX_WIDTH = 4096;
	private static final int LIMIT_MAX_HEIGHT = 4096;

	// 状态字段：宿主 Activity 可能复用插件实例跨"页面会话"残留，
	// jsCallNative 入口必须重置，防止旧回调串扰
	private String mCallbackId;
	private int mQuality;
	private int mMaxWidth;
	private int mMaxHeight;
	private File mPhotoFile;
	private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

	@Override
	public void jsCallNative(String callbackId, String requestParams) {
		// ① 重置状态
		this.mCallbackId = callbackId;
		this.mPhotoFile = null;
		this.mQuality = DEFAULT_QUALITY;
		this.mMaxWidth = DEFAULT_MAX_WIDTH;
		this.mMaxHeight = DEFAULT_MAX_HEIGHT;

		// ② 解析参数：非法值一律回退默认（拍照本身可继续，不因参数笔误 fail）
		try {
			JSONObject p = new JSONObject(requestParams == null ? "{}" : requestParams);
			mQuality = PhotoCodecUtil.clamp(p.optInt("quality", DEFAULT_QUALITY), 10, 100);
			mMaxWidth = PhotoCodecUtil.clamp(p.optInt("maxWidth", DEFAULT_MAX_WIDTH), 0, LIMIT_MAX_WIDTH);
			mMaxHeight = PhotoCodecUtil.clamp(p.optInt("maxHeight", DEFAULT_MAX_HEIGHT), 0, LIMIT_MAX_HEIGHT);
		} catch (Exception e) {
			Log.w(TAG, "bad params, use defaults: " + requestParams);
		}

		// ③ 权限门禁：manifest 已声明 CAMERA，运行时未授予时相机 App 会拒绝回传照片 → 必须先申请
		if (hasCameraPermission()) {
			startCamera();
		} else {
			getHost().registerPermissionCallback(REQUEST_TAKE_PHOTO_PERMISSION, this);
			getHost().requestRuntimePermissions(REQUEST_TAKE_PHOTO_PERMISSION,
					"拍照需要相机权限", android.Manifest.permission.CAMERA);
		}
	}

	private boolean hasCameraPermission() {
		return ContextCompat.checkSelfPermission(getActivity(), android.Manifest.permission.CAMERA)
				== PackageManager.PERMISSION_GRANTED;
	}

	/**
	 * 权限结果。granted/denied 均经宿主 WebViewActivity 的 permissionLauncher 转发；
	 * denied 时宿主仍转发（isGranted=false），本插件据此回 fail code=2。
	 */
	@Override
	public void permissionCallback(int requestCode, @NonNull List<String> perms, boolean isGranted) {
		if (mCallbackId == null) {
			return;   // 防残留状态误触发
		}
		if (isGranted) {
			startCamera();
		} else {
			reportFail(mCallbackId, failJson(2, "camera permission denied"));
		}
	}

	private void startCamera() {
		try {
			File dir = new File(getActivity().getCacheDir(), PHOTO_DIR_NAME);
			if (!dir.exists() && !dir.mkdirs()) {
				reportFail(mCallbackId, failJson(4, "cannot create photo dir"));
				return;
			}
			mPhotoFile = new File(dir, "photo_" + System.currentTimeMillis() + ".jpg");

			// 应用私有 cache 目录经 FileProvider 暴露：零存储权限、不污染相册
			Uri outputUri = FileProvider.getUriForFile(
					getActivity().getApplicationContext(), PROVIDER_AUTHORITIES, mPhotoFile);

			Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);   // 仅拍照，无录像
			takePicture.putExtra(MediaStore.EXTRA_OUTPUT, outputUri);
			takePicture.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION
					| Intent.FLAG_GRANT_READ_URI_PERMISSION);

			getHost().registerResultCallback(REQUEST_TAKE_PHOTO, this);
			getHost().startPluginActivityForResult(takePicture, REQUEST_TAKE_PHOTO);
		} catch (ActivityNotFoundException e) {
			reportFail(mCallbackId, failJson(3, "no camera app available"));
		} catch (Exception e) {
			Log.e(TAG, "startCamera failed", e);
			reportFail(mCallbackId, failJson(4, PhotoCodecUtil.sanitize(e.getMessage())));
		}
	}

	/** 相机结果。宿主 activityResultLauncher 回调经 resultCallbacks 转发到这里。 */
	@Override
	public void resultCallback(int requestCode, int resultCode, @Nullable Intent data) {
		if (mCallbackId == null) {
			return;
		}
		if (resultCode == android.app.Activity.RESULT_OK) {
			final Intent dataRef = data;
			mExecutor.execute(new Runnable() {
				@Override
				public void run() {
					processPhoto(dataRef);   // 解码压缩是耗时 IO → 后台线程；reportXxx 内部自回 UI 线程
				}
			});
		} else {
			// 相机界面按返回键 = RESULT_CANCELED → cancel 态
			reportCancel(mCallbackId, cancelJson("user canceled"));
		}
	}

	private void processPhoto(@Nullable Intent data) {
		try {
			// 兜底：个别相机 App 无视 EXTRA_OUTPUT，只在 data 里回小图
			// （走 EXTRA_OUTPUT 的正规路径 data 为 null，getExtras 也可能为 null，双重判空）
			if (mPhotoFile == null || !mPhotoFile.exists() || mPhotoFile.length() == 0) {
				Bitmap thumb = null;
				if (data != null && data.getExtras() != null) {
					thumb = (Bitmap) data.getExtras().get("data");
				}
				if (thumb == null) {
					reportFail(mCallbackId, failJson(4, "capture failed: empty output"));
					return;
				}
				saveBitmap(thumb, mPhotoFile);
			}

			// ① 边界预读 + inSampleSize 粗缩（内存红线：禁止全尺寸相机原图直接进内存，4000x3000 ≈ 48MB）
			BitmapFactory.Options opts = new BitmapFactory.Options();
			opts.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(mPhotoFile.getAbsolutePath(), opts);
			opts.inSampleSize = PhotoCodecUtil.calcInSampleSize(opts.outWidth, opts.outHeight, mMaxWidth, mMaxHeight);
			opts.inJustDecodeBounds = false;
			Bitmap bitmap = BitmapFactory.decodeFile(mPhotoFile.getAbsolutePath(), opts);

			// ② EXIF 旋转转正（相机按拍摄方向写 EXIF 而非旋转像素）
			bitmap = PhotoCodecUtil.applyRotation(bitmap, readExifRotation(mPhotoFile.getAbsolutePath()));

			// ③ 精确适配框等比缩放（inSampleSize 只有 2 的幂，粗缩后可能仍偏大）
			bitmap = PhotoCodecUtil.scaleToFit(bitmap, mMaxWidth, mMaxHeight);

			// ④ JPEG 压缩 + base64（NO_WRAP/sanitize 等铁律集中在 PhotoCodecUtil）
			PhotoCodecUtil.CompressResult compressed = PhotoCodecUtil.compressToBase64(bitmap, mQuality);

			JSONObject rsp = new JSONObject();
			rsp.put("code", 0);
			rsp.put("base64", compressed.base64);            // 纯 base64，无 dataURL 前缀
			rsp.put("mimeType", "image/jpeg");
			rsp.put("path", mPhotoFile.getAbsolutePath());   // 逃生舱：原生侧需要文件时用
			rsp.put("width", bitmap.getWidth());             // 压缩后尺寸
			rsp.put("height", bitmap.getHeight());
			rsp.put("size", compressed.sizeBytes);
			Log.i(TAG, String.format("photo ready: %dx%d, %d bytes, base64 len=%d",
					bitmap.getWidth(), bitmap.getHeight(), compressed.sizeBytes, compressed.base64.length()));
			reportSuccess(mCallbackId, rsp.toString());
		} catch (Throwable t) {
			Log.e(TAG, "processPhoto failed", t);
			reportFail(mCallbackId, failJson(5, PhotoCodecUtil.sanitize(String.valueOf(t.getMessage()))));
		}
	}

	// ---------------- 私有工具 ----------------

	private int readExifRotation(String path) {
		try {
			ExifInterface exif = new ExifInterface(path);
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
		}
	}

	private void saveBitmap(Bitmap bmp, File file) {
		try {
			FileOutputStream fos = new FileOutputStream(file);
			bmp.compress(Bitmap.CompressFormat.JPEG, 95, fos);
			fos.close();
		} catch (Exception e) {
			Log.e(TAG, "saveBitmap failed", e);
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
}
