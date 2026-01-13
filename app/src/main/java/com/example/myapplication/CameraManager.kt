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

class CameraManager(private val activity: Activity) {
    companion object {
        const val CAMERA_PERMISSION_REQUEST_CODE = 1002
        const val CAMERA_CAPTURE_REQUEST_CODE = 1003
    }
    
    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST_CODE
        )
    }
    
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