package com.raku.smsrelay.mail

import com.raku.smsrelay.data.ForwardMessageEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MailEnvelopeTest {
    @Test
    fun buildsOriginalSmsEnvelopeWithStableIdentity() {
        val message = message(
            id = "message-123",
            sender = "95588\r\nInjected",
            body = "验证码 483921\n请勿泄露",
            simLabel = "SIM 1",
            isTest = false,
        )

        val envelope = MailEnvelopeFactory.from(message)

        assertEquals("[验证码] 95588 Injected · 483921", envelope.subject)
        assertEquals("message-123@smsrelay.local", envelope.messageId)
        assertEquals(1_700_000_000_000L, envelope.receivedAtEpochMs)
        assertTrue(envelope.body.contains("验证码 483921\n请勿泄露"))
        assertTrue(envelope.body.contains("发送方 | 95588 Injected"))
        assertTrue(envelope.body.contains("SIM    | SIM 1"))
        assertTrue(envelope.body.endsWith("================================"))
        assertFalse(envelope.isTest)
    }

    @Test
    fun labelsTestMessageAndMissingSim() {
        val envelope = MailEnvelopeFactory.from(
            message(
                id = "test-456",
                sender = "短信信使",
                body = "测试正文",
                simLabel = null,
                isTest = true,
            ),
        )

        assertEquals("[短信测试] 投递链路正常", envelope.subject)
        assertTrue(envelope.body.contains("SIM    | 未知"))
        assertTrue(envelope.body.contains("类型   | 链路测试"))
        assertTrue(envelope.isTest)
    }

    private fun message(
        id: String,
        sender: String,
        body: String,
        simLabel: String?,
        isTest: Boolean,
    ) = ForwardMessageEntity(
        id = id,
        dedupeKey = "dedupe-$id",
        sender = sender,
        body = body,
        receivedAtEpochMs = 1_700_000_000_000L,
        subscriptionId = null,
        simLabel = simLabel,
        isTest = isTest,
    )
}
