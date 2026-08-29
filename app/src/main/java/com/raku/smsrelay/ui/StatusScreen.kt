package com.raku.smsrelay.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.dp
import com.raku.smsrelay.data.ForwardMessageEntity
import com.raku.smsrelay.data.ForwardStatus
import com.raku.smsrelay.data.SmtpSettings
import com.raku.smsrelay.onboarding.OnboardingStep

@Composable
fun StatusScreen(
    settings: SmtpSettings,
    relayMessages: List<ForwardMessageEntity>,
    hasSmsPermission: Boolean,
    hasSmsRole: Boolean,
    requestPermissions: () -> Unit,
    requestSmsRole: () -> Unit,
    sendTest: ((Boolean) -> Unit) -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val tourTargets = LocalTourTargetRegistry.current
    var testState by remember { mutableStateOf<UiOperationState>(UiOperationState.Idle) }
    val configured = settings.senderEmail.isNotBlank() &&
        settings.recipientEmail.isNotBlank() && settings.hasAuthorizationCode
    val healthy = settings.enabled && configured && hasSmsPermission && hasSmsRole
    LaunchedEffect(testState) {
        if (testState is UiOperationState.Success) {
            kotlinx.coroutines.delay(1_500)
            testState = UiOperationState.Idle
        }
    }

    LazyColumn(
        modifier = Modifier.padding(top = contentPadding.calculateTopPadding()),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 22.dp,
            end = 20.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            BrandHeader(
                detail = "本机",
                modifier = Modifier.tourTarget(OnboardingStep.WELCOME, tourTargets),
            )
        }
        item {
            StatusHero(
                healthy = healthy,
                hasSmsRole = hasSmsRole,
                hasSmsPermission = hasSmsPermission,
                configured = configured,
                forwardingEnabled = settings.enabled,
                requestSmsRole = requestSmsRole,
                modifier = Modifier
                    .fillMaxWidth()
                    .tourTarget(OnboardingStep.DEFAULT_SMS, tourTargets)
                    .tourTarget(OnboardingStep.SMS_PERMISSIONS, tourTargets)
                    .tourTarget(OnboardingStep.NOTIFICATIONS, tourTargets),
            )
        }
        if (!hasSmsPermission) {
            item {
                RelayButton(
                    onClick = requestPermissions,
                    modifier = Modifier.fillMaxWidth(),
                    text = "授权短信权限",
                )
            }
        }
        item {
            HairlineCard(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Metric("已发送", relayMessages.count { it.status == ForwardStatus.SENT }, Modifier.weight(1f))
                    VerticalDivider(Modifier.height(42.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    Metric(
                        "队列中",
                        relayMessages.count {
                            it.status in setOf(ForwardStatus.PENDING, ForwardStatus.SENDING, ForwardStatus.RETRY)
                        },
                        Modifier.weight(1f),
                    )
                    VerticalDivider(Modifier.height(42.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    Metric("失败", relayMessages.count { it.status == ForwardStatus.FAILED }, Modifier.weight(1f))
                }
            }
        }
        item {
            RelayButton(
                onClick = {
                    testState = UiOperationState.Running
                    sendTest { queued ->
                        testState = if (queued) {
                            UiOperationState.Success
                        } else {
                            UiOperationState.Error("测试消息未能进入发送队列，请重试。")
                        }
                    }
                },
                enabled = settings.enabled && configured,
                modifier = Modifier.fillMaxWidth(),
                text = "测试邮件投递链路",
                state = testState,
            )
            AnimatedVisibility(testState is UiOperationState.Success) {
                Text(
                    "测试消息已进入发送队列，可在最近活动中查看结果。",
                    modifier = Modifier.padding(top = RelaySpacing.xs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AnimatedVisibility(testState is UiOperationState.Error) {
                Text(
                    (testState as? UiOperationState.Error)?.message.orEmpty(),
                    modifier = Modifier.padding(top = RelaySpacing.xs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        item {
            SectionHeading(
                "最近活动",
                relayMessages.takeIf { it.isNotEmpty() }?.let { "最近 ${minOf(3, it.size)} 条" },
            )
        }
        if (relayMessages.isEmpty()) {
            item { EmptyState("暂无转发记录", "收到短信或执行链路测试后，状态会显示在这里。") }
        } else {
            items(relayMessages.take(3), key = { it.id }) {
                RelayMessageCard(it, modifier = Modifier.animateItem())
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: Int, modifier: Modifier) {
    val reducedMotion = RelayTheme.motion.reducedMotion
    Column(modifier.padding(horizontal = RelaySpacing.md)) {
        AnimatedContent(
            targetState = value,
            transitionSpec = {
                (fadeIn() + slideInVertically { if (reducedMotion) 0 else it.coerceAtMost(8) }) togetherWith
                    (fadeOut() + slideOutVertically { if (reducedMotion) 0 else -it.coerceAtMost(8) })
            },
            label = "metric value",
        ) { current ->
            Text(
                current.toString(),
                style = MaterialTheme.typography.headlineSmall.copy(textMotion = TextMotion.Animated),
            )
        }
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
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.outlineVariant,
        ),
        shadowElevation = 0.dp,
    ) {
        Column(Modifier.padding(horizontal = RelaySpacing.lg, vertical = RelaySpacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(
                            if (healthy) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.tertiary,
                        ),
                )
                Spacer(Modifier.size(RelaySpacing.xs))
                Text(
                    if (healthy) "链路健康" else "需要处理",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(RelaySpacing.xl))
            Text(
                when {
                    !hasSmsRole -> "受限模式"
                    healthy -> "完整模式运行中"
                    else -> "尚未就绪"
                },
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(RelaySpacing.xs))
            Text(
                when {
                    !hasSmsRole -> "成为默认短信应用后，可可靠接收 Android 17 的验证码短信。"
                    !hasSmsPermission -> "还需要读取、接收或发送短信权限。"
                    !configured -> "请完成发件、收件邮箱和授权码配置。"
                    !forwardingEnabled -> "转发目前已暂停。"
                    else -> "新短信会进入系统收件箱，并自动投递到配置的邮箱。"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (!hasSmsRole) {
                Spacer(Modifier.height(RelaySpacing.lg))
                RelayButton(
                    onClick = requestSmsRole,
                    text = "设为默认短信应用",
                )
            }
        }
    }
}
