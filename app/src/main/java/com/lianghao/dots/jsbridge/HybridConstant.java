package com.lianghao.myapp.jsbridge;

/**
 * JSBridge 框架常量。自 ../news（com.tencent.tbs.jsbridge）移植。
 */
public class HybridConstant {

	/**
	 * 注入给JS调用的对象名称
	 */
	public static final String HYBRID_BRIDGE_NAME = "Android";

	/**
	 * 追加到 WebView UserAgent 的容器标识，H5 通过 navigator.userAgent 匹配 NexBox/x.y 感知运行环境与 App 版本。
	 * 有意沿用 news 侧同名标识，便于同一套 H5 页面在两个 App 中无差别接入（见 docs/adr/0001）。
	 */
	public static final String HYBRID_UA_FLAG = " NexBox/1.0";
}
