package com.raku.smsrelay

import android.app.Application
import com.raku.smsrelay.service.RelayForegroundService

class SmsRelayApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        // 进程被拉起（用户打开应用 / 开机广播 / 短信广播）时，按用户设置恢复后台常驻服务。
        // Android 12+ 限制后台启动前台服务：若此时由后台广播拉起，尝试会失败，捕获避免崩溃；
        // 后续用户打开应用或收到开机广播时仍会正常拉起。
        if (container.settingsRepository.settings.value.backgroundResidentEnabled) {
            runCatching { RelayForegroundService.start(this) }
        }
    }
}
