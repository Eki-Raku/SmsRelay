package com.raku.smsrelay.mail

import android.util.Log
import com.raku.smsrelay.BuildConfig
import com.raku.smsrelay.data.ForwardMessageEntity
import com.raku.smsrelay.data.SmtpRuntimeConfig
import jakarta.mail.AuthenticationFailedException
import jakarta.mail.Message
import jakarta.mail.MessagingException
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import org.eclipse.angus.mail.smtp.SMTPAddressFailedException
import org.eclipse.angus.mail.smtp.SMTPSendFailedException
import org.eclipse.angus.mail.smtp.SMTPSenderFailedException
import java.io.IOException
import java.util.Date
import java.util.Properties

sealed interface MailSendResult {
    data object Success : MailSendResult
    data class Retryable(val reason: String) : MailSendResult
    data class PermanentFailure(val reason: String) : MailSendResult
}

class SmtpMailClient {
    fun send(config: SmtpRuntimeConfig, message: ForwardMessageEntity): MailSendResult {
        val envelope = MailEnvelopeFactory.from(message)
        val session = Session.getInstance(createProperties())
        var transport: Transport? = null

        return try {
            val mimeMessage = createMimeMessage(
                session = session,
                senderEmail = config.senderEmail,
                recipientEmail = config.recipientEmail,
                envelope = envelope,
            )
            transport = session.getTransport("smtp")
            transport.connect(SmtpConfig.HOST, SmtpConfig.PORT, config.senderEmail, config.authorizationCode)
            transport.sendMessage(mimeMessage, mimeMessage.allRecipients)
            MailSendResult.Success
        } catch (error: AuthenticationFailedException) {
            classifyFailure(error)
        } catch (error: MessagingException) {
            classifyFailure(error)
        } catch (error: IOException) {
            classifyFailure(error)
        } finally {
            runCatching { transport?.close() }
        }
    }

    internal fun createProperties(): Properties = Properties().apply {
        setProperty("mail.transport.protocol", "smtp")
        setProperty("mail.smtp.host", SmtpConfig.HOST)
        setProperty("mail.smtp.port", SmtpConfig.PORT.toString())
        setProperty("mail.smtp.auth", "true")
        setProperty("mail.smtp.starttls.enable", "true")
        setProperty("mail.smtp.starttls.required", "true")
        setProperty("mail.smtp.ssl.checkserveridentity", "true")
        setProperty("mail.smtp.connectiontimeout", SmtpConfig.CONNECTION_TIMEOUT_MS.toString())
        setProperty("mail.smtp.timeout", SmtpConfig.READ_TIMEOUT_MS.toString())
        setProperty("mail.smtp.writetimeout", SmtpConfig.WRITE_TIMEOUT_MS.toString())
        setProperty("mail.debug", "false")
    }

    internal fun createMimeMessage(
        session: Session,
        senderEmail: String,
        recipientEmail: String,
        envelope: MailEnvelope,
    ): MimeMessage = MimeMessage(session).apply {
        setFrom(InternetAddress(senderEmail))
        setRecipient(Message.RecipientType.TO, InternetAddress(recipientEmail))
        setSubject(envelope.subject, Charsets.UTF_8.name())
        sentDate = Date(envelope.receivedAtEpochMs)
        setText(envelope.body, Charsets.UTF_8.name())
        saveChanges()
        setHeader("Message-ID", "<${envelope.messageId}>")
    }

    internal fun classifyFailure(error: Throwable): MailSendResult {
        val chain = throwableChain(error)
        if (BuildConfig.DEBUG) {
            val detail = chain.joinToString(" <- ") { throwable ->
                if (throwable is AuthenticationFailedException) {
                    "${throwable::class.java.simpleName}: <redacted>"
                } else {
                    "${throwable::class.java.simpleName}: ${throwable.message}"
                }
            }
            Log.w(TAG, "SMTP failure detail: $detail")
        }
        if (chain.any { it is AuthenticationFailedException }) {
            return MailSendResult.PermanentFailure("SMTP 授权码被拒绝" + failureDetail(chain))
        }
        val responseCode = chain.firstNotNullOfOrNull { throwable ->
            when (throwable) {
                is SMTPAddressFailedException -> throwable.returnCode
                is SMTPSenderFailedException -> throwable.returnCode
                is SMTPSendFailedException -> throwable.returnCode
                else -> Regex("(?:^|\\s)([45]\\d{2})(?:\\s|$)")
                    .find(throwable.message.orEmpty())
                    ?.groupValues
                    ?.get(1)
                    ?.toIntOrNull()
            }
        }
        return when {
            responseCode in 500..599 -> MailSendResult.PermanentFailure("SMTP 服务器拒绝请求" + failureDetail(chain))
            responseCode in 400..499 -> MailSendResult.Retryable("SMTP 服务暂时不可用" + failureDetail(chain))
            chain.any { it is IOException } -> MailSendResult.Retryable("SMTP 网络连接异常" + failureDetail(chain))
            else -> MailSendResult.Retryable("SMTP 发送失败" + failureDetail(chain))
        }
    }

    /**
     * 在失败原因末尾附带异常类型与摘要，便于自助排障。
     * release/debug 均生效；授权失败类异常不附带 message，避免泄露凭据相关文本。
     */
    private fun failureDetail(chain: List<Throwable>): String {
        val root = chain.firstOrNull() ?: return ""
        if (root is AuthenticationFailedException) return ""
        val message = root.message?.lineSequence()?.firstOrNull().orEmpty().take(160)
        if (message.isBlank()) return ""
        return " [${root::class.java.simpleName}: $message]"
    }

    private fun throwableChain(root: Throwable): List<Throwable> {
        val result = mutableListOf<Throwable>()
        val queue = ArrayDeque<Throwable>()
        val seen = mutableSetOf<Throwable>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (!seen.add(current)) continue
            result += current
            current.cause?.let(queue::addLast)
            if (current is MessagingException) {
                (current.nextException as? Throwable)?.let(queue::addLast)
            }
        }
        return result
    }

    private companion object {
        const val TAG = "SmtpMailClient"
    }
}
