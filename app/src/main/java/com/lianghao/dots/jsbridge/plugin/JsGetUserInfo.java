package com.lianghao.myapp.jsbridge.plugin;

import android.util.Log;

import com.lianghao.myapp.jsbridge.BaseJSPlugin;

import org.json.JSONObject;

/**
 * 获取用户信息（异步通道纯数据示范）。自 ../news 移植。
 * 注意：返回的是写死的测试账号，仅用于演示回调结构，勿用于生产。
 */
public class JsGetUserInfo extends BaseJSPlugin {
	private static final String TAG = JsGetUserInfo.class.getSimpleName();

	@Override
	public void jsCallNative(String callbackId, String requestParams) {
		Log.i(TAG, String.format("26- %s", "JsGetUserInfo"));
		JSONObject rspJson = new JSONObject();
		try {
			rspJson.put("username", "lianghao1");
			rspJson.put("password", "happy99...");
			rspJson.put("code", 0);
		} catch (Exception e) {
			reportFail(callbackId);
			return;
		}
		reportSuccess(callbackId, rspJson.toString());
	}
}
