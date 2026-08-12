package com.raku.smsrelay.onboarding

import android.content.Context

class OnboardingRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun isCompleted(): Boolean = preferences.getBoolean(KEY_COMPLETED, false)

    fun markCompleted() {
        preferences.edit().putBoolean(KEY_COMPLETED, true).apply()
    }

    fun reset() {
        preferences.edit().putBoolean(KEY_COMPLETED, false).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "onboarding-v1"
        const val KEY_COMPLETED = "completed"
    }
}
