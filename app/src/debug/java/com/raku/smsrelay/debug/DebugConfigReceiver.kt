package com.raku.smsrelay.debug

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.raku.smsrelay.SmsRelayApplication

class DebugConfigReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_CONFIGURE) return

        val senderEmail = intent.getStringExtra(EXTRA_EMAIL)?.trim().orEmpty()
        val recipientEmail = intent.getStringExtra(EXTRA_RECIPIENT_EMAIL)?.trim().orEmpty()
            .ifEmpty { senderEmail }
        val authorizationCode = intent.getStringExtra(EXTRA_AUTHORIZATION_CODE)?.trim().orEmpty()
        if (senderEmail.isEmpty() || recipientEmail.isEmpty() || authorizationCode.isEmpty()) {
            resultCode = RESULT_INVALID_INPUT
            return
        }

        val application = context.applicationContext as? SmsRelayApplication ?: run {
            resultCode = RESULT_NO_APPLICATION
            return
        }
        application.container.settingsRepository.save(
            enabled = intent.getBooleanExtra(EXTRA_ENABLED, true),
            senderEmail = senderEmail,
            recipientEmail = recipientEmail,
            newAuthorizationCode = authorizationCode,
        )
        resultCode = Activity.RESULT_OK
    }

    private companion object {
        const val ACTION_CONFIGURE = "com.raku.smsrelay.DEBUG_CONFIGURE"
        const val EXTRA_EMAIL = "email"
        const val EXTRA_RECIPIENT_EMAIL = "recipientEmail"
        const val EXTRA_AUTHORIZATION_CODE = "authorizationCode"
        const val EXTRA_ENABLED = "enabled"
        const val RESULT_INVALID_INPUT = 2
        const val RESULT_NO_APPLICATION = 3
    }
}
