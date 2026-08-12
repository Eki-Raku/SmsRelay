package com.raku.smsrelay.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.raku.smsrelay.SmsRelayApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class RespondViaMessageService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val destination = intent?.data?.schemeSpecificPart.orEmpty()
        val body = intent?.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        val application = application as? SmsRelayApplication
        if (application == null || destination.isBlank() || body.isBlank()) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        scope.launch {
            runCatching { application.container.smsSendController.send(destination, body) }
            stopSelf(startId)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
