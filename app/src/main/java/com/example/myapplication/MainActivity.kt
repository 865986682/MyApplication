package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay

/**
 * 主Activity类，负责应用的主要UI展示和功能协调
 * 包含WebView显示H5页面、NFC功能、相机功能、通知功能等
 */
class MainActivity : ComponentActivity() {
    private lateinit var nfcManager: NFCManager
    private lateinit var cameraManager: CameraManager
    private lateinit var qrCodeScannerManager: QRCodeScannerManager
    private var nfcStatusCallback: ((String) -> Unit)? = null
    private var webViewRef: android.webkit.WebView? = null
    
    /**
     * Activity创建时的初始化方法
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcManager = NFCManager(this)
        cameraManager = CameraManager(this)
        qrCodeScannerManager = QRCodeScannerManager(this)
        
        // 创建通知渠道
        NotificationHelper.createNotificationChannel(this)
        
        enableEdgeToEdge()
        setContent {
            var showDialog by remember { mutableStateOf(false) }
            var dialogTitle by remember { mutableStateOf("") }
            var dialogMessage by remember { mutableStateOf("") }
            var bannerVisible by remember { mutableStateOf(false) }
            var bannerText by remember { mutableStateOf("") }
            var nfcStatus by remember { mutableStateOf("点击按钮开始NFC扫描") }
            
            // 设置NFC状态回调
            DisposableEffect(Unit) {
                nfcStatusCallback = { newStatus ->
                    runOnUiThread {
                        nfcStatus = newStatus
                    }
                }
                onDispose {
                    nfcStatusCallback = null
                }
            }
            
            MyApplicationTheme {
                // 弹窗组件
                if (showDialog) {
                    AlertDialog(
                        onDismissRequest = {
                            showDialog = false
                        },
                        title = { Text(text = dialogTitle) },
                        text = { Text(text = dialogMessage) },
                        confirmButton = {
                            TextButton(onClick = { showDialog = false }) {
                                Text("确定")
                            }
                        }
                    )
                }
                
                Box(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // WebView组件 - 显示H5页面
                            H5PageWebView(
                                localAssetFileName = "local_page.html", // 加载本地页面
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                onWebViewEvent = { eventType, eventData ->
                                    // 处理WebView中的事件
                                    when (eventType) {
                                        "progress" -> {
                                            Log.d("WebView", "页面加载进度: $eventData%")
                                        }
                                        "custom" -> {
                                            Log.d("WebView", "自定义事件: $eventData")
                                            // 解析来自H5的事件数据
                                            try {
                                                val dataMap = parseEventData(eventData)
                                                
                                                when (dataMap["action"]) {
                                                    "show_dialog" -> {
                                                        // 显示对话框
                                                        dialogTitle = dataMap["title"] ?: "提示"
                                                        dialogMessage = dataMap["message"] ?: dataMap["data"] ?: "收到消息"
                                                        showDialog = true
                                                    }
                                                    "show_notification" -> {
                                                        // 检查并请求通知权限，然后显示Heads-up通知
                                                        if (NotificationHelper.hasNotificationPermission(this@MainActivity)) {
                                                            // 如果已有权限，直接显示Heads-up通知
                                                            val title = dataMap["title"] ?: "通知"
                                                            val content = dataMap["message"] ?: dataMap["data"] ?: "您有一条新消息"
                                                            
                                                            val intent = packageManager.getLaunchIntentForPackage(packageName)
                                                            NotificationHelper.showHeadsUpNotification(this@MainActivity, title, content, intent)
                                                        } else {
                                                            // 如果没有权限，请求权限
                                                            NotificationHelper.requestNotificationPermission(this@MainActivity)
                                                        }
                                                    }
                                                    "show_banner" -> {
                                                        // 在应用顶部显示横幅通知
                                                        bannerText = dataMap["message"] ?: dataMap["data"] ?: "您有一条新消息"
                                                        bannerVisible = true
                                                    }
                                                    "trigger_nfc_scan" -> {
                                                        // 处理来自H5页面的NFC扫描请求
                                                        if (nfcManager.isNFCAvailable()) {
                                                            nfcStatus = "H5页面请求：请将手机靠近NFC标签 (5秒后自动停止)"
                                                            nfcManager.enableNFCForegroundDispatchWithTimeout(5000)
                                                        } else {
                                                            nfcStatus = "设备不支持NFC或NFC未启用"
                                                        }
                                                    }
                                                    "trigger_camera" -> {
                                                        // 处理来自H5页面的摄像头请求
                                                        cameraManager.captureImage()
                                                    }
                                                    "scan_qr_code" -> {
                                                        // 处理来自H5页面的二维码扫描请求
                                                        qrCodeScannerManager.startQRCodeScan()
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                Log.e("WebView", "解析事件数据失败: ${e.message}")
                                            }
                                        }
                                        else -> {
                                            Log.d("WebView", "未知事件类型: $eventType, 数据: $eventData")
                                        }
                                    }
                                },
                                onWebViewCreated = { webView ->
                                    webViewRef = webView
                                }
                            )
                        }
                    }
                    
                    // 顶部横幅通知
                    AnimatedVisibility(
                        visible = bannerVisible,
                        enter = fadeIn(animationSpec = tween(durationMillis = 300)) +
                                androidx.compose.animation.slideInVertically(initialOffsetY = { -it }),
                        exit = fadeOut(animationSpec = tween(durationMillis = 300)) +
                               androidx.compose.animation.slideOutVertically(targetOffsetY = { -it }),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 56.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            color = Color(0xFF323232),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = bannerText,
                                    color = Color.White,
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.weight(1f)
                                )
                                
                                IconButton(
                                    onClick = { bannerVisible = false },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = androidx.compose.material.icons.Icons.Default.Close,
                                        contentDescription = "关闭",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                    
                    // 使用协程自动隐藏横幅
                    LaunchedEffect(bannerVisible) {
                        if (bannerVisible) {
                            delay(3000) // 3秒后自动隐藏
                            bannerVisible = false
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Activity恢复时调用，启用NFC前台调度
     */
    override fun onResume() {
        super.onResume()
        // 使用带超时的NFC前台调度
//        nfcManager.enableNFCForegroundDispatchWithTimeout(5000)
    }
    
