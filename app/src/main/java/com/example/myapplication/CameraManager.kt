package com.example.myapplication

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    
    private var currentPhotoPath: String? = null
    
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
            // 创建临时文件用于存储照片
            val photoFile: File? = try {
                createImageFile()
            } catch (ex: IOException) {
                ex.printStackTrace()
                null
            }
            
            // 如果文件创建成功，将文件URI传递给相机应用
            photoFile?.also {
                val photoURI: Uri = FileProvider.getUriForFile(
                    activity,
                    "com.example.myapplication.fileprovider",
                    it
                )
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                activity.startActivityForResult(intent, CAMERA_CAPTURE_REQUEST_CODE)
            }
        } else {
            // 没有可用的相机应用，显示错误提示
            android.widget.Toast.makeText(activity, "没有可用的相机应用", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 创建图片文件
     */
    @Throws(IOException::class)
    private fun createImageFile(): File {
        // 创建一个以时间戳命名的图片文件
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: activity.filesDir
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* 前缀 */
            ".jpg", /* 后缀 */
            storageDir /* 目录 */
        ).apply {
            // 保存文件路径以供后续使用
            currentPhotoPath = absolutePath
        }
    } // 缺少的结束大括号在这里
    
    /**
     * 处理相机拍照结果
     * @param requestCode 请求码
     * @param resultCode 结果码
     * @param data 意图数据
     * @return 图片的Base64数据URL字符串
     */
    fun handleCameraResult(requestCode: Int, resultCode: Int, data: Intent?): String? {
        if (requestCode == CAMERA_CAPTURE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // 直接返回保存的图片文件路径
            val photoFile = currentPhotoPath?.let { File(it) }
            if (photoFile != null && photoFile.exists()) {
                try {
                    // 读取图片文件并转换为Base64数据URL
                    val inputStream = FileInputStream(photoFile)
                    val fileBytes = inputStream.readBytes()
                    inputStream.close()
                    
                    // 限制图片大小，如果超过2MB则压缩
                    var bytes = fileBytes
                    if (bytes.size > 2 * 1024 * 1024) { // 2MB
                        // 压缩图片
                        val bitmap = BitmapFactory.decodeByteArray(fileBytes, 0, fileBytes.size)
                        val outputStream = ByteArrayOutputStream()
                        
                        // 计算压缩比例
                        var quality = 80
                        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                        bytes = outputStream.toByteArray()
                        
                        // 继续压缩直到大小合适
                        while (bytes.size > 2 * 1024 * 1024 && quality > 20) {
                            outputStream.reset()
                            quality -= 10
                            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                            bytes = outputStream.toByteArray()
                        }
                    }
                    
                    val base64String = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                    return "data:image/jpeg;base64,$base64String"
                } catch (e: Exception) {
                    e.printStackTrace()
                    return null
                }
            }
        }
        return null
    }
}