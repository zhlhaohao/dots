package com.lianghao.dots.jsbridge;

import android.os.Build;
import android.text.TextUtils;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JSBridge 分发器。自 ../news（com.tencent.tbs.jsbridge.JSBridge）移植：
 * - WebView 类型由 X5 改为系统 android.webkit.WebView；
 * - 移除 X5 专属的 openDebugX5/openWebkit/zxing 扫码直暴露接口（宿主无此设施）；
 * - bindActivity(BaseWebViewActivity) 改为 bindHost(WebViewHost) 接口。
 *
 * JS 侧入口（注入对象名见 HybridConstant.HYBRID_BRIDGE_NAME，当前 "Android"）：
 * - 异步：Android.messageSend(funcName, callbackId, paramsJsonStr) → UI 线程执行插件 →
 *         原生回调 jsbridge.callBackFromNative(callbackId, type, params)
 * - 同步：Android.syndMessageSend(funcName, paramsJsonStr) → JS 线程执行（仅 BaseJSPluginSync 子类）→ return JSON 字符串
 * - 兜底：Android.hybrid(callbackId) —— JS 侧回调字典无该态时通知原生（勿手动调）
 */
public class JSBridge {
	/**
	 * jsPlugin容器，key：js调用的方法名 value：相应的jsPlugin
	 */
	private Map<String, BaseJSPlugin> jsPluginMap;
	private WebView mWebView;
	private WebViewHost host;

	public JSBridge(WebView mWebView) {
		this.mWebView = mWebView;

		// 给webview增加本类。作为javascript接口, HybridConstant.HYBRID_BRIDGE_NAME 是js的接口对象名
		mWebView.addJavascriptInterface(this, HybridConstant.HYBRID_BRIDGE_NAME);
	}

	public void bindHost(WebViewHost host) {
		this.host = host;
	}

	/**
	 * 注册jsPlugin，所有hybrid交互必须先注册jsPlugin
	 */
	public void registerJSPlugin(String jsFunction, BaseJSPlugin jsPlugin) {
		if (jsPluginMap == null) {
			jsPluginMap = new LinkedHashMap<>();
		}
		if (!TextUtils.isEmpty(jsFunction) && jsPlugin != null) {
			jsPlugin.bindHost(this.host);
			jsPluginMap.put(jsFunction, jsPlugin);
		} else {
			throw new UnsupportedOperationException("jsFunction or jsPlugin is not allowed to be empty.");
		}
	}

	/**
	 * 获取jsPlugin
	 */
	public BaseJSPlugin getJSPlugin(String jsFunction) {
		return jsPluginMap == null ? null : jsPluginMap.get(jsFunction);
	}

	/**
	 * 分发js请求，异步
	 */
	private void dispatchJSRequest(final String functionName, final String callbackId, final String params) {
		//run in ui-thread
		mWebView.post(new Runnable() {
			@Override
			public void run() {
				BaseJSPlugin jsPlugin = jsPluginMap.get(functionName);
				try {
					if (jsPlugin != null) {
						jsPlugin.setCallbackId(callbackId);
						jsPlugin.setRequestParams(params);
						jsPlugin.setJSBridge(JSBridge.this);
						jsPlugin.jsCallNative(callbackId, params);
					} else {
						callbackJS(callbackId, JSCallbackType.FAIL, null);
					}
				} catch (Exception e) {
					callbackJS(callbackId, JSCallbackType.FAIL, null);
				}
			}
		});
	}

	/**
	 * android call js ：android调用js统一调用此方法
	 *
	 * 注意：params 是字符串拼接进 JS 代码的，禁止含单引号（会破坏 JS 语法导致回调静默丢失）。
	 */
	public void callbackJS(final String callbackId, final JSCallbackType callbackFunctionName, final String params) {
		if (null == mWebView) {
			return;
		}

		final StringBuilder jsSB = new StringBuilder();
		jsSB.append("jsbridge.callBackFromNative('");
		jsSB.append(callbackId);
		jsSB.append("','");
		jsSB.append(callbackFunctionName.getValue());
		jsSB.append(TextUtils.isEmpty(params) ? "')" : ("','" + params + "')"));

		//run in ui-thread
		mWebView.post(new Runnable() {
			@Override
			public void run() {
				if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
					mWebView.evaluateJavascript(jsSB.toString(), null);
				} else {
					mWebView.loadUrl("javascript:" + jsSB.toString());
				}
			}
		});
	}

	/**
	 * js call android接口，异步：各业务统一从该入口分发
	 */
	@JavascriptInterface
	public void messageSend(String functionName, final String callbackId, String params) {
		//分发js请求
		dispatchJSRequest(functionName, callbackId, params);
	}

	/**
	 * js call android接口，同步
	 */
	@JavascriptInterface
	public String syndMessageSend(String functionName, String params) {
		//分发js请求
		return dispatchJSRequest(functionName, params);
	}

	/**
	 * 分发同步请求
	 */
	private String dispatchJSRequest(String functionName, String params) {
		BaseJSPlugin jsPlugin = jsPluginMap.get(functionName);
		if (jsPlugin != null && (jsPlugin instanceof BaseJSPluginSync)) {
			BaseJSPluginSync jsPluginSync = (BaseJSPluginSync) jsPlugin;
			jsPluginSync.setRequestParams(params);
			jsPluginSync.setJSBridge(this);
			return jsPluginSync.jsCallNative(params);
		}
		return null;
	}

	/**
	 * js call android接口：js报错统一从该入口分发
	 */
	@JavascriptInterface
	public void hybridJSError(String callbackId) {

	}

	/**
	 * js侧回调字典无对应回调态时的兜底通知（jsbridge.js 内部调用，勿手动调）
	 */
	@JavascriptInterface
	public void hybrid(String callbackId) {
		// ignore：事件丢弃即可，与 news 侧行为一致
	}

	/**
	 * 获取WebView
	 */
	public WebView getWebView() {
		return mWebView;
	}
}
