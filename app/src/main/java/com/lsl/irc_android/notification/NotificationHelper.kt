package com.lsl.irc_android.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.lsl.irc_android.MainActivity
import com.lsl.irc_android.R

/**
 * 通知辅助类
 * 处理 IRC 消息通知和振动
 */
class NotificationHelper(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "irc_mentions"
        private const val CHANNEL_NAME = "IRC 提及通知"
        private const val CHANNEL_DESCRIPTION = "当有人在 IRC 中提及你时收到通知"
        private const val NOTIFICATION_ID = 1001
    }

    private val notificationManager = NotificationManagerCompat.from(context)
    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(Vibrator::class.java)
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    init {
        createNotificationChannel()
    }

    /**
     * 创建通知渠道 (Android 8.0+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
                setShowBadge(true)
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * 显示提及通知
     */
    fun showMentionNotification(sender: String, message: String, channel: String) {
        // 检查通知权限 (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // 没有权限，不显示通知
                return
            }
        }

        // 创建点击通知时的 Intent
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 构建通知
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications_black_24dp)
            .setContentTitle("$sender 在 $channel 提及了你")
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(message)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .build()

        // 显示通知
        notificationManager.notify(NOTIFICATION_ID, notification)

        // 触发振动
        vibratePhone()
    }

    /**
     * 振动手机
     */
    private fun vibratePhone() {
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Android 8.0+ 使用 VibrationEffect
                it.vibrate(
                    VibrationEffect.createWaveform(
                        longArrayOf(0, 250, 250, 250),
                        -1 // 不重复
                    )
                )
            } else {
                // 旧版本使用简单振动
                @Suppress("DEPRECATION")
                it.vibrate(longArrayOf(0, 250, 250, 250), -1)
            }
        }
    }

    /**
     * 检测消息是否提及了当前用户
     */
    fun isMentioned(message: String, nickname: String): Boolean {
        if (nickname.isBlank()) return false
        
        // 匹配 @昵称 或 昵称: 或 昵称, 等模式
        val patterns = listOf(
            "@$nickname\\b",           // @昵称
            "\\b$nickname:",           // 昵称:
            "\\b$nickname,",           // 昵称,
            "\\b$nickname\\s",         // 昵称 (后面有空格)
        )
        
        return patterns.any { pattern ->
            message.contains(Regex(pattern, RegexOption.IGNORE_CASE))
        }
    }

    /**
     * 取消所有通知
     */
    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }
}
