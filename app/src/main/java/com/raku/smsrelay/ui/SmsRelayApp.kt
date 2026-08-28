package com.raku.smsrelay.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.raku.smsrelay.MainViewModel
import com.raku.smsrelay.onboarding.MessagingPermissionState
import com.raku.smsrelay.onboarding.OnboardingStep
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

@Composable
fun SmsRelayApp(
    viewModel: MainViewModel,
    permissions: MessagingPermissionState,
    hasSmsRole: Boolean,
    requestPermissions: () -> Unit,
    requestSmsRole: () -> Unit,
    requestOnboardingSmsRole: () -> Unit,
    requestOnboardingSmsPermissions: () -> Unit,
    requestOnboardingNotificationPermission: () -> Unit,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val relayMessages by viewModel.messages.collectAsStateWithLifecycle()
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val threadMessages by viewModel.threadMessages.collectAsStateWithLifecycle()
    val selectedThreadId by viewModel.selectedThreadId.collectAsStateWithLifecycle()
    val composeRecipient by viewModel.composeRecipient.collectAsStateWithLifecycle()
    val onboarding by viewModel.onboarding.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var destination by remember { mutableStateOf(AppDestination.STATUS) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { snackbarHostState.showSnackbar(it) }
    }
    LaunchedEffect(destination, hasSmsRole, permissions.canReadSms) {
        if (destination == AppDestination.MESSAGES && hasSmsRole && permissions.canReadSms) {
            viewModel.refreshSms()
        }
    }
    LaunchedEffect(composeRecipient) {
        if (composeRecipient.isNotBlank()) destination = AppDestination.MESSAGES
    }
    LaunchedEffect(selectedThreadId) {
        if (selectedThreadId != null) destination = AppDestination.MESSAGES
    }
    LaunchedEffect(onboarding.visible, onboarding.step) {
        if (onboarding.visible) {
            destination = if (onboarding.step == OnboardingStep.SETTINGS) {
                AppDestination.SETTINGS
            } else {
                AppDestination.STATUS
            }
        }
    }

    SmsRelayTheme {
        val hazeState = rememberHazeState()
        Box(Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
                ),
                bottomBar = {
                    SmsRelayNavigationBar(
                        selected = destination,
                        hasUnreadMessages = conversations.any { it.unread },
                        onSelect = { destination = it },
                    )
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
            ) { padding ->
                AnimatedContent(
                    targetState = destination,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (onboarding.visible) Modifier.hazeSource(hazeState, zIndex = 0f)
                            else Modifier,
                        ),
                    transitionSpec = {
                        val forward = targetState.ordinal > initialState.ordinal
                        val direction = if (forward) {
                            AnimatedContentTransitionScope.SlideDirection.Left
                        } else {
                            AnimatedContentTransitionScope.SlideDirection.Right
                        }
                        (slideIntoContainer(
                            towards = direction,
                            animationSpec = tween(MotionDurationMedium, easing = FastOutSlowInEasing),
                            initialOffset = { it / 5 },
                        ) + fadeIn(tween(MotionDurationShort))) togetherWith
                            (slideOutOfContainer(
                                towards = direction,
                                animationSpec = tween(MotionDurationMedium, easing = FastOutSlowInEasing),
                                targetOffset = { it / 7 },
                            ) + fadeOut(tween(MotionDurationShort)))
                    },
                    label = "primary destination",
                ) { currentDestination ->
                    when (currentDestination) {
                        AppDestination.STATUS -> StatusScreen(
                            settings = settings,
                            relayMessages = relayMessages,
                            hasSmsPermission = permissions.hasCoreSmsPermissions,
                            hasSmsRole = hasSmsRole,
                            requestPermissions = requestPermissions,
                            requestSmsRole = requestSmsRole,
                            sendTest = viewModel::sendTest,
                            contentPadding = padding,
                        )
                        AppDestination.MESSAGES -> MessagesScreen(
                            conversations = conversations,
                            messages = threadMessages,
                            selectedThreadId = selectedThreadId,
                            composeRecipient = composeRecipient,
                            hasSmsRole = hasSmsRole,
                            permissions = permissions,
                            requestSmsRole = requestSmsRole,
                            requestSmsPermissions = requestPermissions,
                            openConversation = viewModel::openConversation,
                            closeConversation = viewModel::closeConversation,
                            sendSms = viewModel::sendSms,
                            contentPadding = padding,
                        )
                        AppDestination.RELAY -> RelayScreen(
                            messages = relayMessages,
                            retry = viewModel::retry,
                            contentPadding = padding,
                        )
                        AppDestination.SETTINGS -> SettingsScreen(
                            settings = settings,
                            save = viewModel::saveSettings,
                            clearAuthorizationCode = viewModel::clearAuthorizationCode,
                            onAutoStartChange = viewModel::setAutoStart,
                            onResidentChange = viewModel::setBackgroundResident,
                            hasSmsRole = hasSmsRole,
                            permissions = permissions,
                            requestSmsRole = requestSmsRole,
                            requestSmsPermissions = requestPermissions,
                            restartOnboarding = viewModel::restartOnboarding,
                            contentPadding = padding,
                        )
                    }
                }
            }
            OnboardingTour(
                state = onboarding,
                hasSmsRole = hasSmsRole,
                permissions = permissions,
                previous = viewModel::previousOnboarding,
                next = { viewModel.nextOnboarding() },
                skip = viewModel::skipOnboarding,
                requestSmsRole = requestOnboardingSmsRole,
                requestSmsPermissions = requestOnboardingSmsPermissions,
                requestNotificationPermission = requestOnboardingNotificationPermission,
                hazeState = hazeState,
            )
        }
    }
}
