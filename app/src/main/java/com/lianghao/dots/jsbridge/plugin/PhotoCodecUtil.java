package com.lianghao.dots.jsbridge.plugin;

import android.graphics.Bitmap;
import android.util.Base64;

import java.io.ByteArrayOutputStream;

/**
 * 照片压缩/base64 编码静态工具，供 takePhoto / pickPhotos 等 JSBridge 图片插件共用。
 * 自 ../news 的 com.tencent.tbs.jsbridge.plugin.PhotoCodecUtil 移植（纯静态工具，无适配差异）。
 * 无状态纯函数，调用方负责 Bitmap 生命周期（recycle）。
 *
 * 三条铁律（改动前必读）：
 * 1. base64 必须 NO_WRAP —— 默认 76 字符换行，\n 会破坏 JSBridge.callbackJS 拼接出的
 *    单行 JS 字符串字面量，回调静默丢失且无任何报错。
 * 2. 所有进入回调 JSON 的字符串必须过 sanitize —— 原生回调把参数拼进单引号 JS 字面量，
 *    单引号会炸语法。
 * 3. 解码必须两段式（inSampleSize 粗缩 + 精确缩放）—— 全尺寸相机原图 ≈ 48MB 位图，OOM 红线。
 *
 * @author lianghao
 * @date 2026/8/20
 */
final class PhotoCodecUtil {

	private PhotoCodecUtil() {
	}

	/** JPEG 压缩 + base64 编码结果 */
	static class CompressResult {
		final String base64;     // 纯 base64，无 dataURL 前缀、无换行（NO_WRAP）
		final int sizeBytes;     // JPEG 字节数

		CompressResult(String base64, int sizeBytes) {
			this.base64 = base64;
			this.sizeBytes = sizeBytes;
		}
	}

	static int clamp(int v, int min, int max) {
		return v < min ? min : (v > max ? max : v);
	}

	/**
	 * 标准 inSampleSize：目标 2 倍余量，保证粗缩后仍够精确缩放。
	 * reqW/reqH <= 0 视为不缩放，返回 1。
	 */
	static int calcInSampleSize(int outW, int outH, int reqW, int reqH) {
		if (reqW <= 0 || reqH <= 0 || outW <= 0 || outH <= 0) {
			return 1;
		}
		int sample = 1;
		while (outW / (sample * 2) >= reqW && outH / (sample * 2) >= reqH) {
			sample *= 2;
		}
		return sample;
	}

	/** EXIF 角度转正：Matrix.postRotate；0 度直通返回原引用 */
	static Bitmap applyRotation(Bitmap src, int degrees) {
		if (degrees == 0 || src == null) {
			return src;
		}
		android.graphics.Matrix m = new android.graphics.Matrix();
		m.postRotate(degrees);
		Bitmap rotated = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), m, true);
		if (rotated != src) {
			src.recycle();
		}
		return rotated;
	}

	/** 等比缩进适配框（maxW/maxH <= 0 直通）；无需缩放返回原引用 */
	static Bitmap scaleToFit(Bitmap src, int maxW, int maxH) {
		if (src == null || maxW <= 0 || maxH <= 0) {
			return src;
		}
		int w = src.getWidth();
		int h = src.getHeight();
		float scale = Math.min(1.0f, Math.min((float) maxW / w, (float) maxH / h));
		if (scale >= 1.0f) {
			return src;
		}
		Bitmap scaled = Bitmap.createScaledBitmap(src, Math.round(w * scale), Math.round(h * scale), true);
		if (scaled != src) {
			src.recycle();
		}
		return scaled;
	}

	/**
	 * JPEG 压缩 + base64（NO_WRAP，铁律 1）。不 recycle 入参 bitmap，由调用方处理。
	 */
	static CompressResult compressToBase64(Bitmap bitmap, int quality) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.JPEG, quality, bos);
		// ⚠️ NO_WRAP 不能改默认：换行符会炸原生拼接的单行 JS 字符串，回调静默丢失
		String base64 = Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);
		return new CompressResult(base64, bos.size());
	}

	/** 剔除单引号（铁律 2）：null → "unknown" */
	static String sanitize(String s) {
		return s == null ? "unknown" : s.replace("'", "");
	}
}
