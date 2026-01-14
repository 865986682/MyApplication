package com.example.myapplication

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * 通知助手类
 * 提供创建通知渠道、请求通知权限和显示通知等功能
 */
object NotificationHelper {
    // 通知渠道ID
    const val CHANNEL_ID = "default_channel"
    // 通知渠道名称
    const val CHANNEL_NAME = "默认通知渠道"
    // 通知权限请求码
    const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    
    /**
     * 创建通知渠道
     * 在Android O及以上版本中必需
     *
     * @param context 应用上下文
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH // 设置为高重要性以实现heads-up通知效果
            ).apply {
                description = "应用默认通知渠道"
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * 检查是否具有通知权限
     * 在Android 13及以上版本需要运行时权限
     *
     * @param context 应用上下文
     * @return 是否拥有通知权限
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13 (API 33) 及以上需要运行时权限
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 13 以下版本不需要运行时权限
            true
        }
    }
    
    /**
     * 请求通知权限
     * 在Android 13及以上版本中请求POST_NOTIFICATIONS权限
     *
     * @param activity Activity上下文
     */
    fun requestNotificationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13 (API 33) 及以上需要运行时权限
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_REQUEST_CODE
            )
        }
    }
    
    /**
     * 显示Heads-up通知
     * Heads-up通知会在屏幕上短暂显示，即使在全屏模式下也能看到
     *
     * @param context 应用上下文
     * @param title 通知标题
     * @param content 通知内容
     * @param intent 点击通知时触发的意图（可选）
     */
    fun showHeadsUpNotification(
        context: Context,
        title: String,
        content: String,
        intent: Intent? = null
    ) {
        // 创建点击通知时的PendingIntent
        val pendingIntent = intent?.let { notificationIntent ->
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            
            PendingIntent.getActivity(
                context, 0, notificationIntent, flags
            )
        }
        
        // 构建通知
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // 使用系统默认图标
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // 高优先级

        // 添加点击意图（如果有）
        pendingIntent?.let {
            builder.setContentIntent(it)
        }
        
        // 显示通知
        with(NotificationManagerCompat.from(context)) {
            notify(System.currentTimeMillis().toInt(), builder.build())
        }
    }
}