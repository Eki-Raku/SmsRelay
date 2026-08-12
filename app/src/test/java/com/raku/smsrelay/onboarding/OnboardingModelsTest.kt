package com.raku.smsrelay.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingModelsTest {
    @Test
    fun tourUsesFiveOrderedSteps() {
        assertEquals(
            listOf(
                OnboardingStep.WELCOME,
                OnboardingStep.DEFAULT_SMS,
                OnboardingStep.SMS_PERMISSIONS,
                OnboardingStep.NOTIFICATIONS,
                OnboardingStep.SETTINGS,
            ),
            OnboardingStep.entries,
        )
    }

    @Test
    fun sessionSupportsNextPreviousFinishSkipAndRestart() {
        val initial = OnboardingUiState.initial(completed = false)

        assertTrue(initial.visible)
        assertEquals(OnboardingStep.WELCOME, initial.step)
        assertEquals(OnboardingStep.DEFAULT_SMS, initial.next().step)
        assertEquals(OnboardingStep.WELCOME, initial.next().previous().step)
        assertFalse(initial.dismiss().visible)
        assertEquals(OnboardingStep.WELCOME, initial.dismiss().restart().step)
        assertFalse(
            OnboardingUiState(visible = true, step = OnboardingStep.SETTINGS)
                .next()
                .visible,
        )
        assertFalse(OnboardingUiState.initial(completed = true).visible)
    }

    @Test
    fun smsAccessRequiresReadReceiveAndSendSeparately() {
        val state = MessagingPermissionState(
            canReceiveSms = true,
            canReadSms = false,
            canSendSms = true,
            canReceiveMms = true,
            canReceiveWapPush = true,
            canPostNotifications = true,
        )

        assertFalse(state.hasCoreSmsPermissions)
        assertFalse(state.hasAllMessagingPermissions)
        assertEquals(listOf("读取短信"), state.missingCorePermissionLabels)
        assertEquals(SmsInboxBlockReason.READ_PERMISSION, smsInboxBlockReason(roleHeld = true, state))
        assertEquals(SmsInboxBlockReason.DEFAULT_ROLE, smsInboxBlockReason(roleHeld = false, state))
    }

    @Test
    fun inboxIsAvailableWhenRoleAndReadPermissionArePresent() {
        val state = MessagingPermissionState.allGranted()

        assertTrue(state.hasCoreSmsPermissions)
        assertTrue(state.hasAllMessagingPermissions)
        assertNull(smsInboxBlockReason(roleHeld = true, state))
    }

    @Test
    fun readFailureMessageDoesNotBlameDefaultRoleForEveryException() {
        assertEquals(
            "无法读取系统短信：请在系统权限管理中允许读取短信",
            smsReadFailureMessage(SecurityException("denied")),
        )
        assertEquals(
            "系统短信暂时无法读取，请稍后重试",
            smsReadFailureMessage(IllegalArgumentException("unknown column")),
        )
    }
}
