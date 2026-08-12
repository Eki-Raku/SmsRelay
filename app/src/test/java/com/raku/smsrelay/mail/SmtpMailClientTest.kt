package com.raku.smsrelay.mail

import jakarta.mail.AuthenticationFailedException
import jakarta.mail.Message
import jakarta.mail.MessagingException
import jakarta.mail.Session
import jakarta.mail.internet.InternetAddress
import java.io.IOException
import org.eclipse.angus.mail.smtp.SMTPAddressFailedException
import org.eclipse.angus.mail.smtp.SMTPSendFailedException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmtpMailClientTest {
    private val client = SmtpMailClient()

    @Test
    fun requiresAuthenticatedStartTlsWithHostnameVerification() {
        val properties = client.createProperties()

        assertEquals("smtp.qq.com", properties.getProperty("mail.smtp.host"))
        assertEquals("587", properties.getProperty("mail.smtp.port"))
        assertEquals("true", properties.getProperty("mail.smtp.auth"))
        assertEquals("true", properties.getProperty("mail.smtp.starttls.enable"))
        assertEquals("true", properties.getProperty("mail.smtp.starttls.required"))
        assertEquals("true", properties.getProperty("mail.smtp.ssl.checkserveridentity"))
        assertEquals("false", properties.getProperty("mail.debug"))
    }

    @Test
    fun createsUtf8MessageWithIndependentSenderAndRecipient() {
        val envelope = MailEnvelope(
            subject = "[短信] 星河银行",
            body = "验证码 483921",
            messageId = "message-123@smsrelay.local",
            receivedAtEpochMs = 1_700_000_000_000L,
            isTest = false,
        )
        val message = client.createMimeMessage(
            session = Session.getInstance(client.createProperties()),
            senderEmail = "sender@qq.com",
            recipientEmail = "archive@example.com",
            envelope = envelope,
        )

        assertEquals("[短信] 星河银行", message.subject)
        assertEquals("sender@qq.com", message.from.single().toString())
        assertEquals("archive@example.com", message.getRecipients(Message.RecipientType.TO).single().toString())
        assertEquals("<message-123@smsrelay.local>", message.getHeader("Message-ID").single())
        assertEquals("验证码 483921", message.content.toString())
    }

    @Test
    fun classifiesPermanentAndRetryableFailuresWithoutLeakingDetails() {
        val authentication = client.classifyFailure(AuthenticationFailedException("secret authorization code"))
        val wrappedAuthentication = client.classifyFailure(
            MessagingException("outer", AuthenticationFailedException("another secret")),
        )
        val temporary = client.classifyFailure(MessagingException("451 temporary unavailable"))
        val rejected = client.classifyFailure(MessagingException("550 mailbox rejected"))
        val network = client.classifyFailure(MessagingException("wrapped", IOException("socket failed")))

        assertTrue(authentication is MailSendResult.PermanentFailure)
        assertTrue(wrappedAuthentication is MailSendResult.PermanentFailure)
        assertTrue(temporary is MailSendResult.Retryable)
        assertTrue(rejected is MailSendResult.PermanentFailure)
        assertTrue(network is MailSendResult.Retryable)
        assertFalse((authentication as MailSendResult.PermanentFailure).reason.contains("secret"))
    }

    @Test
    fun classifiesNativeAngusReturnCodesWithoutDependingOnExceptionText() {
        val recipientRejected = SMTPAddressFailedException(
            InternetAddress("example@qq.com"),
            "RCPT TO",
            550,
            "mailbox unavailable",
        )
        val serviceBusy = SMTPSendFailedException(
            "DATA",
            451,
            "try later",
            null,
            null,
            null,
            null,
        )

        assertTrue(client.classifyFailure(recipientRejected) is MailSendResult.PermanentFailure)
        assertTrue(client.classifyFailure(serviceBusy) is MailSendResult.Retryable)
    }
}
