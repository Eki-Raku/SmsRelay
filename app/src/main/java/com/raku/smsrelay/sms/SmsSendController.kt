package com.raku.smsrelay.sms

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager

sealed interface SmsSendRequestResult {
    data class Queued(val providerUri: Uri) : SmsSendRequestResult
    data class Rejected(val reason: String) : SmsSendRequestResult
}

class SmsSendController(
    context: Context,
    private val repository: SystemSmsRepository,
) {
    private val appContext = context.applicationContext

    suspend fun send(
        destination: String,
        body: String,
        subscriptionId: Int? = null,
    ): SmsSendRequestResult {
        val normalizedDestination = SmsSendPolicy.normalizeDestination(destination)
            ?: return SmsSendRequestResult.Rejected("收件号码格式不正确")
        val normalizedBody = body.trim()
        if (normalizedBody.isEmpty()) return SmsSendRequestResult.Rejected("短信正文不能为空")

        val providerUri = repository.insertOutbox(
            address = normalizedDestination,
            body = normalizedBody,
            subscriptionId = subscriptionId,
        )
        val manager = smsManager(subscriptionId)
        val parts = manager.divideMessage(normalizedBody).ifEmpty { arrayListOf(normalizedBody) }
        val sentIntents = ArrayList<PendingIntent>(parts.size)
        val deliveredIntents = ArrayList<PendingIntent>(parts.size)
        parts.indices.forEach { index ->
            sentIntents += resultIntent(
                action = SmsSendResultReceiver.ACTION_SENT,
                providerUri = providerUri,
                partIndex = index,
                partCount = parts.size,
            )
            deliveredIntents += resultIntent(
                action = SmsSendResultReceiver.ACTION_DELIVERED,
                providerUri = providerUri,
                partIndex = index,
                partCount = parts.size,
            )
        }
        if (parts.size == 1) {
            manager.sendTextMessage(
                normalizedDestination,
                null,
                normalizedBody,
                sentIntents.single(),
                deliveredIntents.single(),
            )
        } else {
            manager.sendMultipartTextMessage(
                normalizedDestination,
                null,
                parts,
                sentIntents,
                deliveredIntents,
            )
        }
        return SmsSendRequestResult.Queued(providerUri)
    }

    @Suppress("DEPRECATION")
    private fun smsManager(subscriptionId: Int?): SmsManager {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val base = appContext.getSystemService(SmsManager::class.java)
            return subscriptionId?.let(base::createForSubscriptionId) ?: base
        }
        return subscriptionId?.let(SmsManager::getSmsManagerForSubscriptionId)
            ?: SmsManager.getDefault()
    }

    private fun resultIntent(
        action: String,
        providerUri: Uri,
        partIndex: Int,
        partCount: Int,
    ): PendingIntent {
        val requestCode = (providerUri.toString() + action + partIndex).hashCode()
        val intent = Intent(appContext, SmsSendResultReceiver::class.java)
            .setAction(action)
            .putExtra(SmsSendResultReceiver.EXTRA_PROVIDER_URI, providerUri.toString())
            .putExtra(SmsSendResultReceiver.EXTRA_PART_INDEX, partIndex)
            .putExtra(SmsSendResultReceiver.EXTRA_PART_COUNT, partCount)
        return PendingIntent.getBroadcast(
            appContext,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
