package com.example.myapplication

import android.app.Activity
import android.content.Intent
import android.util.Log
import com.google.zxing.integration.android.IntentIntegrator

/**
 * 二维码扫描管理器类
 * 用于处理二维码扫描功能
 */
class QRCodeScannerManager(private val activity: Activity) {

    companion object {
        private const val TAG = "QRCodeScannerManager"
    }

    /**
     * 开始扫描二维码
     */
    fun startQRCodeScan() {
        try {
            val integrator = IntentIntegrator(activity)
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            integrator.setPrompt("扫描二维码")
            integrator.setCameraId(0)  // Use a specific camera of the device
            integrator.setBeepEnabled(false)
            integrator.setBarcodeImageEnabled(true)
            // 设置为竖屏扫描
            integrator.setOrientationLocked(false)

            // 自定义扫描窗口样式，更适合竖屏显示
            integrator.setCaptureActivity(CustomCaptureActivity::class.java)

            // 设置扫描框的比例和位置，更适合竖屏
            integrator.setBeepEnabled(false) // 关闭声音
            integrator.setOrientationLocked(false) // 允许屏幕旋转

            // 设置扫描框尺寸
            val scanIntent = integrator.createScanIntent()
            scanIntent.putExtra("SCAN_WIDTH", 800)  // 扫描框宽度
            scanIntent.putExtra("SCAN_HEIGHT", 800) // 扫描框高度
            scanIntent.putExtra("MAX_HEIGHT", 1000) // 最大高度
            scanIntent.putExtra("MAX_WIDTH", 1000)  // 最大宽度
            scanIntent.putExtra("MARGIN_TOP", 100)  // 上边距

            // 设置激光样式和其他参数
            scanIntent.putExtra("USE_CAMERA", 0)  // 使用后置摄像头
            scanIntent.putExtra("PROMPT_MESSAGE", "请将二维码置于扫描框内")

            // 使用IntentIntegrator提供的方法启动Activity，而不是直接访问私有字段
            integrator.initiateScan()
        } catch (e: Exception) {
            Log.e(TAG, "无法启动二维码扫描: ${e.message}")
        }
    }

    /**
     * 处理扫描结果
     * @param requestCode 请求码
     * @param resultCode 结果码
     * @param data 返回的数据
     * @return 扫描结果字符串，如果处理失败则返回null
     */
    fun handleResult(requestCode: Int, resultCode: Int, data: Intent?): String? {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        return if (result != null) {
            if (result.contents == null) {
                Log.d(TAG, "取消了扫描")
                "用户取消了扫描"
            } else {
                Log.d(TAG, "扫描结果: ${result.contents}")
                result.contents
            }
        } else {
            null
        }
    }
}