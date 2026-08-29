package com.raku.smsrelay.onboarding

enum class OnboardingStep {
    WELCOME,
    DEFAULT_SMS,
    SMS_PERMISSIONS,
    NOTIFICATIONS,
    SETTINGS,
}

data class OnboardingUiState(
    val visible: Boolean,
    val step: OnboardingStep,
) {
    val stepNumber: Int get() = step.ordinal + 1
    val stepCount: Int get() = OnboardingStep.entries.size

    fun next(): OnboardingUiState {
        val next = OnboardingStep.entries.getOrNull(step.ordinal + 1)
        return if (next == null) copy(visible = false) else copy(step = next)
    }

    fun previous(): OnboardingUiState = copy(
        step = OnboardingStep.entries.getOrElse(step.ordinal - 1) { OnboardingStep.WELCOME },
    )

    fun dismiss(): OnboardingUiState = copy(visible = false)

    fun restart(): OnboardingUiState = OnboardingUiState(visible = true, step = OnboardingStep.WELCOME)

    companion object {
        fun initial(completed: Boolean) = OnboardingUiState(
            visible = !completed,
            step = OnboardingStep.WELCOME,
        )
    }
}

data class MessagingPermissionState(
    val canReceiveSms: Boolean,
    val canReadSms: Boolean,
    val canSendSms: Boolean,
    val canReceiveMms: Boolean,
    val canReceiveWapPush: Boolean,
    val canPostNotifications: Boolean,
) {
    val hasCoreSmsPermissions: Boolean
        get() = canReceiveSms && canReadSms && canSendSms

    val hasAllMessagingPermissions: Boolean
        get() = hasCoreSmsPermissions && canReceiveMms && canReceiveWapPush

    val missingCorePermissionLabels: List<String>
        get() = buildList {
            if (!canReadSms) add("读取短信")
            if (!canReceiveSms) add("接收短信")
            if (!canSendSms) add("发送短信")
        }

    companion object {
        fun allGranted() = MessagingPermissionState(
            canReceiveSms = true,
            canReadSms = true,
            canSendSms = true,
            canReceiveMms = true,
            canReceiveWapPush = true,
            canPostNotifications = true,
        )
    }
}

fun OnboardingStep.isSatisfiedBy(
    roleHeld: Boolean,
    permissions: MessagingPermissionState,
): Boolean = when (this) {
    OnboardingStep.WELCOME, OnboardingStep.SETTINGS -> true
    OnboardingStep.DEFAULT_SMS -> roleHeld
    OnboardingStep.SMS_PERMISSIONS -> permissions.hasAllMessagingPermissions
    OnboardingStep.NOTIFICATIONS -> permissions.canPostNotifications
}

enum class SmsInboxBlockReason {
    DEFAULT_ROLE,
    READ_PERMISSION,
}

fun smsInboxBlockReason(
    roleHeld: Boolean,
    permissions: MessagingPermissionState,
): SmsInboxBlockReason? = when {
    !roleHeld -> SmsInboxBlockReason.DEFAULT_ROLE
    !permissions.canReadSms -> SmsInboxBlockReason.READ_PERMISSION
    else -> null
}

fun smsReadFailureMessage(error: Throwable): String = when (error) {
    is SecurityException -> "无法读取系统短信：请在系统权限管理中允许读取短信"
    else -> "系统短信暂时无法读取，请稍后重试"
}
