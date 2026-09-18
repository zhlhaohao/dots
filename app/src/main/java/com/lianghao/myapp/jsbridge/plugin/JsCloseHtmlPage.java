package com.lianghao.myapp.jsbridge.plugin;

import android.app.Activity;
import android.util.Log;

import com.lianghao.myapp.jsbridge.BaseJSPluginSync;

import org.json.JSONObject;

/**
 * 关闭当前 WebView 页面（同步通道 + UI 操作示范）。自 ../news 移植。
 *
 * 注意：news 侧原本就是同步插件里调 finish()（JS 线程碰 UI 的反例），此处保持原语义，
 * finish() 本身线程安全可通过 Activity.runOnUiThread 兜底，行为与 news 一致。
 */
public class JsCloseHtmlPage extends BaseJSPluginSync {
	private static final String TAG = JsCloseHtmlPage.class.getSimpleName();

	@Override
	public String jsCallNative(String data) {
		try {
			Log.i(TAG, String.format("20- %s", "JsCloseHtmlPage"));
			Activity activity = getActivity();
			if (activity != null) {
				activity.runOnUiThread(activity::finish);
			}
			JSONObject result = new JSONObject();
			result.put("code", 0);
			return result.toString();
		} catch (Exception e) {
			Log.e(TAG, "An error occurred", e);
			return ErrorJson(e.getMessage());
		}
	}
}
