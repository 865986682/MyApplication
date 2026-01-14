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