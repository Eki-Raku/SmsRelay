package com.raku.smsrelay.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.raku.smsrelay.SmsRelayApplication
import com.raku.smsrelay.data.ForwardStatus
import com.raku.smsrelay.mail.MailSendResult
import com.raku.smsrelay.mail.SmtpSettingsPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ForwardSmsWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val messageId = inputData.getString(KEY_MESSAGE_ID) ?: return Result.failure()
        val application = applicationContext as? SmsRelayApplication ?: return Result.failure()
        val container = application.container
        val dao = container.database.forwardMessageDao()
        val message = dao.getById(messageId) ?: return Result.success()
        if (message.status == ForwardStatus.SENT) return Result.success()

        val config = container.settingsRepository.current()
        val configurationError = SmtpSettingsPolicy.configurationError(config)
        if (configurationError != null) {
            dao.markFailed(messageId, configurationError)
            container.failureNotifier.show(messageId)
            return Result.success()
        }

        dao.markSending(messageId)
        return try {
            val smtpResult = withContext(Dispatchers.IO) {
                container.smtpMailClient.send(config, message)
            }
            when (smtpResult) {
                MailSendResult.Success -> {
                    dao.markSent(messageId, System.currentTimeMillis())
                    Result.success()
                }

                is MailSendResult.PermanentFailure -> {
                    dao.markFailed(messageId, smtpResult.reason)
                    container.failureNotifier.show(messageId)
                    Result.success()
                }

                is MailSendResult.Retryable -> retryOrFail(messageId, smtpResult.reason)
            }
        } catch (_: Exception) {
            retryOrFail(messageId, "发送过程中发生异常")
        }
    }

    private suspend fun retryOrFail(messageId: String, reason: String): Result {
        val application = applicationContext as SmsRelayApplication
        val dao = application.container.database.forwardMessageDao()
        return if (runAttemptCount + 1 >= MAX_ATTEMPTS) {
            dao.markFailed(messageId, "$reason，已达到最大重试次数")
            application.container.failureNotifier.show(messageId)
            Result.success()
        } else {
            dao.markRetry(messageId, reason)
            Result.retry()
        }
    }

    companion object {
        const val KEY_MESSAGE_ID = "message-id"
        private const val MAX_ATTEMPTS = 8
    }
}
