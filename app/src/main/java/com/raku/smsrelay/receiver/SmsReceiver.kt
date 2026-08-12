package com.raku.smsrelay.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.raku.smsrelay.SmsRelayApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val application = context.applicationContext as? SmsRelayApplication ?: return@launch
                if (application.container.smsRoleManager.isHeld()) return@launch
                val parsed = SmsParser.parse(intent) ?: return@launch
                application.container.forwardIngress.accept(parsed)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
