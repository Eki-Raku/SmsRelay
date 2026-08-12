package com.raku.smsrelay.mail

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
}
