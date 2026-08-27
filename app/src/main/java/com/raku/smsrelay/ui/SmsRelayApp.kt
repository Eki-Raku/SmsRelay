package com.raku.smsrelay.ui

import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.raku.smsrelay.MainViewModel
import com.raku.smsrelay.onboarding.MessagingPermissionState
import com.raku.smsrelay.onboarding.OnboardingStep
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier

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
        Box(Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    SmsRelayNavigationBar(
                        selected = destination,
                        hasUnreadMessages = conversations.any { it.unread },
                        onSelect = { destination = it },
                    )
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
            ) { padding ->
                when (destination) {
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
            )
        }
    }
}
