package com.raku.smsrelay

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.raku.smsrelay.ui.AppDestination
import com.raku.smsrelay.ui.SmsRelayNavigationBar
import com.raku.smsrelay.ui.StatusScreen
import com.raku.smsrelay.ui.SettingsScreen
import com.raku.smsrelay.ui.MessagesScreen
import com.raku.smsrelay.ui.OnboardingTour
import com.raku.smsrelay.data.SmtpSettings
import com.raku.smsrelay.onboarding.MessagingPermissionState
import com.raku.smsrelay.onboarding.OnboardingStep
import com.raku.smsrelay.onboarding.OnboardingUiState
import org.junit.Rule
import org.junit.Test

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
    fun settingsSeparatesSenderAndRecipientFields() {
        composeRule.setContent {
            SettingsScreen(
                settings = settings(),
                save = { _, _, _, _ -> },
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

        composeRule.onNodeWithText("发件 QQ 邮箱").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("收件邮箱").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("重新查看初始化引导").assertIsDisplayed()
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
