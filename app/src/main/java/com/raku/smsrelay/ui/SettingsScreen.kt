package com.raku.smsrelay.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ForwardToInbox
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.raku.smsrelay.data.SmtpSettings
import com.raku.smsrelay.onboarding.MessagingPermissionState

@Composable
fun SettingsScreen(
    settings: SmtpSettings,
    save: (Boolean, String, String, String) -> Unit,
    clearAuthorizationCode: () -> Unit,
    onAutoStartChange: (Boolean) -> Unit,
    onResidentChange: (Boolean) -> Unit,
    hasSmsRole: Boolean,
    permissions: MessagingPermissionState,
    requestSmsRole: () -> Unit,
    requestSmsPermissions: () -> Unit,
    restartOnboarding: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    var enabled by remember(settings.enabled) { mutableStateOf(settings.enabled) }
    var senderEmail by remember(settings.senderEmail) { mutableStateOf(settings.senderEmail) }
    var recipientEmail by remember(settings.recipientEmail) { mutableStateOf(settings.recipientEmail) }
    var authorizationCode by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .padding(
                start = 20.dp,
                top = contentPadding.calculateTopPadding(),
                end = 20.dp,
            )
            .clipToBounds()
            .verticalScroll(rememberScrollState())
            .padding(top = 22.dp, bottom = contentPadding.calculateBottomPadding() + 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenHeader("CONFIGURATION", "设置", "系统能力、后台策略与邮件投递。")

        SectionHeading("系统能力")
        HairlineCard(Modifier.fillMaxWidth().animateContentSize(spring(dampingRatio = 0.88f))) {
            Column {
                SettingRow(
                    icon = Icons.Outlined.Sms,
                    title = "默认短信应用",
                    detail = if (hasSmsRole) "已启用，验证码短信可正常接收" else "未启用，Android 17 验证码可能被过滤",
                ) {
                    if (hasSmsRole) StatusText("已启用") else ActionText("启用", requestSmsRole)
                }
                SettingDivider()
                SettingRow(
                    icon = Icons.Outlined.MarkEmailRead,
                    title = "短信权限",
                    detail = if (permissions.hasCoreSmsPermissions) {
                        "读取、接收与发送短信权限已允许"
                    } else {
                        "缺少：${permissions.missingCorePermissionLabels.joinToString("、")}"
                    },
                ) {
                    if (permissions.hasCoreSmsPermissions) StatusText("已允许") else ActionText("授权", requestSmsPermissions)
                }
                SettingDivider()
                SettingRow(
                    icon = Icons.Outlined.AutoAwesome,
                    title = "重新查看初始化引导",
                    detail = "再次检查默认角色、短信与通知权限",
                ) { ActionText("查看", restartOnboarding) }
            }
        }

        SectionHeading("自动化")
        HairlineCard(Modifier.fillMaxWidth()) {
            Column {
                SettingSwitch(
                    icon = Icons.AutoMirrored.Outlined.ForwardToInbox,
                    title = "自动转发",
                    detail = "将新短信加入邮件投递队列",
                    checked = enabled,
                    onCheckedChange = { enabled = it },
                )
                SettingDivider()
                SettingSwitch(
                    icon = Icons.Outlined.NotificationsActive,
                    title = "后台常驻",
                    detail = "通过前台服务降低系统清理概率",
                    checked = settings.backgroundResidentEnabled,
                    onCheckedChange = onResidentChange,
                )
                SettingDivider()
                SettingSwitch(
                    icon = Icons.Outlined.PowerSettingsNew,
                    title = "开机自启",
                    detail = "设备启动后恢复转发服务",
                    checked = settings.autoStartEnabled,
                    onCheckedChange = onAutoStartChange,
                )
            }
        }

        SectionHeading("邮件投递", "QQ SMTP")
        HairlineCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingsTextField(
                    value = senderEmail,
                    onValueChange = { senderEmail = it },
                    label = "发件 QQ 邮箱",
                    placeholder = "your-account@qq.com",
                    supportingText = "用于登录 smtp.qq.com，仅支持 QQ 邮箱",
                    icon = Icons.Outlined.Email,
                    keyboardType = KeyboardType.Email,
                )
                SettingsTextField(
                    value = recipientEmail,
                    onValueChange = { recipientEmail = it },
                    label = "收件邮箱",
                    placeholder = "archive@example.com",
                    supportingText = "可与发件邮箱不同，支持任意有效邮箱",
                    icon = Icons.AutoMirrored.Outlined.ForwardToInbox,
                    keyboardType = KeyboardType.Email,
                )
                SettingsTextField(
                    value = authorizationCode,
                    onValueChange = { authorizationCode = it },
                    label = if (settings.hasAuthorizationCode) "SMTP 授权码 · 已配置" else "SMTP 授权码",
                    placeholder = if (settings.hasAuthorizationCode) "留空以保持现有授权码" else "不是 QQ 登录密码",
                    icon = Icons.Outlined.Key,
                    password = true,
                )
                Button(
                    onClick = {
                        save(enabled, senderEmail, recipientEmail, authorizationCode)
                        authorizationCode = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    contentPadding = PaddingValues(vertical = 14.dp),
                ) { Text("保存设置") }
            }
        }
        if (settings.hasAuthorizationCode) {
            TextButton(
                onClick = clearAuthorizationCode,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text("清除 SMTP 授权码") }
        }
        Text(
            "授权码经 Android Keystore 加密保存；邮件通过 smtp.qq.com:587 与 STARTTLS 投递。",
            modifier = Modifier.padding(horizontal = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SettingsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: ImageVector,
    supportingText: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    password: Boolean = false,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        supportingText = supportingText?.let { text -> { Text(text) } },
        leadingIcon = { androidx.compose.material3.Icon(icon, contentDescription = null) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}

@Composable
private fun SettingSwitch(
    icon: ImageVector,
    title: String,
    detail: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) = SettingRow(icon, title, detail) {
    Switch(checked = checked, onCheckedChange = onCheckedChange)
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    detail: String,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(34.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.material3.Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(3.dp))
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing()
    }
}

@Composable
private fun SettingDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 60.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
private fun StatusText(text: String) {
    Text(
        text,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun ActionText(text: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}
