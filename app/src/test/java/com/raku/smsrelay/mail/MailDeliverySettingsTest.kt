package com.raku.smsrelay.mail

import com.raku.smsrelay.data.SmtpSettings
import com.raku.smsrelay.data.canEnableForwarding
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MailDeliverySettingsTest {
    @Test
    fun recipientEmailAcceptsAValidSingleMailbox() {
        assertEquals(
            "ops@example.com",
            SmtpConfig.normalizeRecipientEmail(" OPS@Example.COM "),
        )
    }

    @Test
    fun recipientEmailRejectsMultipleOrMalformedAddresses() {
        assertNull(SmtpConfig.normalizeRecipientEmail("a@example.com,b@example.com"))
        assertNull(SmtpConfig.normalizeRecipientEmail("a@example.com\r\nBcc: attacker@example.com"))
        assertNull(SmtpConfig.normalizeRecipientEmail("missing-at.example.com"))
    }

    @Test
    fun forwardingCanOnlyBeEnabledWithACompleteMailConfiguration() {
        val ready = settings()

        assertTrue(ready.canEnableForwarding)
        assertFalse(ready.copy(senderEmail = "").canEnableForwarding)
        assertFalse(ready.copy(recipientEmail = "").canEnableForwarding)
        assertFalse(ready.copy(hasAuthorizationCode = false).canEnableForwarding)
    }

    private fun settings() = SmtpSettings(
        enabled = false,
        senderEmail = "sender@qq.com",
        recipientEmail = "recipient@example.com",
        hasAuthorizationCode = true,
        autoStartEnabled = true,
        backgroundResidentEnabled = true,
    )
}
