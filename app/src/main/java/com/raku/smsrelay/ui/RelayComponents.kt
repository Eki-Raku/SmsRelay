package com.raku.smsrelay.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

sealed interface UiOperationState {
    data object Idle : UiOperationState
    data object Running : UiOperationState
    data object Success : UiOperationState
    data class Error(val message: String) : UiOperationState
}

@Composable
fun RelayScaffold(
    modifier: Modifier = Modifier,
    includeBottomInset: Boolean = false,
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing.only(
            if (includeBottomInset) {
                WindowInsetsSides.Top + WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
            } else {
                WindowInsetsSides.Top + WindowInsetsSides.Horizontal
            },
        ),
        bottomBar = bottomBar,
        snackbarHost = snackbarHost,
        content = content,
    )
}

@Composable
fun RelayTabBar(
    selected: AppDestination,
    hasUnreadMessages: Boolean,
    onSelect: (AppDestination) -> Unit,
) {
    SmsRelayNavigationBar(selected, hasUnreadMessages, onSelect)
}

@Stable
fun Modifier.relayClickable(
    enabled: Boolean = true,
    role: Role? = null,
    onClick: () -> Unit,
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled && !RelayTheme.motion.reducedMotion) 0.98f else 1f,
        animationSpec = if (RelayTheme.motion.reducedMotion) {
            snap()
        } else if (pressed) {
            androidx.compose.animation.core.tween(RelayTheme.motion.duration(RelayTheme.motion.pressMillis))
        } else {
            spring(dampingRatio = 0.88f, stiffness = 650f)
        },
        label = "relay press",
    )
    scale(scale).clickable(
        interactionSource = interactionSource,
        indication = null,
        enabled = enabled,
        role = role,
        onClick = onClick,
    )
}

@Composable
fun InsetGroup(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        content = content,
    )
}

@Composable
fun RelayRow(
    icon: ImageVector,
    title: String,
    detail: String,
    modifier: Modifier = Modifier,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {},
) {
    val actionModifier = if (onClick == null) modifier else modifier.relayClickable(onClick = onClick)
    Row(
        modifier = actionModifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(horizontal = RelaySpacing.md, vertical = RelaySpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(34.dp).background(iconColor.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        trailing()
    }
}

@Composable
fun RelayGroupDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 60.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
fun RelayToggle(
    checked: Boolean,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val track by androidx.compose.animation.animateColorAsState(
        when {
            !enabled -> MaterialTheme.colorScheme.outlineVariant
            checked -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.outline
        },
        animationSpec = androidx.compose.animation.core.tween(RelayTheme.motion.duration(160)),
        label = "toggle track",
    )
    val offset by animateFloatAsState(
        if (checked) 18f else 2f,
        animationSpec = if (RelayTheme.motion.reducedMotion) snap() else {
            spring(dampingRatio = 0.88f, stiffness = 650f)
        },
        label = "toggle thumb",
    )
    Box(
        modifier
            .size(width = 46.dp, height = 28.dp)
            .background(track, CircleShape)
            .padding(2.dp),
    ) {
        Box(
            Modifier
                .size(24.dp)
                .scale(1f)
                .then(Modifier)
                .graphicsLayer { translationX = offset.dp.toPx() - 2.dp.toPx() }
                .background(Color.White, CircleShape),
        )
    }
}

@Composable
fun RelayTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
        }
        Text(
            title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.size(48.dp))
    }
}

@Composable
fun RelayButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    state: UiOperationState = UiOperationState.Idle,
    enabled: Boolean = true,
) {
    val motion = RelayTheme.motion
    Button(
        onClick = onClick,
        enabled = enabled && state !is UiOperationState.Running,
        modifier = modifier.heightIn(min = 50.dp),
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        AnimatedContent(
            targetState = state,
            transitionSpec = {
                fadeIn(androidx.compose.animation.core.tween(motion.duration(140))) togetherWith
                    fadeOut(androidx.compose.animation.core.tween(motion.duration(140)))
            },
            label = "button operation",
        ) { operation ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (operation) {
                    UiOperationState.Running -> CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    UiOperationState.Success -> Icon(Icons.Outlined.Check, contentDescription = null, Modifier.size(18.dp))
                    is UiOperationState.Error -> Icon(Icons.Outlined.Close, contentDescription = null, Modifier.size(18.dp))
                    UiOperationState.Idle -> Unit
                }
                Text(
                    when (operation) {
                        UiOperationState.Running -> "处理中"
                        UiOperationState.Success -> "已完成"
                        is UiOperationState.Error -> "重试"
                        UiOperationState.Idle -> text
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelaySheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surface,
        content = content,
    )
}
