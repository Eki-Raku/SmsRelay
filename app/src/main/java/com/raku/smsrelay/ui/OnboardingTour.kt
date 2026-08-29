package com.raku.smsrelay.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.raku.smsrelay.onboarding.MessagingPermissionState
import com.raku.smsrelay.onboarding.OnboardingStep
import com.raku.smsrelay.onboarding.OnboardingUiState
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

@Composable
fun OnboardingTour(
    state: OnboardingUiState,
    hasSmsRole: Boolean,
    permissions: MessagingPermissionState,
    previous: () -> Unit,
    next: () -> Unit,
    skip: () -> Unit,
    requestSmsRole: () -> Unit,
    requestSmsPermissions: () -> Unit,
    requestNotificationPermission: () -> Unit,
    smsPermissionDenied: Boolean = false,
    notificationPermissionDenied: Boolean = false,
    openSystemSettings: () -> Unit = {},
    hazeState: HazeState? = null,
) {
    if (!state.visible) return

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val motion = RelayTheme.motion
        val registry = LocalTourTargetRegistry.current
        val requestedTarget = registry.bounds(state.step)?.let { bounds ->
            with(density) {
                val x = bounds.left.toDp().coerceIn(8.dp, maxWidth - 8.dp)
                val y = bounds.top.toDp().coerceIn(8.dp, maxHeight - 8.dp)
                val width = bounds.width.toDp().coerceAtMost(maxWidth - x - 8.dp)
                val height = bounds.height.toDp().coerceAtMost(maxHeight - y - 8.dp)
                val below = y + height + 16.dp
                TourTarget(
                    x = x,
                    y = y,
                    width = width,
                    height = height,
                    cardTop = if (below + 250.dp < maxHeight) below else (y - 258.dp).coerceAtLeast(16.dp),
                )
            }
        } ?: tourTarget(state.step, maxWidth, maxHeight)
        val positionSpec = if (motion.reducedMotion) snap<Dp>() else spring(dampingRatio = 0.86f)
        val target = requestedTarget.copy(
            x = animateDpAsState(requestedTarget.x, positionSpec, label = "tour x").value,
            y = animateDpAsState(requestedTarget.y, positionSpec, label = "tour y").value,
            width = animateDpAsState(requestedTarget.width, positionSpec, label = "tour width").value,
            height = animateDpAsState(requestedTarget.height, positionSpec, label = "tour height").value,
            cardTop = animateDpAsState(requestedTarget.cardTop, positionSpec, label = "tour card").value,
        )
        val interactionSource = remember { MutableInteractionSource() }
        val bottomSheetLayout = density.fontScale >= 1.5f || maxWidth < 360.dp || maxHeight < 600.dp

        if (hazeState != null && !motion.reducedMotion) {
            TourBackdropBlur(
                hazeState = hazeState,
                target = target,
                maxWidth = maxWidth,
                maxHeight = maxHeight,
            )
        }

        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {},
                ),
        ) {
            drawRect(Color.Black.copy(alpha = 0.68f))
            with(density) {
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = androidx.compose.ui.geometry.Offset(target.x.toPx(), target.y.toPx()),
                    size = androidx.compose.ui.geometry.Size(target.width.toPx(), target.height.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()),
                    blendMode = BlendMode.Clear,
                )
            }
        }

        Box(
            Modifier
                .offset(target.x, target.y)
                .size(target.width, target.height)
                .testTag("tour-cutout-${state.step.name}")
                .border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.large),
        )

        TourCard(
            state = state,
            hasSmsRole = hasSmsRole,
            permissions = permissions,
            previous = previous,
            next = next,
            skip = skip,
            requestSmsRole = requestSmsRole,
            requestSmsPermissions = requestSmsPermissions,
            requestNotificationPermission = requestNotificationPermission,
            smsPermissionDenied = smsPermissionDenied,
            notificationPermissionDenied = notificationPermissionDenied,
            openSystemSettings = openSystemSettings,
            maxCardHeight = if (bottomSheetLayout) maxHeight - 40.dp else null,
            modifier = if (bottomSheetLayout) {
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 20.dp)
            } else {
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = RelaySpacing.lg)
                    .offset(y = target.cardTop)
            },
        )
    }
}

