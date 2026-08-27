package com.raku.smsrelay.mail

import com.raku.smsrelay.data.ForwardMessageEntity
import com.raku.smsrelay.sms.SmsPresentationFactory
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class MailEnvelope(
    val subject: String,
    val body: String,
    val messageId: String,
    val receivedAtEpochMs: Long,
    val isTest: Boolean,
)

object MailEnvelopeFactory {
    fun from(message: ForwardMessageEntity): MailEnvelope {
        val presentation = SmsPresentationFactory.from(message.sender, message.body)
        val subject = if (message.isTest) {
            "[短信测试] 投递链路正常"
        } else {
            presentation.mailSubject
        }
        val body = buildString {
            appendLine(if (message.isTest) "SMS RELAY · 链路测试" else "SMS RELAY · 新短信")
            appendLine("================================")
            appendLine()
            appendLine(message.body)
            appendLine()
            appendLine("--------------------------------")
            appendLine("发送方 | ${presentation.displaySender}")
            if (presentation.displaySender != presentation.sourceAddress) {
                appendLine("来源号 | ${presentation.sourceAddress}")
            }
            appendLine("接收于 | ${RECEIVED_TIME_FORMATTER.format(Instant.ofEpochMilli(message.receivedAtEpochMs))}")
            appendLine("SIM    | ${message.simLabel ?: "未知"}")
            appendLine("类型   | ${if (message.isTest) "链路测试" else "短信"}")
            appendLine()
            append("================================")
        }

        return MailEnvelope(
            subject = subject,
            body = body,
            messageId = "${message.id}@${SmtpConfig.MESSAGE_ID_DOMAIN}",
            receivedAtEpochMs = message.receivedAtEpochMs,
            isTest = message.isTest,
        )
    }

    private val RECEIVED_TIME_FORMATTER: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z").withZone(ZoneId.systemDefault())
}
