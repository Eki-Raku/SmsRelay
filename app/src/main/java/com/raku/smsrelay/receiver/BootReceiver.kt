package com.raku.smsrelay.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.raku.smsrelay.SmsRelayApplication
import com.raku.smsrelay.service.RelayForegroundService

/**
 * 开机自启接收器。
 *
 * 系统开机完成后，若用户开启了「后台常驻」，则启动前台服务。
 * 注意：Android 15+ 要求应用至少被启动过一次、且未被「强行停止」，才能收到 BOOT_COMPLETED；
 * 被用户强行停止后需重新打开一次应用。
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val application = context.applicationContext as? SmsRelayApplication ?: return
        if (application.container.settingsRepository.settings.value.backgroundResidentEnabled) {
            RelayForegroundService.start(context)
        }
    }
}
