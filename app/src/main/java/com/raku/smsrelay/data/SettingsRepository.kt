package com.raku.smsrelay.data

import android.content.Context
import com.raku.smsrelay.mail.SmtpConfig
import com.raku.smsrelay.mail.SmtpSettingsPolicy
import com.raku.smsrelay.security.SecureCredentialStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SmtpSettings(
    val enabled: Boolean,
    val senderEmail: String,
    val recipientEmail: String,
    val hasAuthorizationCode: Boolean,
    val autoStartEnabled: Boolean,
    val backgroundResidentEnabled: Boolean,
)

val SmtpSettings.canEnableForwarding: Boolean
    get() = SmtpConfig.normalizeQqEmail(senderEmail) != null &&
        SmtpConfig.normalizeRecipientEmail(recipientEmail) != null &&
        hasAuthorizationCode

data class SmtpRuntimeConfig(
    val enabled: Boolean,
    val senderEmail: String,
    val recipientEmail: String,
    val authorizationCode: String,
)

class SettingsRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val credentialStore = SecureCredentialStore(context)

    private val mutableSettings = MutableStateFlow(loadSettings())
    val settings: StateFlow<SmtpSettings> = mutableSettings.asStateFlow()

    fun current(): SmtpRuntimeConfig {
        val current = mutableSettings.value
        return SmtpRuntimeConfig(
            enabled = current.enabled,
            senderEmail = current.senderEmail,
            recipientEmail = current.recipientEmail,
            authorizationCode = credentialStore.read(),
        )
    }

    fun save(
        enabled: Boolean,
        senderEmail: String,
        recipientEmail: String,
        newAuthorizationCode: String?,
    ) {
        val normalizedSender = SmtpConfig.normalizeQqEmail(senderEmail).orEmpty()
        val normalizedRecipient = SmtpConfig.normalizeRecipientEmail(recipientEmail).orEmpty()
        preferences.edit()
            .putBoolean(KEY_ENABLED, enabled)
            .putString(KEY_SENDER_EMAIL, normalizedSender)
            .putString(KEY_RECIPIENT_EMAIL, normalizedRecipient)
            .apply()

        SmtpSettingsPolicy.authorizationCodeUpdate(newAuthorizationCode.orEmpty())
            ?.let(credentialStore::write)

        mutableSettings.value = loadSettings()
    }

    fun clearAuthorizationCode() {
        credentialStore.clear()
        preferences.edit().putBoolean(KEY_ENABLED, false).apply()
        mutableSettings.value = loadSettings()
    }

    fun setForwardingEnabled(enabled: Boolean): Boolean {
        if (enabled && !mutableSettings.value.canEnableForwarding) return false
        preferences.edit().putBoolean(KEY_ENABLED, enabled).apply()
        mutableSettings.value = loadSettings()
        return true
    }

    fun setAutoStart(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_AUTO_START, enabled).apply()
        mutableSettings.value = loadSettings()
    }

    fun setBackgroundResident(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_BACKGROUND_RESIDENT, enabled).apply()
        mutableSettings.value = loadSettings()
    }

    private fun loadSettings(): SmtpSettings {
        val legacyEmail = preferences.getString(KEY_LEGACY_EMAIL, "").orEmpty()
        return SmtpSettings(
            enabled = preferences.getBoolean(KEY_ENABLED, false),
            senderEmail = preferences.getString(KEY_SENDER_EMAIL, null) ?: legacyEmail,
            recipientEmail = preferences.getString(KEY_RECIPIENT_EMAIL, null) ?: legacyEmail,
            hasAuthorizationCode = credentialStore.hasAuthorizationCode(),
            autoStartEnabled = preferences.getBoolean(KEY_AUTO_START, true),
            backgroundResidentEnabled = preferences.getBoolean(KEY_BACKGROUND_RESIDENT, true),
        )
    }

    private companion object {
        const val PREFERENCES_NAME = "smtp-settings-v1"
        const val KEY_ENABLED = "forwarding-enabled"
        const val KEY_LEGACY_EMAIL = "smtp-email"
        const val KEY_SENDER_EMAIL = "smtp-sender-email"
        const val KEY_RECIPIENT_EMAIL = "smtp-recipient-email"
        const val KEY_AUTO_START = "auto-start-enabled"
        const val KEY_BACKGROUND_RESIDENT = "background-resident-enabled"
    }
}
