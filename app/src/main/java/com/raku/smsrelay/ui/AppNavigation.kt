package com.raku.smsrelay.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.annotation.DrawableRes
import com.raku.smsrelay.R

sealed class AppRoute(val route: String) {
    data object Status : AppRoute("status")
    data object Messages : AppRoute("messages")
    data object Relay : AppRoute("relay")
    data object Settings : AppRoute("settings")
    data object MailSettings : AppRoute("mail-settings")
    data object ComposeMessage : AppRoute("compose-message?recipient={recipient}")
    data object Onboarding : AppRoute("onboarding")
    data object Conversation : AppRoute("conversation/{threadId}") {
        fun create(threadId: Long) = "conversation/$threadId"
    }
}

internal fun shouldNavigateToConversation(
    currentRoute: String,
    activeThreadId: Long?,
    targetThreadId: Long,
    onboardingVisible: Boolean,
): Boolean = !onboardingVisible && (
    currentRoute != AppRoute.Conversation.route || activeThreadId != targetThreadId
)

enum class AppDestination(
    val label: String,
    val route: AppRoute,
    @param:DrawableRes val outlinedIcon: Int,
    @param:DrawableRes val filledIcon: Int,
) {
    STATUS("状态", AppRoute.Status, R.drawable.msr_home, R.drawable.msr_home_filled),
    MESSAGES("短信", AppRoute.Messages, R.drawable.msr_sms, R.drawable.msr_sms_filled),
    RELAY("转发", AppRoute.Relay, R.drawable.msr_send, R.drawable.msr_send_filled),
    SETTINGS("设置", AppRoute.Settings, R.drawable.msr_settings, R.drawable.msr_settings_filled),
}

@Composable
fun SmsRelayNavigationBar(
    selected: AppDestination,
    hasUnreadMessages: Boolean,
    onSelect: (AppDestination) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("adaptive-navigation-shell"),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
        tonalElevation = 0.dp,
        shadowElevation = 10.dp,
    ) {
        Column(Modifier.fillMaxWidth()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
            BoxWithConstraints(
                Modifier
                    .fillMaxWidth()
                    .selectableGroup()
                    .windowInsetsPadding(WindowInsets.navigationBars),
            ) {
                val itemWidth = maxWidth / AppDestination.entries.size
                val largeText = LocalDensity.current.fontScale >= 1.5f
                val itemHeight = if (largeText) 88.dp else 64.dp
                val indicatorOffset by animateDpAsState(
                    targetValue = itemWidth * selected.ordinal,
                    animationSpec = if (RelayTheme.motion.reducedMotion) snap() else {
                        androidx.compose.animation.core.tween(
                            RelayTheme.motion.duration(RelayTheme.motion.iconMillis),
                            easing = RelayTheme.motion.standardEasing,
                        )
                    },
                    label = "tab selection position",
                )
                Row(Modifier.fillMaxWidth()) {
                    AppDestination.entries.forEach { destination ->
                        RelayNavigationItem(
                            destination = destination,
                            selected = selected == destination,
                            unread = destination == AppDestination.MESSAGES && hasUnreadMessages,
                            onClick = { onSelect(destination) },
                            modifier = Modifier.weight(1f),
                            showIndicator = false,
                        )
                    }
                }
                Box(
                    Modifier
                        .offset { IntOffset(indicatorOffset.roundToPx(), 0) }
                        .width(itemWidth)
                        .height(itemHeight)
                        .testTag("navigation-selection-indicator-${selected.name}"),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    SelectionLightPoint()
                }
            }
        }
    }
}

@Composable
fun SmsRelayNavigationRail(
    selected: AppDestination,
    hasUnreadMessages: Boolean,
    onSelect: (AppDestination) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxHeight().width(84.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
        shadowElevation = 8.dp,
        tonalElevation = 0.dp,
    ) {
        Row {
            Box(
                Modifier
                    .weight(1f)
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start),
                    )
                    .padding(top = RelaySpacing.lg),
            ) {
                val largeText = LocalDensity.current.fontScale >= 1.5f
                val itemHeight = if (largeText) 88.dp else 64.dp
                val itemStride = itemHeight + 4.dp
                val indicatorOffset by animateDpAsState(
                    targetValue = itemStride * selected.ordinal,
                    animationSpec = if (RelayTheme.motion.reducedMotion) snap() else {
                        androidx.compose.animation.core.tween(
                            RelayTheme.motion.duration(RelayTheme.motion.iconMillis),
                            easing = RelayTheme.motion.standardEasing,
                        )
                    },
                    label = "rail selection position",
                )
                Column(
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
                ) {
                    AppDestination.entries.forEach { destination ->
                        RelayNavigationItem(
                            destination = destination,
                            selected = selected == destination,
                            unread = destination == AppDestination.MESSAGES && hasUnreadMessages,
                            onClick = { onSelect(destination) },
                            modifier = Modifier.fillMaxWidth(),
                            showIndicator = false,
                        )
                    }
                }
                Box(
                    Modifier
                        .offset { IntOffset(0, indicatorOffset.roundToPx()) }
                        .fillMaxWidth()
                        .height(itemHeight)
                        .testTag("navigation-selection-indicator-${selected.name}"),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    SelectionLightPoint()
                }
            }
            androidx.compose.material3.VerticalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}

@Composable
private fun RelayNavigationItem(
    destination: AppDestination,
    selected: Boolean,
    unread: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showIndicator: Boolean = true,
) {
    val haptics = LocalHapticFeedback.current
    val largeText = LocalDensity.current.fontScale >= 1.5f
    val motion = RelayTheme.motion
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = androidx.compose.animation.core.tween(motion.duration(motion.iconMillis)),
        label = "navigation content",
    )
    val iconScale by animateFloatAsState(
        targetValue = if (motion.reducedMotion || selected) 1f else 0.96f,
        animationSpec = if (motion.reducedMotion) {
            androidx.compose.animation.core.snap()
        } else {
            spring(dampingRatio = 0.88f, stiffness = 650f)
        },
        label = "navigation icon scale",
    )

    Column(
        modifier = modifier
            .height(if (largeText) 88.dp else 64.dp)
            .then(
                if (selected && showIndicator) Modifier.testTag("navigation-selection-indicator-${destination.name}")
                else Modifier,
            )
            .semantics { contentDescription = destination.label }
            .selectable(
                selected = selected,
                role = Role.Tab,
                onClick = {
                    if (!selected) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
            )
            .padding(top = RelaySpacing.xs, bottom = RelaySpacing.xxs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Icon(
                painter = painterResource(
                    if (selected) destination.filledIcon else destination.outlinedIcon,
                ),
                contentDescription = null,
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
            modifier = Modifier.padding(top = 2.dp),
            color = contentColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
        Box(
            Modifier.padding(top = 2.dp).size(width = 20.dp, height = 2.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                if (showIndicator) SelectionLightPoint()
            }
        }
    }
}

@Composable
private fun SelectionLightPoint() {
    Box(
        Modifier
            .size(width = 20.dp, height = 2.dp)
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(1.dp)),
    )
}
