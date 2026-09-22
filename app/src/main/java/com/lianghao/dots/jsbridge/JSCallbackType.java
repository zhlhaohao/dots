package com.lianghao.dots.jsbridge;

/**
 * 异步通道四态回调。自 ../news 移植。
 */
public enum JSCallbackType {
	SUCCESS("success"), FAIL("fail"), CANCEL("cancel"), COMPLETION("completion");

	private String value;

	JSCallbackType(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}
}
