package com.lianghao.myapp

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
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

        webView.webViewClient = WebViewClient()
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
