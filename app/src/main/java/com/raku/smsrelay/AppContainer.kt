package com.raku.smsrelay

import android.content.Context
import com.raku.smsrelay.mail.SmtpMailClient
import com.raku.smsrelay.data.AppDatabase
import com.raku.smsrelay.data.SettingsRepository
import com.raku.smsrelay.onboarding.OnboardingRepository
import com.raku.smsrelay.worker.FailureNotifier
import com.raku.smsrelay.worker.ForwardScheduler
import com.raku.smsrelay.receiver.ForwardIngress
import com.raku.smsrelay.role.SmsRoleManager
import com.raku.smsrelay.sms.SystemSmsRepository
import com.raku.smsrelay.sms.SmsSendController
import com.raku.smsrelay.sms.IncomingSmsNotifier

class AppContainer(context: Context) {
    val database: AppDatabase = AppDatabase.create(context)
    val settingsRepository = SettingsRepository(context)
    val onboardingRepository = OnboardingRepository(context)
    val scheduler = ForwardScheduler(context)
    val smsRoleManager = SmsRoleManager(context)
    val systemSmsRepository = SystemSmsRepository(context)
    val smsSendController = SmsSendController(context, systemSmsRepository)
    val incomingSmsNotifier = IncomingSmsNotifier(context)
    val forwardIngress = ForwardIngress(
        settingsRepository = settingsRepository,
        dao = database.forwardMessageDao(),
        scheduler = scheduler,
    )
    val smtpMailClient = SmtpMailClient()
    val failureNotifier = FailureNotifier(context)
}
