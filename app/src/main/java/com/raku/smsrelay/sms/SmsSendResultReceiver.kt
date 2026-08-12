package com.raku.smsrelay.sms

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.raku.smsrelay.SmsRelayApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsSendResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SENT && intent.action != ACTION_DELIVERED) return
        if (intent.action == ACTION_DELIVERED) return
        val uri = intent.getStringExtra(EXTRA_PROVIDER_URI)?.let(Uri::parse) ?: return
        val partCount = intent.getIntExtra(EXTRA_PART_COUNT, 1).coerceAtLeast(1)
        val callbackResultCode = resultCode
        val pendingResult = goAsync()
        callbackScope.launch {
            try {
                val application = context.applicationContext as? SmsRelayApplication ?: return@launch
                if (callbackResultCode != Activity.RESULT_OK) {
                    application.container.systemSmsRepository.markFailed(uri, callbackResultCode)
                    clearProgress(context, uri)
                    return@launch
                }
                val progress = context.getSharedPreferences(PROGRESS_STORE, Context.MODE_PRIVATE)
                val key = uri.toString()
                val successfulParts = progress.getInt(key, 0) + 1
                if (successfulParts >= partCount) {
                    application.container.systemSmsRepository.markSent(uri)
                    clearProgress(context, uri)
                } else {
                    progress.edit().putInt(key, successfulParts).apply()
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun clearProgress(context: Context, uri: Uri) {
        context.getSharedPreferences(PROGRESS_STORE, Context.MODE_PRIVATE)
            .edit()
            .remove(uri.toString())
            .apply()
    }

    companion object {
        const val ACTION_SENT = "com.raku.smsrelay.SMS_SENT"
        const val ACTION_DELIVERED = "com.raku.smsrelay.SMS_DELIVERED"
        const val EXTRA_PROVIDER_URI = "provider-uri"
        const val EXTRA_PART_INDEX = "part-index"
        const val EXTRA_PART_COUNT = "part-count"
        private const val PROGRESS_STORE = "sms-send-progress-v1"
        private val callbackScope = CoroutineScope(Dispatchers.IO.limitedParallelism(1))
    }
}
