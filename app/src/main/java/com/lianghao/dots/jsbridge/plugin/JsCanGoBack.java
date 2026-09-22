package com.lianghao.dots.jsbridge.plugin;

import android.util.Log;

import com.lianghao.dots.jsbridge.BaseJSPlugin;

import org.json.JSONObject;

/**
 * 控制宿主是否允许被返回键关闭。自 ../news 移植（原依赖宿主 public 字段，本版状态内聚在插件）。
 *
 * JS 调用 canGoBack({value:"true"/"false"})：
 * - "true"（默认）：WebView 无历史时返回键直接关闭宿主页
 * - "false"：返回键不关页，转而回调该次调用注册的 success（{"result":"true"}），由页面自行处理回退
 */
public class JsCanGoBack extends BaseJSPlugin {
	private static final String TAG = JsCanGoBack.class.getSimpleName();
	private String callbackId = "";
	private boolean allowClose = true; // 默认允许关闭，与 news 宿主字段初值一致

	@Override
	public void jsCallNative(String callbackId, String data) {
		this.callbackId = callbackId;
		try {
			JSONObject param = new JSONObject(data);
			String value = param.getString("value");
			this.allowClose = value.equals("true");
			Log.i(TAG, "allowClose=" + allowClose);
		} catch (Exception e) {
			reportFail(callbackId);
		}
	}

	/**
	 * 宿主查询：JS 是否允许返回键关闭页面
	 */
	public boolean isAllowClose() {
		return allowClose;
	}

	/**
	 * 宿主在返回键被拦截时调用，用于通知页面自行处理回退
	 */
	@Override
	public void pageGoBack() {
		if (this.callbackId.equals("")) {
			return;
		}
		JSONObject rspJson = new JSONObject();
		try {
			rspJson.put("result", "true");
		} catch (Exception e) {
			reportFail(this.callbackId);
			return;
		}
		// 返回结果给html js
		reportSuccess(this.callbackId, rspJson.toString());
	}
}