@Composable
private fun TourBackdropBlur(
    hazeState: HazeState,
    target: TourTarget,
    maxWidth: Dp,
    maxHeight: Dp,
) {
    val style = HazeStyle(
        backgroundColor = MaterialTheme.colorScheme.background,
        tints = listOf(HazeTint(Color.Black.copy(alpha = 0.04f))),
        blurRadius = 18.dp,
        noiseFactor = 0.03f,
    )
    val rightWidth = (maxWidth - target.x - target.width).coerceAtLeast(0.dp)
    val bottomHeight = (maxHeight - target.y - target.height).coerceAtLeast(0.dp)

    Box(Modifier.size(maxWidth, target.y).hazeEffect(hazeState, style))
    Box(
        Modifier
            .offset(y = target.y)
            .size(target.x, target.height)
            .hazeEffect(hazeState, style),
    )
    Box(
        Modifier
            .offset(x = target.x + target.width, y = target.y)
            .size(rightWidth, target.height)
            .hazeEffect(hazeState, style),
    )
    Box(
        Modifier
            .offset(y = target.y + target.height)
            .size(maxWidth, bottomHeight)
            .hazeEffect(hazeState, style),
    )
}

@Composable
private fun TourCard(
    state: OnboardingUiState,
    hasSmsRole: Boolean,
    permissions: MessagingPermissionState,
    previous: () -> Unit,
    next: () -> Unit,
    skip: () -> Unit,
    requestSmsRole: () -> Unit,
    requestSmsPermissions: () -> Unit,
    requestNotificationPermission: () -> Unit,
    smsPermissionDenied: Boolean,
    notificationPermissionDenied: Boolean,
    openSystemSettings: () -> Unit,
    maxCardHeight: Dp?,
    modifier: Modifier,
) {
    val copy = tourCopy(
        state.step,
        hasSmsRole,
        permissions,
        smsPermissionDenied,
        notificationPermissionDenied,
    )
    val motion = RelayTheme.motion
    val usesLargeTextLayout = LocalDensity.current.fontScale >= 1.5f
    val recoveryNeeded = when (state.step) {
        OnboardingStep.SMS_PERMISSIONS -> smsPermissionDenied && !permissions.hasAllMessagingPermissions
        OnboardingStep.NOTIFICATIONS -> notificationPermissionDenied && !permissions.canPostNotifications
        else -> false
    }
    val primaryAction = if (recoveryNeeded) {
        openSystemSettings
    } else {
        when (state.step) {
            OnboardingStep.WELCOME, OnboardingStep.SETTINGS -> next
            OnboardingStep.DEFAULT_SMS -> if (hasSmsRole) next else requestSmsRole
            OnboardingStep.SMS_PERMISSIONS -> {
                if (permissions.hasAllMessagingPermissions) next else requestSmsPermissions
            }
            OnboardingStep.NOTIFICATIONS -> {
                if (permissions.canPostNotifications) next else requestNotificationPermission
            }
        }
    }
    Box(
        modifier
            .widthIn(max = 420.dp)
            .then(maxCardHeight?.let { Modifier.heightIn(max = it) } ?: Modifier),
    ) {
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-6).dp)
                .size(14.dp)
                .graphicsLayer { rotationZ = 45f }
                .background(MaterialTheme.colorScheme.surface),
        )
        Surface(
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = MaterialTheme.shapes.large,
            shadowElevation = 18.dp,
            tonalElevation = 0.dp,
        ) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(RelaySpacing.lg)
                    .animateContentSize(
                        if (motion.reducedMotion) snap() else spring(dampingRatio = 0.88f),
                    ),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${state.stepNumber} / ${state.stepCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "跳过",
                        modifier = Modifier.clickable(onClick = skip).padding(RelaySpacing.xs),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(8.dp))
                AnimatedContent(
                    targetState = copy,
                    transitionSpec = {
                        (fadeIn() + scaleIn(initialScale = if (motion.reducedMotion) 1f else 0.98f)) togetherWith
                            (fadeOut() + scaleOut(targetScale = if (motion.reducedMotion) 1f else 0.98f))
                    },
                    label = "tour copy",
                ) { currentCopy ->
                    Column {
                        Text(currentCopy.title, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(RelaySpacing.xs))
                        Text(
                            currentCopy.body,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                copy.status?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        it,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                            .padding(horizontal = RelaySpacing.sm, vertical = RelaySpacing.xs),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                Spacer(Modifier.height(RelaySpacing.lg))
                if (usesLargeTextLayout) {
                    Column(
                        Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = primaryAction,
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.extraLarge,
                        ) {
                            Text(copy.primaryLabel)
                        }
                        if (state.step != OnboardingStep.WELCOME) {
                            TextButton(onClick = previous, modifier = Modifier.fillMaxWidth()) {
                                Text("上一步")
                            }
                        }
                    }
                } else {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        OnboardingStep.entries.forEach { step ->
                            Box(
                                Modifier
                                    .width(if (step == state.step) 18.dp else 6.dp)
                                    .height(6.dp)
                                    .background(
                                        if (step == state.step) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline,
                                        CircleShape,
                                    ),
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (state.step != OnboardingStep.WELCOME) {
                            TextButton(onClick = previous) { Text("上一步", maxLines = 1) }
                        }
                        Button(onClick = primaryAction, shape = MaterialTheme.shapes.extraLarge) {
                            Text(copy.primaryLabel, maxLines = 1)
                        }
                    }
                }
                }
            }
        }
    }
}

