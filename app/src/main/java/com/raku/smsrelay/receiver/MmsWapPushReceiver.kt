package com.raku.smsrelay.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.raku.smsrelay.R

class MmsWapPushReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.provider.Telephony.WAP_PUSH_DELIVER") return
        context.getSharedPreferences(STORE, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_RECEIVED_AT, System.currentTimeMillis())
            .apply()

        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "不支持的彩信",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            )
        }
        manager.notify(
            NOTIFICATION_ID,
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_sms)
                .setContentTitle("收到一条暂不支持的彩信")
                .setContentText("本次内容无法显示，请联系发送方改用文本短信")
                .setAutoCancel(true)
                .build(),
        )
    }

    companion object {
        const val STORE = "unsupported-mms-v1"
        const val KEY_LAST_RECEIVED_AT = "last-received-at"
        private const val CHANNEL_ID = "unsupported-mms"
        private const val NOTIFICATION_ID = 0x4D4D53
    }
}
