package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import android.webkit.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

// JavaScript接口类，用于处理WebView中的事件
class WebAppInterface(
    private val context: Context,
    private val onEvent: (String, String) -> Unit  // 事件回调
) {
    @android.webkit.JavascriptInterface
    fun onCustomEvent(eventType: String, eventData: String) {
        onEvent(eventType, eventData)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun H5PageWebView(
    url: String? = null,
    localAssetFileName: String? = null,
    modifier: Modifier = Modifier,
    onWebViewEvent: (String, String) -> Unit = { _, _ -> },  // 新增事件回调参数
    // 添加一个回调函数，允许外部调用JavaScript
    onWebViewCreated: (android.webkit.WebView) -> Unit = {}
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                
                webViewClient = object : WebViewClient() {
                    // 允许加载本地资源
                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        return false
                    }
                    
                    // 处理错误情况
                    override fun onReceivedError(
                        view: WebView?, 
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        super.onReceivedError(view, request, error)
                        // 可以在这里处理错误
                        println("WebView Error: ${error?.description}")
                    }
                }
                
                webChromeClient = object : WebChromeClient() {
                    // 可以处理更多WebView事件，如进度变化
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        // 这里可以监听页面加载进度
                        onWebViewEvent("progress", newProgress.toString())
                    }
                }
                
                // 启用JavaScript
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                settings.javaScriptCanOpenWindowsAutomatically = true
                
                // 启用跨域请求
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                
                // 其他设置
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.setSupportZoom(true)
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                
                // 允许访问文件
                settings.allowFileAccess = true
                settings.allowContentAccess = true
                
                // 启用缓存
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                
                // 添加JavaScript接口
                addJavascriptInterface(
                    WebAppInterface(context) { eventType, eventData ->
                        onWebViewEvent(eventType, eventData)
                    }, 
                    "AndroidInterface"
                )
                
                // 调用回调函数，传递WebView实例
                onWebViewCreated(this)
                
                // 根据参数决定加载远程URL还是本地资源
                when {
                    !localAssetFileName.isNullOrEmpty() -> {
                        loadUrl("file:///android_asset/$localAssetFileName")
                    }
                    !url.isNullOrEmpty() -> {
                        loadUrl(url)
                    }
                    else -> {
                        // 默认加载示例页面
                        loadUrl("file:///android_asset/local_page.html")
                    }
                }
            }
        },
        update = { webView ->
            when {
                !localAssetFileName.isNullOrEmpty() -> {
                    webView.loadUrl("file:///android_asset/$localAssetFileName")
                }
                !url.isNullOrEmpty() -> {
                    webView.loadUrl(url)
                }
            }
        },
        modifier = modifier.fillMaxSize()
    )
}