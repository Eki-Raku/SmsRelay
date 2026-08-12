package com.raku.smsrelay.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.raku.smsrelay.MainActivity
import com.raku.smsrelay.R

/**
 * 后台常驻前台服务。
 *
 * 作为应用的存活锚点：提供低优先级常驻通知，降低被系统或厂商激进省电策略清理的概率，
 * 保证收到短信广播时进程可用、WorkManager 能及时发送。服务本身不执行任何转发工作，
 * 转发逻辑全部走 SmsReceiver -> Room -> WorkManager。
 */
class RelayForegroundService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAsForeground()
        return START_STICKY
    }

    override fun onDestroy() {
        // START_STICKY：系统会尝试重建服务；这里仅在显式停止时被调用。
        super.onDestroy()
    }

    private fun startAsForeground() {
        createChannelIfNeeded()
        val notification = buildNotification()
        // API 34+ 使用 specialUse 类型；更低版本传 0（ServiceCompat 内部按版本处理）。
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "短信转发服务",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "保持短信转发服务在后台运行"
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_sms)
            .setContentTitle("短信转发服务运行中")
            .setContentText("收到短信后会自动发送到 QQ 邮箱")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setShowWhen(false)
            .setContentIntent(contentIntent)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "relay-foreground-service"
        private const val NOTIFICATION_ID = 0x526C // "Rl"

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, RelayForegroundService::class.java),
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, RelayForegroundService::class.java))
        }
    }
}
