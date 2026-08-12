package com.raku.smsrelay.mail

import com.raku.smsrelay.data.ForwardMessageEntity
import java.time.Instant

data class MailEnvelope(
    val subject: String,
    val body: String,
    val messageId: String,
    val receivedAtEpochMs: Long,
    val isTest: Boolean,
)

object MailEnvelopeFactory {
    fun from(message: ForwardMessageEntity): MailEnvelope {
        val subjectSender = message.sender
            .replace(Regex("[\\r\\n]+"), " ")
            .trim()
            .ifBlank { "未知号码" }
        val subjectPrefix = if (message.isTest) "[短信测试]" else "[短信]"
        val body = buildString {
            appendLine("发送方：${message.sender}")
            appendLine("接收时间：${Instant.ofEpochMilli(message.receivedAtEpochMs)}")
            appendLine("SIM：${message.simLabel ?: "未知"}")
            appendLine("类型：${if (message.isTest) "链路测试" else "短信"}")
            appendLine()
            append(message.body)
        }

        return MailEnvelope(
            subject = "$subjectPrefix $subjectSender",
            body = body,
            messageId = "${message.id}@${SmtpConfig.MESSAGE_ID_DOMAIN}",
            receivedAtEpochMs = message.receivedAtEpochMs,
            isTest = message.isTest,
        )
    }
}
