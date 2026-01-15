package com.example.myapplication

import com.journeyapps.barcodescanner.CaptureActivity

/**
 * 自定义的二维码扫描Activity，用于适配竖屏显示
 */
class CustomCaptureActivity : CaptureActivity() {
    // 这里可以留空，或者添加特定的自定义逻辑
    // 我们主要通过IntentIntegrator进行自定义
}