package com.example.myapplication

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * 相机管理类
 * 负责处理Android设备的相机功能，包括权限检查、拍照和图片处理
 *
 * @param activity Activity上下文
 */
class CameraManager(private val activity: Activity) {
    companion object {
        // 相机权限请求码
        const val CAMERA_PERMISSION_REQUEST_CODE = 1002
        // 相机拍照请求码
        const val CAMERA_CAPTURE_REQUEST_CODE = 1003
    }
    
    /**
     * 检查是否具有相机权限
     * @return 是否拥有相机权限
     */
    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * 请求相机权限
     */
    fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST_CODE
        )
    }
    
    /**
     * 启动相机拍照
     */
    fun captureImage() {
        if (!hasCameraPermission()) {
            requestCameraPermission()
            return
        }
        
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(activity.packageManager) != null) {
            activity.startActivityForResult(intent, CAMERA_CAPTURE_REQUEST_CODE)
        }
    }
    
    /**
     * 处理相机拍照结果
     * @param requestCode 请求码
     * @param resultCode 结果码
     * @param data 意图数据
     * @return Base64编码的图片字符串
     */
    fun handleCameraResult(requestCode: Int, resultCode: Int, data: Intent?): String? {
        if (requestCode == CAMERA_CAPTURE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as? Bitmap
            return imageBitmap?.let { bitmap ->
                // 将Bitmap转换为Base64字符串，以便传递给WebView
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream) // 使用JPEG格式，质量90
                val byteArray = outputStream.toByteArray()
                "data:image/jpeg;base64," + android.util.Base64.encodeToString(byteArray, android.util.Base64.DEFAULT)
            }
        }
        return null
    }
}