package com.raku.smsrelay.receiver

import android.content.Intent
import android.provider.Telephony
import android.telephony.SubscriptionManager

data class ParsedSms(
    val sender: String,
    val body: String,
    val receivedAtEpochMs: Long,
    val subscriptionId: Int?,
)

object SmsParser {
    fun parse(intent: Intent): ParsedSms? {
        if (
            intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION &&
            intent.action != Telephony.Sms.Intents.SMS_DELIVER_ACTION
        ) return null

        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (parts.isEmpty()) return null

        val sender = parts.firstNotNullOfOrNull { it.displayOriginatingAddress }
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: "未知来源"
        val body = parts.joinToString(separator = "") { it.displayMessageBody.orEmpty() }
        if (body.isEmpty()) return null

        val timestamp = parts.first().timestampMillis.takeIf { it > 0L }
            ?: System.currentTimeMillis()
        val rawSubscriptionId = intent.getIntExtra(
            SubscriptionManager.EXTRA_SUBSCRIPTION_INDEX,
            SubscriptionManager.INVALID_SUBSCRIPTION_ID,
        )

        return ParsedSms(
            sender = sender,
            body = body,
            receivedAtEpochMs = timestamp,
            // isValidSubscriptionId() was only added in API 29; non-negative IDs are the
            // platform contract on earlier supported versions as well.
            subscriptionId = rawSubscriptionId.takeIf { it >= 0 },
        )
    }
}
