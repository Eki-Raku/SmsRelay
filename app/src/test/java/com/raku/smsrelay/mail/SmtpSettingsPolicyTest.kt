package com.raku.smsrelay.mail

import com.raku.smsrelay.data.SmtpRuntimeConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SmtpSettingsPolicyTest {
    @Test
    fun blankAuthorizationCodeMeansPreserveExistingValue() {
        assertNull(SmtpSettingsPolicy.authorizationCodeUpdate("   "))
        assertEquals("authorization-code", SmtpSettingsPolicy.authorizationCodeUpdate(" authorization-code "))
    }

    @Test
    fun configurationRequiresEnabledValidSenderRecipientAndAuthorizationCode() {
        assertEquals(
            "转发开关已关闭",
            SmtpSettingsPolicy.configurationError(config(enabled = false)),
        )
        assertEquals(
            "QQ 邮箱格式不正确",
            SmtpSettingsPolicy.configurationError(config(senderEmail = "not-qq@example.com")),
        )
        assertEquals(
            "收件邮箱格式不正确",
            SmtpSettingsPolicy.configurationError(config(recipientEmail = "not-an-email")),
        )
        assertEquals(
            "尚未配置 SMTP 授权码",
            SmtpSettingsPolicy.configurationError(config(authorizationCode = "")),
        )
        assertNull(SmtpSettingsPolicy.configurationError(config()))
    }

    private fun config(
        enabled: Boolean = true,
        senderEmail: String = "sender@qq.com",
        recipientEmail: String = "recipient@example.com",
        authorizationCode: String = "authorization-code",
    ) = SmtpRuntimeConfig(
        enabled = enabled,
        senderEmail = senderEmail,
        recipientEmail = recipientEmail,
        authorizationCode = authorizationCode,
    )
}
