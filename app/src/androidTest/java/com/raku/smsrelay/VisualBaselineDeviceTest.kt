package com.raku.smsrelay

import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import com.raku.smsrelay.data.ForwardMessageEntity
import com.raku.smsrelay.data.ForwardStatus
import com.raku.smsrelay.data.SmtpSettings
import com.raku.smsrelay.onboarding.MessagingPermissionState
import com.raku.smsrelay.sms.SmsConversation
import com.raku.smsrelay.ui.MailSettingsScreen
import com.raku.smsrelay.ui.MessagesScreen
import com.raku.smsrelay.ui.RelayScreen
import com.raku.smsrelay.ui.SettingsScreen
import com.raku.smsrelay.ui.SmsRelayTheme
import com.raku.smsrelay.ui.StatusScreen
import java.io.File
import org.junit.Rule
import org.junit.Test

class VisualBaselineDeviceTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun statusLight() = capture("status-light") {
        StatusScreen(
            settings = readySettings(),
            relayMessages = relayMessages(),
            hasSmsPermission = true,
            hasSmsRole = true,
            requestPermissions = {},
            requestSmsRole = {},
            sendTest = {},
        )
    }

    @Test
    fun statusDark() = capture("status-dark", dark = true) {
        StatusScreen(
            settings = readySettings(),
            relayMessages = relayMessages(),
            hasSmsPermission = true,
            hasSmsRole = true,
            requestPermissions = {},
            requestSmsRole = {},
            sendTest = {},
        )
    }

    @Test
    fun messagesList() = capture("messages-list") {
        MessagesScreen(
            conversations = listOf(
                SmsConversation(1, "10086", "本月套餐余量充足，回复查询更多信息。", 1_777_500_000_000, true),
                SmsConversation(2, "95588", "您的账户服务提醒已送达。", 1_777_400_000_000, false),
            ),
            messages = emptyList(),
            selectedThreadId = null,
            composeRecipient = "",
            hasSmsRole = true,
            permissions = MessagingPermissionState.allGranted(),
            requestSmsRole = {},
            requestSmsPermissions = {},
            openConversation = {},
            closeConversation = {},
            sendSms = { _, _ -> },
        )
    }

    @Test
    fun relayStates() = capture("relay-states") {
        RelayScreen(messages = relayMessages(), retry = {})
    }

    @Test
    fun settingsGroups() = capture("settings-groups") {
        SettingsScreen(
            settings = readySettings(),
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

    @Test
    fun settingsAtTwoTimesText() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                SmsRelayTheme {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        SettingsScreen(
                            settings = readySettings(),
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
                }
            }
        }
        writeRoot("settings-font-2x")
    }

    @Test
    fun mailSettings() = capture("mail-settings") {
        MailSettingsScreen(
            settings = readySettings(),
            save = { _, _, _, _ -> true },
            clearAuthorizationCode = {},
            onBack = {},
        )
    }

    private fun capture(
        name: String,
        dark: Boolean = false,
        content: @androidx.compose.runtime.Composable () -> Unit,
    ) {
        composeRule.setContent {
            SmsRelayTheme(darkTheme = dark) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    content()
                }
            }
        }
        writeRoot(name)
    }

    private fun writeRoot(name: String) {
        composeRule.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val additionalOutput = InstrumentationRegistry.getArguments()
            .getString("additionalTestOutputDir")
            ?.let(::File)
        val directory = File(
            additionalOutput ?: context.getExternalFilesDir(null),
            "visual-baselines",
        ).apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { output ->
            composeRule.onRoot(useUnmergedTree = true)
                .captureToImage()
                .asAndroidBitmap()
                .compress(Bitmap.CompressFormat.PNG, 100, output)
        }
    }

    private fun readySettings() = SmtpSettings(
        enabled = true,
        senderEmail = "sender@qq.com",
        recipientEmail = "recipient@example.com",
        hasAuthorizationCode = true,
        autoStartEnabled = true,
        backgroundResidentEnabled = true,
    )

    private fun relayMessages(): List<ForwardMessageEntity> {
        val now = 1_777_500_000_000
        return listOf(
            ForwardMessageEntity(
                id = "sent",
                dedupeKey = "sent",
                sender = "10086",
                body = "您本月的套餐余量充足。",
                receivedAtEpochMs = now,
                subscriptionId = 1,
                simLabel = "备用机",
                status = ForwardStatus.SENT,
            ),
            ForwardMessageEntity(
                id = "pending",
                dedupeKey = "pending",
                sender = "95588",
                body = "这条消息正在等待邮件投递。",
                receivedAtEpochMs = now - 60_000,
                subscriptionId = 1,
                simLabel = "备用机",
                status = ForwardStatus.PENDING,
            ),
            ForwardMessageEntity(
                id = "failed",
                dedupeKey = "failed",
                sender = "服务通知",
                body = "这条消息用于验证失败与重试状态。",
                receivedAtEpochMs = now - 120_000,
                subscriptionId = 1,
                simLabel = "备用机",
                status = ForwardStatus.FAILED,
                lastError = "SMTP 暂时不可用",
            ),
        )
    }
}
