package com.raku.smsrelay.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.raku.smsrelay.data.SmtpSettings
import com.raku.smsrelay.data.canEnableForwarding
import com.raku.smsrelay.mail.SmtpConfig
import com.raku.smsrelay.onboarding.MessagingPermissionState
import com.raku.smsrelay.onboarding.OnboardingStep
import kotlinx.coroutines.delay

@Composable
fun SettingsScreen(
    settings: SmtpSettings,
    save: (Boolean, String, String, String) -> Boolean,
    clearAuthorizationCode: () -> Unit,
    onAutoStartChange: (Boolean) -> Unit,
    onResidentChange: (Boolean) -> Unit,
    hasSmsRole: Boolean,
    permissions: MessagingPermissionState,
    requestSmsRole: () -> Unit,
    requestSmsPermissions: () -> Unit,
    restartOnboarding: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    openMailSettings: (() -> Unit)? = null,
    onForwardingChange: (Boolean) -> Unit = {},
) {
    val tourTargets = LocalTourTargetRegistry.current
    var enabled by remember(settings.enabled) { mutableStateOf(settings.enabled) }
    var showingInternalMailSettings by remember { mutableStateOf(false) }
    if (showingInternalMailSettings) {
        MailSettingsScreen(
            settings = settings.copy(enabled = enabled),
            save = save,
            clearAuthorizationCode = clearAuthorizationCode,
            onBack = { showingInternalMailSettings = false },
            contentPadding = contentPadding,
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = RelaySpacing.lg,
                top = contentPadding.calculateTopPadding(),
                end = RelaySpacing.lg,
            )
            .verticalScroll(rememberScrollState())
            .padding(top = RelaySpacing.lg, bottom = contentPadding.calculateBottomPadding() + RelaySpacing.xxl),
        verticalArrangement = Arrangement.spacedBy(RelaySpacing.sm),
    ) {
        ScreenHeader("SETTINGS", "设置", "系统能力、自动化与邮件投递。")

        SettingsGroupTitle("系统能力")
        InsetGroup(
            Modifier.animateContentSize(
                if (RelayTheme.motion.reducedMotion) snap() else spring(dampingRatio = 0.88f),
            ),
        ) {
            Column {
                RelayRow(
                    icon = Icons.Outlined.Email,
                    title = "默认短信应用",
                    detail = if (hasSmsRole) "系统角色已启用" else "需要启用以可靠接收验证码",
                    onClick = if (hasSmsRole) null else requestSmsRole,
                    modifier = Modifier.tourTarget(OnboardingStep.DEFAULT_SMS, tourTargets),
                ) { SettingStatus(if (hasSmsRole) "已启用" else "去启用", hasSmsRole) }
                RelayGroupDivider()
                RelayRow(
                    icon = Icons.Outlined.Check,
                    title = "短信权限",
                    detail = if (permissions.hasCoreSmsPermissions) {
                        "读取、接收与发送短信均已允许"
                    } else {
                        "缺少：${permissions.missingCorePermissionLabels.joinToString("、")}"
                    },
                    onClick = if (permissions.hasCoreSmsPermissions) null else requestSmsPermissions,
                    modifier = Modifier.tourTarget(OnboardingStep.SMS_PERMISSIONS, tourTargets),
                ) { SettingStatus(if (permissions.hasCoreSmsPermissions) "已允许" else "去授权", permissions.hasCoreSmsPermissions) }
                RelayGroupDivider()
                RelayRow(
                    icon = Icons.Outlined.Info,
                    title = "重新查看初始化引导",
                    detail = "重新检查角色、短信与通知权限",
                    onClick = restartOnboarding,
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        SettingsGroupTitle("自动化")
        InsetGroup {
            Column {
                SettingsToggleRow(
                    icon = Icons.AutoMirrored.Outlined.Send,
                    title = "自动转发",
                    detail = if (settings.canEnableForwarding) {
                        "将新短信加入邮件投递队列"
                    } else {
                        "完成邮件投递配置后可开启"
                    },
                    checked = enabled,
                    enabled = settings.canEnableForwarding,
                    tag = "settings-toggle-auto-forward",
                    onCheckedChange = {
                        enabled = it
                        onForwardingChange(it)
                    },
                )
                RelayGroupDivider()
                SettingsToggleRow(
                    icon = Icons.Outlined.Notifications,
                    title = "后台常驻",
                    detail = "降低系统清理转发服务的概率",
                    checked = settings.backgroundResidentEnabled,
                    tag = "settings-toggle-resident",
                    onCheckedChange = onResidentChange,
                )
                RelayGroupDivider()
                SettingsToggleRow(
                    icon = Icons.Outlined.Refresh,
                    title = "开机自启",
                    detail = "设备启动后恢复转发服务",
                    checked = settings.autoStartEnabled,
                    tag = "settings-toggle-auto-start",
                    onCheckedChange = onAutoStartChange,
                )
            }
        }

        SettingsGroupTitle("邮件投递")
        InsetGroup {
            RelayRow(
                icon = Icons.Outlined.Email,
                title = "QQ SMTP",
                detail = mailSummary(settings),
                modifier = Modifier
                    .testTag("mail-settings-entry")
                    .tourTarget(OnboardingStep.SETTINGS, tourTargets),
                onClick = {
                    if (openMailSettings != null) openMailSettings() else showingInternalMailSettings = true
                },
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    SettingStatus(if (mailReady(settings)) "已配置" else "待完善", mailReady(settings))
                    Icon(
                        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
        Text(
            "凭据仅保存在本机 Android Keystore。SmsRelay 未实现端到端加密，请勿用于高安全场景。",
            modifier = Modifier.padding(horizontal = RelaySpacing.xxs),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MailSettingsScreen(
    settings: SmtpSettings,
    save: (Boolean, String, String, String) -> Boolean,
    clearAuthorizationCode: () -> Unit,
    onBack: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    var senderEmail by rememberSaveable(settings.senderEmail) { mutableStateOf(settings.senderEmail) }
    var recipientEmail by rememberSaveable(settings.recipientEmail) { mutableStateOf(settings.recipientEmail) }
    // Credentials must never enter Activity saved state. Recreating this screen clears both values.
    var authorizationCode by remember { mutableStateOf("") }
    var revealCode by remember { mutableStateOf(false) }
    var saveState by remember { mutableStateOf<UiOperationState>(UiOperationState.Idle) }
    var confirmClear by remember { mutableStateOf(false) }
    val senderError = senderEmail.isNotBlank() && SmtpConfig.normalizeQqEmail(senderEmail) == null
    val recipientError = recipientEmail.isNotBlank() && SmtpConfig.normalizeRecipientEmail(recipientEmail) == null
    val changedSenderNeedsCode = settings.hasAuthorizationCode &&
        settings.senderEmail.isNotBlank() &&
        SmtpConfig.normalizeQqEmail(senderEmail) != settings.senderEmail &&
        authorizationCode.isBlank()
    val canSave = senderEmail.isNotBlank() && recipientEmail.isNotBlank() && !senderError && !recipientError &&
        !changedSenderNeedsCode && (settings.hasAuthorizationCode || authorizationCode.isNotBlank())

    LaunchedEffect(saveState) {
        if (saveState is UiOperationState.Success) {
            delay(1_100)
            saveState = UiOperationState.Idle
        }
    }
    Column(
        Modifier
            .fillMaxSize()
            .padding(top = contentPadding.calculateTopPadding())
            .navigationBarsPadding()
            .imePadding(),
    ) {
        RelayTopBar("邮件投递", onBack)
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = RelaySpacing.lg, vertical = RelaySpacing.lg),
            verticalArrangement = Arrangement.spacedBy(RelaySpacing.md),
        ) {
            Text(
                "发件和收件邮箱可独立配置。发件账号必须是已开启 SMTP 的 QQ 邮箱。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            RelayTextField(
                value = senderEmail,
                onValueChange = { senderEmail = it; saveState = UiOperationState.Idle },
                label = "发件 QQ 邮箱",
                placeholder = "your-account@qq.com",
                icon = Icons.Outlined.Email,
                keyboardType = KeyboardType.Email,
                error = if (senderError) "请输入有效的 QQ 邮箱" else null,
            )
            AnimatedVisibility(changedSenderNeedsCode) {
                Text(
                    "更换发件账号时，请填写该账号对应的新授权码。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            RelayTextField(
                value = recipientEmail,
                onValueChange = { recipientEmail = it; saveState = UiOperationState.Idle },
                label = "收件邮箱",
                placeholder = "archive@example.com",
                icon = Icons.AutoMirrored.Outlined.Send,
                keyboardType = KeyboardType.Email,
                error = if (recipientError) "请输入有效的收件邮箱" else null,
            )
            RelayTextField(
                value = authorizationCode,
                onValueChange = { authorizationCode = it; saveState = UiOperationState.Idle },
                label = if (settings.hasAuthorizationCode) "SMTP 授权码 · 已配置" else "SMTP 授权码",
                placeholder = if (settings.hasAuthorizationCode) "留空以保留现有授权码" else "不是 QQ 登录密码",
                icon = Icons.Outlined.Lock,
                password = !revealCode,
                trailing = {
                    TextButton(onClick = { revealCode = !revealCode }) {
                        Text(if (revealCode) "隐藏" else "显示")
                    }
                },
            )
            AnimatedVisibility(saveState is UiOperationState.Error) {
                Text(
                    (saveState as? UiOperationState.Error)?.message.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (settings.hasAuthorizationCode) {
                TextButton(
                    onClick = { confirmClear = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("清除 SMTP 授权码", color = MaterialTheme.colorScheme.error)
                }
            }
            Text(
                "smtp.qq.com:587 · STARTTLS\n授权码经 Android Keystore 加密保存。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
        ) {
            RelayButton(
                text = "保存邮件设置",
                state = saveState,
                enabled = canSave,
                onClick = {
                    if (!canSave) {
                        saveState = UiOperationState.Error("请检查邮箱格式和授权码")
                    } else {
                        saveState = UiOperationState.Running
                        if (save(settings.enabled, senderEmail, recipientEmail, authorizationCode)) {
                            authorizationCode = ""
                            revealCode = false
                            saveState = UiOperationState.Success
                        } else {
                            saveState = UiOperationState.Error("保存失败，请检查配置后重试")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RelaySpacing.lg, vertical = RelaySpacing.sm),
            )
        }
    }

    if (confirmClear) {
        RelaySheet(onDismissRequest = { confirmClear = false }) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = RelaySpacing.lg).padding(bottom = RelaySpacing.xl),
                verticalArrangement = Arrangement.spacedBy(RelaySpacing.sm),
            ) {
                Text("清除 SMTP 授权码？", style = MaterialTheme.typography.titleLarge)
                Text(
                    "清除后自动转发会暂停，重新填写授权码即可恢复。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                RelayButton(
                    text = "确认清除",
                    onClick = {
                        clearAuthorizationCode()
                        confirmClear = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(onClick = { confirmClear = false }, modifier = Modifier.fillMaxWidth()) {
                    Text("取消")
                }
            }
        }
    }
}

@Composable
private fun SettingsGroupTitle(title: String) {
    Text(
        title,
        modifier = Modifier.padding(start = RelaySpacing.sm, top = RelaySpacing.sm, bottom = RelaySpacing.xxs),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    detail: String,
    checked: Boolean,
    enabled: Boolean = true,
    tag: String,
    onCheckedChange: (Boolean) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    RelayRow(
        icon = icon,
        title = title,
        detail = detail,
        modifier = Modifier
            .testTag(tag)
            .semantics {
                stateDescription = when {
                    !enabled -> "不可用，需要先完成邮件投递配置"
                    checked -> "已开启"
                    else -> "已关闭"
                }
            }
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = { value ->
                    haptics.performHapticFeedback(
                        if (value) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff,
                    )
                    onCheckedChange(value)
                },
            ),
    ) {
        RelayToggle(checked = checked, enabled = enabled)
    }
}

@Composable
private fun SettingStatus(text: String, positive: Boolean) {
    val background = if (positive) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val foreground = if (positive) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        text,
        modifier = Modifier
            .background(background, CircleShape)
            .padding(horizontal = RelaySpacing.xs, vertical = RelaySpacing.xxs),
        color = foreground,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
fun RelayTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    password: Boolean = false,
    error: String? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        trailingIcon = trailing,
        supportingText = error?.let { message -> { Text(message) } },
        isError = error != null,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent,
        ),
    )
}

private fun mailReady(settings: SmtpSettings): Boolean = settings.canEnableForwarding

private fun mailSummary(settings: SmtpSettings): String = when {
    settings.senderEmail.isNotBlank() && settings.recipientEmail.isNotBlank() ->
        "${settings.senderEmail} → ${settings.recipientEmail}"
    settings.senderEmail.isNotBlank() -> "${settings.senderEmail} → 待填写"
    else -> "配置发件邮箱、收件邮箱与授权码"
}
