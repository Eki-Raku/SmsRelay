package com.raku.smsrelay.sms

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.raku.smsrelay.MainActivity
import com.raku.smsrelay.R
import com.raku.smsrelay.receiver.ParsedSms

class IncomingSmsNotifier(context: Context) {
    private val appContext = context.applicationContext

    fun show(message: ParsedSms, threadId: Long, unreadCount: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        createChannel()
        val presentation = SmsPresentationFactory.from(message.sender, message.body)
        val openConversation = Intent(appContext, MainActivity::class.java)
            .setAction(ACTION_OPEN_CONVERSATION)
            .putExtra(EXTRA_THREAD_ID, threadId)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val contentIntent = PendingIntent.getActivity(
            appContext,
            notificationId(threadId),
            openConversation,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_sms)
            .setContentTitle(presentation.displaySender)
            .setContentText(presentation.notificationPreview)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message.body))
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setNumber(unreadCount.coerceAtLeast(1))
            .setWhen(message.receivedAtEpochMs)
            .setShowWhen(true)
            .build()

        runCatching {
            NotificationManagerCompat.from(appContext).notify(notificationId(threadId), notification)
        }
    }

    fun dismiss(threadId: Long) {
        NotificationManagerCompat.from(appContext).cancel(notificationId(threadId))
    }

    private fun createChannel() {
        val manager = appContext.getSystemService(NotificationManager::class.java)
        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                appContext.getString(R.string.notification_channel_incoming_sms),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = appContext.getString(R.string.notification_channel_incoming_sms_description)
                setShowBadge(true)
                enableVibration(true)
                setSound(sound, audioAttributes)
            },
        )
    }

    private fun notificationId(threadId: Long): Int =
        (threadId xor (threadId ushr 32)).toInt() and Int.MAX_VALUE

    companion object {
        const val ACTION_OPEN_CONVERSATION = "com.raku.smsrelay.OPEN_CONVERSATION"
        const val EXTRA_THREAD_ID = "thread-id"
        private const val CHANNEL_ID = "incoming-sms-v1"
    }
}
