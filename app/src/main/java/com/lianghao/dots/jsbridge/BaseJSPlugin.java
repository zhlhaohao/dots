package com.lianghao.dots.jsbridge;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.util.List;

/**
 * 所有jsPlugin应继承该类（异步插件基类）。自 ../news（com.tencent.tbs.jsbridge）移植，
 * WebView 类型由 X5（com.tencent.smtt.sdk.WebView）改为系统 android.webkit.WebView，
 * 宿主由 BaseWebViewActivity 改为 WebViewHost 接口。
 *
 * 线程约束：异步插件在 UI 线程执行，禁止耗时/IO 操作（需要时插件内部开线程，完成后回 UI 线程 report）。
 * 回调约束：每个请求恰好回调一次（四态选一），返回 JSON 字符串中禁止出现单引号。
 */
public abstract class BaseJSPlugin {
	private static final String TAG = BaseJSPlugin.class.getSimpleName();
	private WebViewHost host;
	private JSBridge mJSBridge;
	private String mCallbackId;
	private String mRequestParams;

	public BaseJSPlugin() {
	}

	public void bindHost(WebViewHost host) {
		this.host = host;
	}

	public Activity getActivity() {
		return this.host != null ? this.host.getHostActivity() : null;
	}

	public WebViewHost getHost() {
		return this.host;
	}

	public JSBridge getJSBridge() {
		return mJSBridge;
	}

	public void setJSBridge(JSBridge jsBridge) {
		this.mJSBridge = jsBridge;
	}

	public String getCallbackId() {
		return mCallbackId;
	}

	public void setCallbackId(String mCallbackId) {
		this.mCallbackId = mCallbackId;
	}

	public String getRequestParams() {
		return mRequestParams;
	}

	public void setRequestParams(String mRequestParams) {
		this.mRequestParams = mRequestParams;
	}

	/**
	 * 获取WebView
	 */
	public WebView getWebView() {
		if (mJSBridge != null) {
			return mJSBridge.getWebView();
		}
		return null;
	}

	/**
	 * 获取Context
	 */
	public Context getContext() {
		WebView webView = getWebView();
		if (webView != null) {
			return webView.getContext();
		}
		return null;
	}

	/**
	 * 报告JS成功
	 */
	public void reportSuccess(String callbackId) {
		reportSuccess(callbackId, null);
	}

	/**
	 * 报告JS成功
	 */
	public void reportSuccess(String callbackId, String params) {
		mJSBridge.callbackJS(callbackId, JSCallbackType.SUCCESS, params);
	}

	/**
	 * 报告JS错误
	 */
	public void reportFail(String callbackId) {
		reportFail(callbackId, null);
	}

	/**
	 * 报告JS错误
	 */
	public void reportFail(String callbackId, String params) {
		mJSBridge.callbackJS(callbackId, JSCallbackType.FAIL, params);
	}

	/**
	 * 报告JS取消
	 */
	public void reportCancel(String callbackId) {
		reportCancel(callbackId, null);
	}

	/**
	 * 报告JS取消
	 */
	public void reportCancel(String callbackId, String params) {
		mJSBridge.callbackJS(callbackId, JSCallbackType.CANCEL, params);
	}

	/**
	 * 报告JS完成
	 */
	public void reportCompletion(String callbackId) {
		reportCompletion(callbackId, null);
	}

	/**
	 * 报告JS完成
	 */
	public void reportCompletion(String callbackId, String params) {
		mJSBridge.callbackJS(callbackId, JSCallbackType.COMPLETION, params);
	}

	/**
	 * 所有子类在此实现js调native业务逻辑，异步
	 */
	public abstract void jsCallNative(String callbackId, String requestParams);

	/**
	 * 通知web页面返回上一个页面
	 */
	public void pageGoBack() {
	}

	public void resultCallback(int requestCode, int resultCode, @Nullable Intent data) {

	}

	public void permissionCallback(int requestCode, @NonNull List<String> perms, boolean isGranted) {

	}

	protected String ErrorJson(String errorMessage) {
		try {
			JSONObject errorResult = new JSONObject();
			errorResult.put("error", errorMessage);
			errorResult.put("code", -1); // 假设-1表示错误
			return errorResult.toString();
		} catch (org.json.JSONException je) {
			Log.i(TAG, 189 + "-Failed to create error JSON");
			return "";
		}
	}
}