private data class TourCopy(
    val title: String,
    val body: String,
    val primaryLabel: String,
    val status: String? = null,
)

private fun tourCopy(
    step: OnboardingStep,
    hasSmsRole: Boolean,
    permissions: MessagingPermissionState,
    smsPermissionDenied: Boolean,
    notificationPermissionDenied: Boolean,
): TourCopy = when (step) {
    OnboardingStep.WELCOME -> TourCopy(
        title = "欢迎使用短信信使",
        body = "让新短信进入系统收件箱，并通过 QQ SMTP 自动投递到你指定的邮箱。接下来只申请完成这条链路所需的权限。",
        primaryLabel = "开始设置",
    )
    OnboardingStep.DEFAULT_SMS -> TourCopy(
        title = "设为默认短信应用",
        body = "Android 17 只把受保护的验证码短信交给默认短信应用。你可以随时在系统设置中切回其他应用。",
        primaryLabel = if (hasSmsRole) "下一步" else "同意并继续",
        status = if (hasSmsRole) "已是默认短信应用" else "等待系统确认",
    )
    OnboardingStep.SMS_PERMISSIONS -> TourCopy(
        title = "允许读取、接收与发送短信",
        body = "读取权限用于展示系统收件箱；接收权限用于转发新短信；发送权限用于在短信页回复。彩信相关权限只用于满足默认短信角色契约。",
        primaryLabel = when {
            permissions.hasAllMessagingPermissions -> "下一步"
            smsPermissionDenied -> "前往系统设置"
            else -> "同意并继续"
        },
        status = if (permissions.hasAllMessagingPermissions) "短信权限已完整" else if (smsPermissionDenied) {
            "未授权，请在系统设置中恢复后返回"
        } else {
            permissions.missingCorePermissionLabels.joinToString("、").ifBlank { "等待短信权限" }
        },
    )
    OnboardingStep.NOTIFICATIONS -> TourCopy(
        title = "允许通知与后台状态提醒",
        body = "通知用于显示后台常驻和投递失败提醒。小米的自启动与省电白名单仍需在系统权限管理中单独设置。",
        primaryLabel = when {
            permissions.canPostNotifications -> "下一步"
            notificationPermissionDenied -> "前往系统设置"
            else -> "同意并继续"
        },
        status = when {
            permissions.canPostNotifications -> "通知权限已开启"
            notificationPermissionDenied -> "未授权，请在系统设置中恢复后返回"
            else -> "等待通知权限"
        },
    )
    OnboardingStep.SETTINGS -> TourCopy(
        title = "完成邮箱设置",
        body = "进入设置填写发件 QQ 邮箱、可独立配置的收件邮箱和 SMTP 授权码。保存后即可发送链路测试。",
        primaryLabel = "完成",
        status = "以后可在设置中重新查看本引导",
    )
}

private data class TourTarget(
    val x: Dp,
    val y: Dp,
    val width: Dp,
    val height: Dp,
    val cardTop: Dp,
)

private fun tourTarget(step: OnboardingStep, maxWidth: Dp, maxHeight: Dp): TourTarget = when (step) {
    OnboardingStep.WELCOME -> TourTarget(18.dp, 70.dp, maxWidth - 36.dp, 52.dp, 148.dp)
    OnboardingStep.DEFAULT_SMS,
    OnboardingStep.SMS_PERMISSIONS,
    OnboardingStep.NOTIFICATIONS,
    -> TourTarget(18.dp, 134.dp, maxWidth - 36.dp, 146.dp, 300.dp)
    OnboardingStep.SETTINGS -> TourTarget(
        x = maxWidth * 0.76f,
        y = maxHeight - 104.dp,
        width = maxWidth * 0.22f,
        height = 82.dp,
        cardTop = maxHeight - 430.dp,
    )
}
