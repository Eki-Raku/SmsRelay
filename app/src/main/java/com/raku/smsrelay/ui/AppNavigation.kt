package com.raku.smsrelay.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Outbox
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class AppDestination(val label: String, val icon: ImageVector) {
    STATUS("状态", Icons.Outlined.Home),
    MESSAGES("短信", Icons.Outlined.ChatBubbleOutline),
    RELAY("转发", Icons.Outlined.Outbox),
    SETTINGS("设置", Icons.Outlined.Settings),
}

@Composable
fun SmsRelayNavigationBar(
    selected: AppDestination,
    hasUnreadMessages: Boolean,
    onSelect: (AppDestination) -> Unit,
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val navigationShape = RoundedCornerShape(30.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("adaptive-navigation-shell")
            .windowInsetsPadding(
                WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(),
            shape = navigationShape,
            color = surfaceColor.copy(alpha = 0.94f),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)),
            shadowElevation = 12.dp,
            tonalElevation = 0.dp,
        ) {
            Row(Modifier.fillMaxWidth().padding(5.dp)) {
                AppDestination.entries.forEach { destination ->
                    AdaptiveNavigationItem(
                        destination = destination,
                        selected = selected == destination,
                        unread = destination == AppDestination.MESSAGES && hasUnreadMessages,
                        onClick = { onSelect(destination) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun AdaptiveNavigationItem(
    destination: AppDestination,
    selected: Boolean,
    unread: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val background by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        label = "navigation background",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "navigation content",
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.92f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 520f),
        label = "navigation icon scale",
    )

    Box(
        modifier = modifier
            .height(58.dp)
            .background(background, RoundedCornerShape(25.dp))
            .clickable {
                if (!selected) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box {
                Icon(
                    imageVector = destination.icon,
                    contentDescription = destination.label,
                    tint = contentColor,
                    modifier = Modifier.size(23.dp).scale(iconScale),
                )
                if (unread) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .size(7.dp)
                            .background(MaterialTheme.colorScheme.error, CircleShape),
                    )
                }
            }
            Text(
                destination.label,
                modifier = Modifier.padding(top = 3.dp),
                color = contentColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            )
        }
    }
}
