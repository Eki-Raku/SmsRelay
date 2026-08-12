package com.raku.smsrelay.role

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Telephony

class SmsRoleManager(context: Context) {
    private val appContext = context.applicationContext

    fun isAvailable(): Boolean {
        if (!appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_MESSAGING)) {
            return false
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appContext.getSystemService(RoleManager::class.java)?.isRoleAvailable(RoleManager.ROLE_SMS) == true
        } else {
            true
        }
    }

    fun isHeld(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appContext.getSystemService(RoleManager::class.java)?.isRoleHeld(RoleManager.ROLE_SMS) == true
    } else {
        Telephony.Sms.getDefaultSmsPackage(appContext) == appContext.packageName
    }

    fun createRequestIntent(): Intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        requireNotNull(appContext.getSystemService(RoleManager::class.java))
            .createRequestRoleIntent(RoleManager.ROLE_SMS)
    } else {
        Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).putExtra(
            Telephony.Sms.Intents.EXTRA_PACKAGE_NAME,
            appContext.packageName,
        )
    }
}
