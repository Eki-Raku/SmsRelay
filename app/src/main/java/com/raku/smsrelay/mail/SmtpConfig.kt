package com.raku.smsrelay.mail

import java.util.Locale

object SmtpConfig {
    const val HOST = "smtp.qq.com"
    const val PORT = 587
    const val START_TLS_REQUIRED = true
    const val CONNECTION_TIMEOUT_MS = 10_000
    const val READ_TIMEOUT_MS = 15_000
    const val WRITE_TIMEOUT_MS = 15_000
    const val MESSAGE_ID_DOMAIN = "smsrelay.local"

    fun normalizeQqEmail(value: String): String? {
        val normalized = normalizeRecipientEmail(value) ?: return null
        return normalized.takeIf { it.endsWith("@qq.com") }
    }

    fun normalizeRecipientEmail(value: String): String? {
        val normalized = value.trim().lowercase(Locale.ROOT)
        val atIndex = normalized.indexOf('@')
        return normalized.takeIf {
            it.length in 3..254 &&
                atIndex in 1 until it.lastIndex &&
                atIndex == it.lastIndexOf('@') &&
                it.substring(atIndex + 1).contains('.') &&
                it.none(Char::isWhitespace) &&
                it.none { character -> character in charArrayOf(',', ';', '<', '>') }
        }
    }
}
