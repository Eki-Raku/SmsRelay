package com.raku.smsrelay.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.raku.smsrelay.R
import com.raku.smsrelay.data.ForwardMessageEntity
import com.raku.smsrelay.data.ForwardStatus
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun BrandHeader(detail: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(RelayPalette.Indigo),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.smsrelay_brand_mark),
                contentDescription = "短信信使品牌标识",
                modifier = Modifier.size(39.dp),
                contentScale = ContentScale.Fit,
            )
        }
        Spacer(Modifier.width(RelaySpacing.sm))
        Column(Modifier.weight(1f)) {
            Text("短信信使", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(
                "SMS RELAY",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.1.sp,
            )
        }
        Text(detail, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun ScreenHeader(eyebrow: String, title: String, detail: String) {
    Column {
        Text(
            eyebrow.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.1.sp,
        )
        Spacer(Modifier.height(RelaySpacing.xs))
        Text(title, style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(RelaySpacing.xs))
        Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun SectionHeading(title: String, detail: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = RelaySpacing.sm, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        detail?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
internal fun HairlineCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        content = { content() },
    )
}

@Composable
internal fun EmptyState(title: String, detail: String) {
    HairlineCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(RelaySpacing.xs))
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun StatusPill(status: String) {
    val (background, foreground) = when (status) {
        ForwardStatus.SENT -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        ForwardStatus.FAILED -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    }
    val animatedBackground by animateColorAsState(background, label = "status badge background")
    val animatedForeground by animateColorAsState(foreground, label = "status badge foreground")
    Text(
        text = statusLabel(status),
        modifier = Modifier
            .background(animatedBackground, CircleShape)
            .padding(horizontal = RelaySpacing.sm, vertical = RelaySpacing.xxs),
        color = animatedForeground,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
internal fun RelayMessageCard(
    message: ForwardMessageEntity,
    modifier: Modifier = Modifier,
    retry: ((String) -> Unit)? = null,
) {
    var retrying by remember(message.id) { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(retrying, message.status) {
        if (retrying) {
            if (message.status != ForwardStatus.FAILED) {
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                retrying = false
            } else {
                kotlinx.coroutines.delay(1_000)
                retrying = false
            }
        }
    }
    HairlineCard(
        modifier.fillMaxWidth().animateContentSize(
            if (RelayTheme.motion.reducedMotion) snap() else {
                spring(dampingRatio = 0.86f, stiffness = 420f)
            },
        ),
    ) {
        Column(Modifier.padding(horizontal = RelaySpacing.md, vertical = RelaySpacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (message.isTest) "链路测试" else message.sender,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.width(12.dp))
                StatusPill(message.status)
            }
            Spacer(Modifier.height(8.dp))
            Text(message.body, maxLines = 3, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(RelaySpacing.xs))
            Text(
                "${formatTime(message.receivedAtEpochMs)}${message.simLabel?.let { " · $it" }.orEmpty()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            message.lastError?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(RelaySpacing.xs))
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            if (message.status == ForwardStatus.FAILED && retry != null) {
                Spacer(Modifier.height(RelaySpacing.sm))
                TextButton(
                    onClick = {
                        retrying = true
                        retry(message.id)
                    },
                    enabled = !retrying,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                ) {
                    if (retrying) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("正在重试")
                    } else {
                        Text("重新发送")
                    }
                }
            }
        }
    }
}

private fun statusLabel(status: String): String = when (status) {
    ForwardStatus.PENDING -> "排队中"
    ForwardStatus.SENDING -> "发送中"
    ForwardStatus.SENT -> "已发送"
    ForwardStatus.RETRY -> "待重试"
    ForwardStatus.FAILED -> "失败"
    else -> status
}

internal val UiTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm")
    .withZone(ZoneId.systemDefault())

internal fun formatTime(epochMs: Long): String = UiTimeFormatter.format(Instant.ofEpochMilli(epochMs))
