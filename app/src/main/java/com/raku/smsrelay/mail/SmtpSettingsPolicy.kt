package com.raku.smsrelay.mail

import com.raku.smsrelay.data.SmtpRuntimeConfig

object SmtpSettingsPolicy {
    fun authorizationCodeUpdate(value: String): String? = value.trim().ifBlank { null }

    fun configurationError(config: SmtpRuntimeConfig): String? = when {
        !config.enabled -> "转发开关已关闭"
        SmtpConfig.normalizeQqEmail(config.senderEmail) == null -> "QQ 邮箱格式不正确"
        SmtpConfig.normalizeRecipientEmail(config.recipientEmail) == null -> "收件邮箱格式不正确"
        config.authorizationCode.isBlank() -> "尚未配置 SMTP 授权码"
        else -> null
    }
}
