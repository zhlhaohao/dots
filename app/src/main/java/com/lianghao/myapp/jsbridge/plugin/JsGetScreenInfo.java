package com.lianghao.myapp.jsbridge.plugin;

import android.graphics.Point;
import android.util.Log;
import android.view.Display;

import com.lianghao.myapp.jsbridge.BaseJSPluginSync;

import org.json.JSONObject;

/**
 * 获取屏幕信息（同步通道示范）。自 ../news 移植。
 */
public class JsGetScreenInfo extends BaseJSPluginSync {
	private static final String TAG = JsGetScreenInfo.class.getSimpleName();

	@Override
	public String jsCallNative(String data) {
		try {
			Display display = getActivity().getWindowManager().getDefaultDisplay();
			Point size = new Point();
			display.getSize(size);
			int width = size.x;
			int height = size.y;

			JSONObject result = new JSONObject();
			result.put("width", width);
			result.put("height", height);
			result.put("code", 0);
			Log.i(TAG, String.format("32- width:%d height:%d", width, height));
			return result.toString();
		} catch (Exception e) {
			Log.e(TAG, "An error occurred", e);
			return ErrorJson(e.getMessage());
		}
	}
}
