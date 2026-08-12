package com.raku.smsrelay.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            .verticalScroll(rememberScrollState())
            .padding(
                start = 20.dp,
                top = contentPadding.calculateTopPadding() + 24.dp,
                end = 20.dp,
                bottom = contentPadding.calculateBottomPadding() + 32.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenHeader("CONFIGURATION", "设置", "只保留影响接收与投递可靠性的选项。")
        SectionHeading("系统能力")
        HairlineCard(Modifier.fillMaxWidth()) {
            Column {
                SettingRow(
                    title = "默认短信应用",
                    detail = if (hasSmsRole) "已启用，验证码短信可正常接收" else "未启用，Android 17 验证码可能被过滤",
                ) {
                    if (hasSmsRole) StatusText("已启用")
                    else OutlinedButton(onClick = requestSmsRole) { Text("启用") }
                }
                SettingDivider()
                SettingRow(
                    title = "短信权限",
                    detail = if (permissions.hasCoreSmsPermissions) {
                        "读取、接收与发送短信权限已允许"
                    } else {
                        "缺少：${permissions.missingCorePermissionLabels.joinToString("、")}"
                    },
                ) {
                    if (permissions.hasCoreSmsPermissions) StatusText("已允许")
                    else OutlinedButton(onClick = requestSmsPermissions) { Text("授权") }
                }
                SettingDivider()
                SettingRow(
                    title = "重新查看初始化引导",
                    detail = "再次检查默认角色、短信与通知权限",
                ) {
                    OutlinedButton(onClick = restartOnboarding) { Text("查看") }
                }
                SettingDivider()
                SettingSwitch(
                    title = "自动转发",
                    detail = "将新短信加入邮件投递队列",
                    checked = enabled,
                    onCheckedChange = { enabled = it },
                )
                SettingDivider()
                SettingSwitch(
                    title = "后台常驻",
                    detail = "通过前台服务降低系统清理概率",
                    checked = settings.backgroundResidentEnabled,
                    onCheckedChange = onResidentChange,
                )
                SettingDivider()
                SettingSwitch(
                    title = "开机自启",
                    detail = "设备启动后恢复转发服务",
                    checked = settings.autoStartEnabled,
                    onCheckedChange = onAutoStartChange,
                )
            }
        }

        SectionHeading("邮件投递", "QQ SMTP")
        HairlineCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = senderEmail,
                    onValueChange = { senderEmail = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("发件 QQ 邮箱") },
                    placeholder = { Text("your-account@qq.com") },
                    supportingText = { Text("用于登录 smtp.qq.com，仅支持 QQ 邮箱") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = recipientEmail,
                    onValueChange = { recipientEmail = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("收件邮箱") },
                    placeholder = { Text("archive@example.com") },
                    supportingText = { Text("可与发件邮箱不同，支持任意有效邮箱") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = authorizationCode,
                    onValueChange = { authorizationCode = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (settings.hasAuthorizationCode) "SMTP 授权码 · 已配置" else "SMTP 授权码") },
                    placeholder = { Text(if (settings.hasAuthorizationCode) "留空以保持现有授权码" else "不是 QQ 登录密码") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                )
                Button(
                    onClick = {
                        save(enabled, senderEmail, recipientEmail, authorizationCode)
                        authorizationCode = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("保存设置") }
            }
        }
        if (settings.hasAuthorizationCode) {
            OutlinedButton(onClick = clearAuthorizationCode, modifier = Modifier.fillMaxWidth()) {
                Text("清除 SMTP 授权码")
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Text(
            "授权码经 Android Keystore 加密保存；邮件通过 smtp.qq.com:587 与 STARTTLS 投递。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    detail: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) = SettingRow(title, detail) {
    Switch(checked = checked, onCheckedChange = onCheckedChange)
}

@Composable
private fun SettingRow(
    title: String,
    detail: String,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing()
    }
}

@Composable
private fun SettingDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 16.dp),
        color = MaterialTheme.colorScheme.outline,
    )
}

@Composable
private fun StatusText(text: String) {
    Text(
        text,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.secondaryContainer, androidx.compose.foundation.shape.CircleShape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
    )
}
