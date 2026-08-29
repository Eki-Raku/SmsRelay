package com.raku.smsrelay

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.raku.smsrelay.ui.AppDestination
import com.raku.smsrelay.ui.SmsRelayNavigationBar
import com.raku.smsrelay.ui.StatusScreen
import com.raku.smsrelay.ui.SettingsScreen
import com.raku.smsrelay.ui.MessagesScreen
import com.raku.smsrelay.ui.OnboardingTour
import com.raku.smsrelay.ui.RelayScreen
import com.raku.smsrelay.data.SmtpSettings
import com.raku.smsrelay.onboarding.MessagingPermissionState
import com.raku.smsrelay.onboarding.OnboardingStep
import com.raku.smsrelay.onboarding.OnboardingUiState
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class DefaultSmsRoleUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun navigationExposesFourPrimaryDestinations() {
        composeRule.setContent {
            SmsRelayNavigationBar(
                selected = AppDestination.STATUS,
                hasUnreadMessages = false,
                onSelect = {},
            )
        }

        listOf("状态", "短信", "转发", "设置").forEach { label ->
            composeRule.onNodeWithText(label).assertIsDisplayed()
        }
    }

    @Test
    fun navigationUsesAnAdaptiveFullWidthShell() {
        composeRule.setContent {
            SmsRelayNavigationBar(
                selected = AppDestination.STATUS,
                hasUnreadMessages = false,
                onSelect = {},
            )
        }

        composeRule.onNodeWithTag("adaptive-navigation-shell")
            .assertWidthIsAtLeast(300.dp)
    }

    @Test
    fun navigationUsesACompactSelectionIndicatorInsteadOfASelectedItemBlock() {
        composeRule.setContent {
            SmsRelayNavigationBar(
                selected = AppDestination.STATUS,
                hasUnreadMessages = false,
                onSelect = {},
            )
        }

        composeRule.onNodeWithTag("navigation-selection-indicator-STATUS")
            .assertIsDisplayed()
    }

    @Test
    fun navigationSelectionLightPointMovesBetweenTabs() {
        val selected = mutableStateOf(AppDestination.STATUS)
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            SmsRelayNavigationBar(
                selected = selected.value,
                hasUnreadMessages = false,
                onSelect = { selected.value = it },
            )
        }

        val start = composeRule.onNodeWithTag("navigation-selection-indicator-STATUS")
            .fetchSemanticsNode().boundsInRoot.left
        composeRule.onNodeWithContentDescription("设置").performClick()
        composeRule.mainClock.advanceTimeBy(80)
        val middle = composeRule.onNodeWithTag("navigation-selection-indicator-SETTINGS")
            .fetchSemanticsNode().boundsInRoot.left
        composeRule.mainClock.advanceTimeBy(200)
        val end = composeRule.onNodeWithTag("navigation-selection-indicator-SETTINGS")
            .fetchSemanticsNode().boundsInRoot.left

        assertTrue("indicator should leave the first tab", middle > start)
        assertTrue("indicator should still be moving at 80ms", middle < end)
    }

    @Test
    fun restrictedStatusOffersDefaultSmsRoleAction() {
        composeRule.setContent {
            StatusScreen(
                settings = settings(),
                relayMessages = emptyList(),
                hasSmsPermission = true,
                hasSmsRole = false,
                requestPermissions = {},
                requestSmsRole = {},
                sendTest = {},
            )
        }

        composeRule.onNodeWithText("受限模式").assertIsDisplayed()
        composeRule.onNodeWithText("设为默认短信应用").assertIsDisplayed()
        composeRule.onNodeWithText("短信信使").assertIsDisplayed()
        composeRule.onNodeWithText("SMS RELAY").assertIsDisplayed()
    }

    @Test
    fun statusDoesNotReportSuccessWhenTestEnqueueFails() {
        composeRule.setContent {
            StatusScreen(
                settings = settings(),
                relayMessages = emptyList(),
                hasSmsPermission = true,
                hasSmsRole = true,
                requestPermissions = {},
                requestSmsRole = {},
                sendTest = { result -> result(false) },
            )
        }

        composeRule.onNodeWithText("测试邮件投递链路").performClick()
        composeRule.onNodeWithText("测试消息未能进入发送队列，请重试。")
            .assertIsDisplayed()
    }

    @Test
    fun settingsSeparatesSenderAndRecipientFields() {
        composeRule.setContent {
            SettingsScreen(
                settings = settings(),
                save = { _, _, _, _ -> true },
                clearAuthorizationCode = {},
                onAutoStartChange = {},
                onResidentChange = {},
                hasSmsRole = true,
                permissions = MessagingPermissionState.allGranted(),
                requestSmsRole = {},
                requestSmsPermissions = {},
                restartOnboarding = {},
            )
        }

        composeRule.onNodeWithText("重新查看初始化引导").assertIsDisplayed()
        composeRule.onNodeWithTag("mail-settings-entry").performClick()
        composeRule.onNodeWithText("发件 QQ 邮箱").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("收件邮箱").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun settingsSummarizesMailDeliveryBeforeOpeningTheEditor() {
        composeRule.setContent {
            SettingsScreen(
                settings = settings(),
                save = { _, _, _, _ -> true },
                clearAuthorizationCode = {},
                onAutoStartChange = {},
                onResidentChange = {},
                hasSmsRole = true,
                permissions = MessagingPermissionState.allGranted(),
                requestSmsRole = {},
                requestSmsPermissions = {},
                restartOnboarding = {},
            )
        }

        composeRule.onNodeWithText("sender@qq.com → recipient@example.com")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("mail-settings-entry").performClick()
        composeRule.onNodeWithText("发件 QQ 邮箱").assertIsDisplayed()
        composeRule.onNodeWithText("收件邮箱").assertIsDisplayed()
    }

    @Test
    fun automationToggleUsesTheWholeSettingRowAsItsTouchTarget() {
        var persistedValue: Boolean? = null
        composeRule.setContent {
            SettingsScreen(
                settings = settings(),
                save = { _, _, _, _ -> true },
                clearAuthorizationCode = {},
                onAutoStartChange = {},
                onResidentChange = {},
                hasSmsRole = true,
                permissions = MessagingPermissionState.allGranted(),
                requestSmsRole = {},
                requestSmsPermissions = {},
                restartOnboarding = {},
                onForwardingChange = { persistedValue = it },
            )
        }

        composeRule.onNodeWithTag("settings-toggle-auto-forward").performClick()
        composeRule.onNodeWithTag("settings-toggle-auto-forward").assertIsOff()
        assertEquals(false, persistedValue)
    }

    @Test
    fun forwardingCannotBeEnabledBeforeMailDeliveryIsConfigured() {
        composeRule.setContent {
            SettingsScreen(
                settings = settings().copy(
                    enabled = false,
                    senderEmail = "",
                    recipientEmail = "",
                    hasAuthorizationCode = false,
                ),
                save = { _, _, _, _ -> true },
                clearAuthorizationCode = {},
                onAutoStartChange = {},
                onResidentChange = {},
                hasSmsRole = true,
                permissions = MessagingPermissionState.allGranted(),
                requestSmsRole = {},
                requestSmsPermissions = {},
                restartOnboarding = {},
            )
        }

        composeRule.onNodeWithTag("settings-toggle-auto-forward")
            .performScrollTo()
            .assertIsOff()
            .assertIsNotEnabled()
    }

    @Test
    fun roleHolderWithoutReadPermissionGetsPermissionAction() {
        composeRule.setContent {
            MessagesScreen(
                conversations = emptyList(),
                messages = emptyList(),
                selectedThreadId = null,
                composeRecipient = "",
                hasSmsRole = true,
                permissions = MessagingPermissionState.allGranted().copy(canReadSms = false),
                requestSmsRole = {},
                requestSmsPermissions = {},
                openConversation = {},
                closeConversation = {},
                sendSms = { _, _ -> },
            )
        }

        composeRule.onNodeWithText("需要短信读取权限").assertIsDisplayed()
        composeRule.onNodeWithText("授权短信权限").assertIsDisplayed()
    }

    @Test
    fun onboardingTourShowsAntStyleProgressAndPermissionCopy() {
        composeRule.setContent {
            OnboardingTour(
                state = OnboardingUiState(visible = true, step = OnboardingStep.SMS_PERMISSIONS),
                hasSmsRole = true,
                permissions = MessagingPermissionState.allGranted().copy(canReadSms = false),
                previous = {},
                next = {},
                skip = {},
                requestSmsRole = {},
                requestSmsPermissions = {},
                requestNotificationPermission = {},
            )
        }

        composeRule.onNodeWithText("3 / 5").assertIsDisplayed()
        composeRule.onNodeWithText("允许读取、接收与发送短信").assertIsDisplayed()
        composeRule.onNodeWithText("同意并继续").assertIsDisplayed()
        composeRule.onNodeWithText("跳过").assertIsDisplayed()
        composeRule.onNodeWithTag("tour-cutout-SMS_PERMISSIONS").assertIsDisplayed()
    }

    @Test
    fun deniedPermissionStepOffersSystemSettingsRecoveryWithoutAdvancing() {
        composeRule.setContent {
            OnboardingTour(
                state = OnboardingUiState(visible = true, step = OnboardingStep.SMS_PERMISSIONS),
                hasSmsRole = true,
                permissions = MessagingPermissionState.allGranted().copy(canReadSms = false),
                previous = {},
                next = {},
                skip = {},
                requestSmsRole = {},
                requestSmsPermissions = {},
                requestNotificationPermission = {},
                smsPermissionDenied = true,
            )
        }

        composeRule.onNodeWithText("3 / 5").assertIsDisplayed()
        composeRule.onNodeWithText("前往系统设置").assertIsDisplayed()
        composeRule.onNodeWithText("未授权，请在系统设置中恢复后返回").assertIsDisplayed()
    }

    @Test
    fun relayFilterExpandsForTwoTimesTextInsteadOfClipping() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                RelayScreen(messages = emptyList(), retry = {})
            }
        }

        composeRule.onNodeWithTag("relay-filter-selection")
            .assertHeightIsAtLeast(80.dp)
    }

    @Test
    fun relayFilterUsesASingleSharedSelectionIndicator() {
        composeRule.setContent {
            RelayScreen(messages = emptyList(), retry = {})
        }

        composeRule.onNodeWithTag("relay-filter-selection").assertIsDisplayed()
    }

    private fun settings() = SmtpSettings(
        enabled = true,
        senderEmail = "sender@qq.com",
        recipientEmail = "recipient@example.com",
        hasAuthorizationCode = true,
        autoStartEnabled = true,
        backgroundResidentEnabled = true,
    )
}
