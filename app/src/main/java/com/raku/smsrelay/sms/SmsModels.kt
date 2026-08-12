package com.raku.smsrelay.sms

data class SystemSmsMessage(
    val id: Long,
    val threadId: Long,
    val address: String,
    val body: String,
    val dateEpochMs: Long,
    val type: Int,
    val read: Boolean,
    val subscriptionId: Int?,
)

data class SmsConversation(
    val threadId: Long,
    val address: String,
    val snippet: String,
    val dateEpochMs: Long,
    val unread: Boolean,
)

enum class SmsSendAggregate {
    SENT,
    FAILED,
}

object SmsSendPolicy {
    fun normalizeDestination(value: String): String? {
        val normalized = value.trim()
        return normalized.takeIf {
            it.isNotEmpty() &&
                it.length <= 40 &&
                it.none { character -> character == '\r' || character == '\n' } &&
                it.all { character -> character.isDigit() || character in "+-() " }
        }
    }

    fun aggregate(parts: List<Boolean>): SmsSendAggregate =
        if (parts.isNotEmpty() && parts.all { it }) SmsSendAggregate.SENT else SmsSendAggregate.FAILED
}
