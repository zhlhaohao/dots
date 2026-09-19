package com.lianghao.myapp

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.net.toUri
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.lianghao.myapp.jsbridge.BaseJSPlugin
import com.lianghao.myapp.jsbridge.HybridConstant
import com.lianghao.myapp.jsbridge.JSBridge
import com.lianghao.myapp.jsbridge.WebViewHost
import com.lianghao.myapp.jsbridge.plugin.JsCanGoBack
import com.lianghao.myapp.jsbridge.plugin.JsCloseHtmlPage
import com.lianghao.myapp.jsbridge.plugin.JsGetScreenInfo
import com.lianghao.myapp.jsbridge.plugin.JsGetUserInfo
import com.lianghao.myapp.jsbridge.plugin.JsTakePhoto
import com.lianghao.myapp.jsbridge.plugin.JsPickPhotos
import com.lianghao.myapp.jsbridge.plugin.JsPickAndUploadFiles

/**
 * JSBridge 宿主页面：承载系统 WebView，注册 JS 插件并加载 URL。
 *
 * 生命周期约定（保证零初始化时序，移植自 news 的 BaseWebViewActivity）：
 *   initWebViewClient() → registerJSApi() → loadUrl()
 * 插件注入先于页面任何 JS 执行，H5 引入 jsbridge.js 后随处可调，无需 ready 握手。
 *
 * 用法：WebViewActivity.start(context, url)；url 缺省加载内置演示页。
 */
class WebViewActivity : AppCompatActivity(), WebViewHost {

    companion object {
        const val EXTRA_URL = "extra_url"
        const val DEMO_PAGE_URL = "file:///android_asset/webpage/jsbridge_demo.html"

        /** WebView 可自行加载的网页协议；其余 scheme 视为外部 App 协议 */
        private val WEB_SCHEMES = setOf("http", "https", "file", "about", "blob", "data")

        /** 内网私有网段（RFC1918）与 loopback：证书校验放行范围 */
        private val PRIVATE_HOSTS = listOf<(String) -> Boolean>(
            { it == "localhost" || it == "127.0.0.1" || it == "::1" },
            { Regex("^10\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$").matches(it) },
            { Regex("^192\\.168\\.(\\d{1,3})\\.(\\d{1,3})$").matches(it) },
            { Regex("^172\\.(1[6-9]|2\\d|3[01])\\.(\\d{1,3})\\.(\\d{1,3})$").matches(it) }
        )

        fun start(context: android.content.Context, url: String? = null) {
            val intent = Intent(context, WebViewActivity::class.java)
            intent.putExtra(EXTRA_URL, url ?: DEMO_PAGE_URL)
            context.startActivity(intent)
        }
    }

    private lateinit var webView: WebView
    private lateinit var jsBridge: JSBridge

    // Activity 结果 / 权限回调注册表：插件把自己挂进来，宿主转发结果（拉起扫码/拍照等场景预留）
    private val resultCallbacks = HashMap<Int, BaseJSPlugin>()
    private val permissionCallbacks = HashMap<Int, BaseJSPlugin>()
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>

    override fun getHostActivity(): Activity = this

    override fun registerResultCallback(requestCode: Int, jsPlugin: BaseJSPlugin) {
        resultCallbacks[requestCode] = jsPlugin
    }

    override fun registerPermissionCallback(requestCode: Int, jsPlugin: BaseJSPlugin) {
        permissionCallbacks[requestCode] = jsPlugin
    }

    override fun startPluginActivityForResult(intent: Intent, requestCode: Int) {
        activityResultLauncher.launch(intent)
    }

    override fun requestRuntimePermissions(requestCode: Int, rationale: String, vararg permissions: String) {
        permissionLauncher.launch(arrayOf(*permissions))
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)
        title = getString(R.string.webview_title)

        webView = findViewById(R.id.webView)

        activityResultLauncher = registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            // 单插件场景：取首个注册者转发（news 侧按 requestCode 精确匹配，本宿主预留简化实现）
            resultCallbacks.values.firstOrNull()?.resultCallback(-1, result.resultCode, result.data)
        }
        permissionLauncher = registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
        ) { grants: Map<String, Boolean> ->
            val plugin = permissionCallbacks.values.firstOrNull()
            val granted = grants.values.all { it }
            plugin?.permissionCallback(-1, grants.keys.toList(), granted)
        }

        initWebViewClient()
        registerJSApi()
        webView.loadUrl(intent.getStringExtra(EXTRA_URL) ?: DEMO_PAGE_URL)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebViewClient() {
        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        // 追加容器标识，H5 通过 UA 里的 NexBox/x.y 感知运行环境（与 news 侧约定一致）
        settings.userAgentString = settings.userAgentString + HybridConstant.HYBRID_UA_FLAG

        webView.webViewClient = object : WebViewClient() {
            /**
             * 拦截非网页协议导航（如 m 站跳 App 的私有 scheme snssdk143://）。
             * 放行 http/https/file/about/blob/data，其余尝试唤起外部 App，
             * 唤不起（无 App 处理）则忽略本次导航，页面留在原地继续渲染。
             * 不拦截会直接报 ERR_UNKNOWN_URL_SCHEME 并用错误页覆盖已加载内容。
             */
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val url = request.url
                val scheme = url.scheme?.lowercase() ?: return false
                if (scheme in WEB_SCHEMES) return false // 网页协议：交 WebView 正常加载
                return try {
                    startActivity(Intent(Intent.ACTION_VIEW, url)) // 私有 scheme：尝试唤起对应 App
                    true
                } catch (e: android.content.ActivityNotFoundException) {
                    true // 无 App 可处理：吞掉导航，保留当前页面
                }
            }
            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler,
                error: SslError?
            ) {
                // 内网私有部署站点（如 10.x 网段 gui 服务）常用过期/自签证书，
                // 系统默认 cancel 直接 ERR_CERT_AUTHORITY_INVALID。
                // 策略：私有网段地址放行，其余仍拒绝。
                val host = view?.url?.toUri()?.host ?: ""
                if (PRIVATE_HOSTS.any { it(host) }) {
                    handler.proceed()
                } else {
                    handler.cancel()
                }
            }
        }
        webView.webChromeClient = WebChromeClient()
    }

    private fun registerJSApi() {
        jsBridge = JSBridge(webView)
        jsBridge.bindHost(this)

        // 示范插件：同步通道 / 异步通道 / UI 操作（详见 app/src/main/assets/webpage/jsbridge_demo.html）
        jsBridge.registerJSPlugin("getScreenInfo", JsGetScreenInfo())
        jsBridge.registerJSPlugin("getUserInfo", JsGetUserInfo())
        jsBridge.registerJSPlugin("closeHtmlPage", JsCloseHtmlPage())
        jsBridge.registerJSPlugin("canGoBack", JsCanGoBack())
        jsBridge.registerJSPlugin("takePhoto", JsTakePhoto())
        jsBridge.registerJSPlugin("pickPhotos", JsPickPhotos())
        jsBridge.registerJSPlugin("pickAndUploadFiles", JsPickAndUploadFiles())
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
            return
        }
        // WebView 无历史：JS 未禁止关闭（默认允许）则正常关页；禁止则转发给页面处理
        val canGoBackPlugin = jsBridge.getJSPlugin("canGoBack") as? JsCanGoBack
        if (canGoBackPlugin?.isAllowClose() == false) {
            canGoBackPlugin.pageGoBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