    /**
     * Activity暂停时调用，禁用NFC前台调度
     */
    override fun onPause() {
        super.onPause()
        nfcManager.disableNFCForegroundDispatch()
    }
    
    /**
     * 处理NFC新意图，接收NFC标签数据
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        nfcManager.handleNfcIntent(intent) { nfcData ->
            // 通过回调更新NFC状态
            nfcStatusCallback?.invoke("读取到NFC数据: $nfcData")
            
            // 可以在这里显示通知或执行其他操作
            if (nfcData.isNotEmpty()) {
                NotificationHelper.showHeadsUpNotification(
                    this@MainActivity,
                    "NFC数据读取成功",
                    "读取到数据: ${nfcData.take(30)}${if (nfcData.length > 30) "..." else ""}"
                )
            }
        }
    }
    
    /**
     * 处理权限请求结果
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            NotificationHelper.NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    // 权限被授予，可以发送通知
                    Log.d("Notification", "通知权限已授予")
                } else {
                    // 权限被拒绝
                    Log.d("Notification", "通知权限被拒绝")
                }
            }
            CameraManager.CAMERA_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    // 权限被授予，可以使用相机
                    Log.d("Camera", "相机权限已授予")
                    cameraManager.captureImage()
                } else {
                    // 权限被拒绝
                    Log.d("Camera", "相机权限被拒绝")

                }
            }
        }
    }
    
    /**
     * 处理相机拍照结果，将拍摄的图片传递给H5页面
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == CameraManager.CAMERA_CAPTURE_REQUEST_CODE) {
            val imageData = cameraManager.handleCameraResult(requestCode, resultCode, data)
            if (imageData != null) {
                // 将图片数据传递给WebView
                runOnUiThread {
                    // 发送通知
                    NotificationHelper.showHeadsUpNotification(
                        this@MainActivity,
                        "拍照成功",
                        "照片已拍摄，数据已传递给H5页面"
                    )
                    
                    // 使用延迟机制确保WebView引用已设置
                    Handler(mainLooper).postDelayed({
                        webViewRef?.let { webView ->
                            // 确保页面已加载后再执行JavaScript
                            webView.post {
                                // 将图片数据作为Base64字符串传递给H5页面的函数
                                val jsCode = "javascript:receiveCapturedImage('$imageData')"
                                webView.loadUrl(jsCode)
                            }
                        } ?: run {
                            // 如果webViewRef仍然为null，记录错误
                            Log.e("MainActivity", "WebView reference is still null after delay")
                        }
                    }, 1000) // 延迟1秒执行，确保页面完全加载
                }
            }
        }
        
        // 处理二维码扫描结果
        val qrCodeResult = qrCodeScannerManager.handleResult(requestCode, resultCode, data)
        if (qrCodeResult != null) {
            runOnUiThread {
                // 使用延迟机制确保WebView引用已设置
                Handler(mainLooper).postDelayed({
                    webViewRef?.let { webView ->
                        // 确保页面已加载后再执行JavaScript
                        webView.post {
                            // 将二维码扫描结果传递给H5页面的函数
                            val escapedResult = qrCodeResult.replace("'", "\\'")
                            val jsCode = "javascript:receiveQRCodeResult('$escapedResult')"
                            webView.loadUrl(jsCode)
                        }
                    } ?: run {
                        // 如果webViewRef仍然为null，记录错误
                        Log.e("MainActivity", "WebView reference is still null after delay when handling QR code")
                    }
                }, 1000) // 延迟1秒执行，确保页面完全加载
            }
        }
    }
    
    /**
     * 解析H5传来的事件数据
     */
    private fun parseEventData(data: String): Map<String, String> {
        // 简单解析键值对格式的数据
        return if (data.startsWith("{")) {
            // 如果是JSON格式
            try {
                // 简单的解析逻辑，实际应用中可以用JSON库
                val map = mutableMapOf<String, String>()
                val cleanedData = data.replace("{", "").replace("}", "")
                    .replace("\"", "").replace("\\", "")
                for (pair in cleanedData.split(",")) {
                    val parts = pair.split(":")
                    if (parts.size == 2) {
                        map[parts[0].trim()] = parts[1].trim()
                    }
                }
                map
            } catch (e: Exception) {
                mutableMapOf("data" to data)
            }
        } else {
            // 如果是简单键值对格式，如 "key1:value1,key2:value2"
            val map = mutableMapOf<String, String>()
            for (pair in data.split(",")) {
                val parts = pair.split(":")
                if (parts.size == 2) {
                    map[parts[0].trim()] = parts[1].trim()
                }
            }
            map
        }
    }
    
    /**
     * 重写返回按钮事件，实现WebView页面返回功能
     */
    override fun onBackPressed() {
        webViewRef?.let { webView ->
            if (webView.canGoBack()) {
                webView.goBack()
            } else {
                super.onBackPressed()
            }
        } ?: run {
            super.onBackPressed()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme {
    }
}