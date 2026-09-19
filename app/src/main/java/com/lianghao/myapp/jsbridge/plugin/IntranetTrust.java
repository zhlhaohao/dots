package com.lianghao.myapp.jsbridge.plugin;

import android.util.Log;

import java.net.InetAddress;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/** 内网(RFC1918/loopback)自签名证书信任器 —— 与 WebView onReceivedSslError 的放行策略对齐(见 docs/adr/0004)。 */
public final class IntranetTrust {
	private static final String TAG = "IntranetTrust";

	private IntranetTrust() {}

	/** 目标 host 是否属于"可信内网"范围(RFC1918 + loopback)。host 非法返回 false。 */
	public static boolean isPrivate(String host) {
		try {
			if (host == null || host.isEmpty()) return false;
			if ("localhost".equalsIgnoreCase(host)) return true;
			InetAddress a = InetAddress.getByName(host);
			return a.isLoopbackAddress() || a.isSiteLocalAddress();
		} catch (Exception e) {
			return false;
		}
	}

	/** 返回信任一切的 X509TrustManager(仅限内网地址使用)。 */
	public static X509TrustManager trustAllManager() {
		return new X509TrustManager() {
			@Override public void checkClientTrusted(X509Certificate[] chain, String authType) {}
			@Override public void checkServerTrusted(X509Certificate[] chain, String authType) {}
			@Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
		};
	}

	/** 构造信任一切的 SSLSocketFactory。 */
	public static SSLSocketFactory trustAllFactory(X509TrustManager tm) {
		try {
			SSLContext ctx = SSLContext.getInstance("TLS");
			ctx.init(null, new TrustManager[]{tm}, null);
			return ctx.getSocketFactory();
		} catch (Exception e) {
			Log.e(TAG, "init ssl failed", e);
			return null;
		}
	}
}
