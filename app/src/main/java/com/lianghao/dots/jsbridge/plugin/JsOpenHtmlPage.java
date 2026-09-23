package com.lianghao.dots.jsbridge.plugin;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.lianghao.dots.WebViewActivity;
import com.lianghao.dots.jsbridge.BaseJSPluginSync;

import org.json.JSONObject;

/**
 * 在当前 WebView 之上打开新一层 WebViewActivity 加载指定 URL（同步通道 + 启动新页示范）。
 * 自 ../news 的 JsOpenHtmlPage 移植（news 打开的是 PureX5WebViewActivity，此处改为 dots 的 WebViewActivity）。
 *
 * JS 调用契约：
 *   jsbridge.openHtmlPage({url: "https://example.com"}) → 原生启动新一层 WebViewActivity，
 *   返回同步 JSON 字符串 {code:"success"}（与 news 一致：字符串而非数字）。
 *
 * 与 shouldOverrideUrlLoading 的关系：
 *   - 主框架 http/https 自然导航：由 WebViewClient 自动开新层（无需调本插件）。
 *   - 由 JS 主动触发（任意时机）：用本插件，避免必须借助 <a href> 才能开新层。
 */
public class JsOpenHtmlPage extends BaseJSPluginSync {
	private static final String TAG = JsOpenHtmlPage.class.getSimpleName();

	@Override
	public String jsCallNative(String data) {
		try {
			JSONObject param = new JSONObject(data);
			String url = param.getString("url");
			Log.i(TAG, "open url: " + url);

			Activity activity = getActivity();
			if (activity != null) {
				// 与 DashboardActivity 单击格子走同一入口，统一用 EXTRA_URL 常量
				Intent intent = new Intent(activity, WebViewActivity.class);
				intent.putExtra(WebViewActivity.EXTRA_URL, url);
				activity.startActivity(intent);
			}

			JSONObject result = new JSONObject();
			result.put("code", "success");
			return result.toString();
		} catch (Exception e) {
			Log.e(TAG, "An error occurred", e);
			return ErrorJson(e.getMessage());
		}
	}
}