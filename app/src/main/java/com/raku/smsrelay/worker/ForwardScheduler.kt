package com.raku.smsrelay.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.raku.smsrelay.BuildConfig
import java.util.concurrent.TimeUnit

class ForwardScheduler(context: Context) {
    private val workManager = WorkManager.getInstance(context.applicationContext)

    fun enqueue(messageId: String, replace: Boolean = false) {
        val constraints = Constraints.Builder().apply {
            if (!BuildConfig.DEBUG) setRequiredNetworkType(NetworkType.CONNECTED)
        }.build()
        val request = OneTimeWorkRequestBuilder<ForwardSmsWorker>()
            .setInputData(workDataOf(ForwardSmsWorker.KEY_MESSAGE_ID to messageId))
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        workManager.enqueueUniqueWork(
            "sms-forward-$messageId",
            if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            request,
        )
    }
}
