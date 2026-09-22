package com.lianghao.dots.jsbridge;

/**
 * 同步插件基类。自 ../news 移植。
 *
 * 线程约束：jsCallNative 运行在 JS 线程（非 UI），绝对禁止碰 UI/WebView，只做纯数据查询。
 * 返回值必须是 JSON 字符串（JS 侧 JSON.parse）。
 */
public abstract class BaseJSPluginSync extends BaseJSPlugin {
	@Override
	public void jsCallNative(String callbackId, String requestParams) {
		// ignore
	}

	/**
	 * 所有子类在此实现js同步调native业务逻辑
	 */
	public abstract String jsCallNative(String requestParams);
}
