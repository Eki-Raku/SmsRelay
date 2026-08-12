package com.raku.smsrelay.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.raku.smsrelay.MainActivity
import com.raku.smsrelay.R

class FailureNotifier(private val context: Context) {
    fun show(messageId: String) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_failures),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(context.getString(R.string.notification_failure_title))
            .setContentText(context.getString(R.string.notification_failure_body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(messageId.hashCode(), notification)
        }
    }

    private companion object {
        const val CHANNEL_ID = "forward-failures"
    }
}

