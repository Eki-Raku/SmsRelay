package com.raku.smsrelay.mail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmtpConfigTest {
    @Test
    fun normalizesQqEmail() {
        assertEquals("example@qq.com", SmtpConfig.normalizeQqEmail("  Example@QQ.COM "))
    }

    @Test
    fun rejectsNonQqAndMalformedAddresses() {
        assertNull(SmtpConfig.normalizeQqEmail("example@gmail.com"))
        assertNull(SmtpConfig.normalizeQqEmail("@qq.com"))
        assertNull(SmtpConfig.normalizeQqEmail("a@@qq.com"))
        assertNull(SmtpConfig.normalizeQqEmail("a b@qq.com"))
        assertNull(SmtpConfig.normalizeQqEmail("a@qq.com\r\nBcc: attacker@example.com"))
    }

    @Test
    fun usesFixedQqStartTlsEndpoint() {
        assertEquals("smtp.qq.com", SmtpConfig.HOST)
        assertEquals(587, SmtpConfig.PORT)
        assertTrue(SmtpConfig.START_TLS_REQUIRED)
    }
}
