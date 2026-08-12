package com.raku.smsrelay.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.raku.smsrelay.data.ForwardMessageEntity
import com.raku.smsrelay.data.ForwardStatus
import com.raku.smsrelay.data.SmtpSettings

@Composable
fun StatusScreen(
    settings: SmtpSettings,
    relayMessages: List<ForwardMessageEntity>,
    hasSmsPermission: Boolean,
    hasSmsRole: Boolean,
    requestPermissions: () -> Unit,
    requestSmsRole: () -> Unit,
    sendTest: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val configured = settings.senderEmail.isNotBlank() &&
        settings.recipientEmail.isNotBlank() && settings.hasAuthorizationCode
    val healthy = settings.enabled && configured && hasSmsPermission && hasSmsRole

    LazyColumn(
        contentPadding = PaddingValues(
            start = 20.dp,
            top = contentPadding.calculateTopPadding() + 24.dp,
            end = 20.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { BrandHeader(detail = "本机") }
        item {
            StatusHero(
                healthy = healthy,
                hasSmsRole = hasSmsRole,
                hasSmsPermission = hasSmsPermission,
                configured = configured,
                forwardingEnabled = settings.enabled,
                requestSmsRole = requestSmsRole,
            )
        }
        if (!hasSmsPermission) {
            item { OutlinedButton(onClick = requestPermissions, modifier = Modifier.fillMaxWidth()) { Text("授权短信权限") } }
        }
        item {
            HairlineCard(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                Metric("已发送", relayMessages.count { it.status == ForwardStatus.SENT }, Modifier.weight(1f))
                VerticalDivider(Modifier.height(42.dp), color = MaterialTheme.colorScheme.outline)
                Metric(
                    "队列中",
                    relayMessages.count { it.status in setOf(ForwardStatus.PENDING, ForwardStatus.SENDING, ForwardStatus.RETRY) },
                    Modifier.weight(1f),
                )
                VerticalDivider(Modifier.height(42.dp), color = MaterialTheme.colorScheme.outline)
                Metric("失败", relayMessages.count { it.status == ForwardStatus.FAILED }, Modifier.weight(1f))
                }
            }
        }
        item {
            OutlinedButton(
                onClick = sendTest,
                enabled = settings.enabled && configured,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("测试邮件投递链路") }
        }
        item { SectionHeading("最近活动", relayMessages.takeIf { it.isNotEmpty() }?.let { "最近 ${minOf(3, it.size)} 条" }) }
        if (relayMessages.isEmpty()) {
            item { EmptyState("暂无转发记录", "收到短信或执行链路测试后，状态会显示在这里。") }
        } else {
            items(relayMessages.take(3), key = { it.id }) { RelayMessageCard(it) }
        }
    }
}

@Composable
private fun Metric(label: String, value: Int, modifier: Modifier) {
    Column(modifier.padding(horizontal = 14.dp)) {
            Text(value.toString(), style = MaterialTheme.typography.headlineSmall)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StatusHero(
    healthy: Boolean,
    hasSmsRole: Boolean,
    hasSmsPermission: Boolean,
    configured: Boolean,
    forwardingEnabled: Boolean,
    requestSmsRole: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = RelayPalette.Indigo, contentColor = Color.White),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(horizontal = 22.dp, vertical = 21.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(if (healthy) Color(0xFF75C6BA) else Color(0xFFE8BD69)),
                )
                Spacer(Modifier.size(9.dp))
                Text(
                    if (healthy) "链路健康" else "需要处理",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.72f),
                )
            }
            Spacer(Modifier.height(22.dp))
            Text(
                when {
                    !hasSmsRole -> "受限模式"
                    healthy -> "完整模式运行中"
                    else -> "尚未就绪"
                },
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(7.dp))
            Text(
                when {
                    !hasSmsRole -> "成为默认短信应用后，可可靠接收 Android 17 的验证码短信。"
                    !hasSmsPermission -> "还需要读取、接收或发送短信权限。"
                    !configured -> "请完成发件、收件邮箱和授权码配置。"
                    !forwardingEnabled -> "转发目前已暂停。"
                    else -> "新短信会进入系统收件箱，并自动投递到配置的邮箱。"
                },
                color = Color.White.copy(alpha = 0.76f),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (!hasSmsRole) {
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = requestSmsRole,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RelayPalette.Violet,
                        contentColor = RelayPalette.Indigo,
                    ),
                ) { Text("设为默认短信应用") }
            }
        }
    }
}
