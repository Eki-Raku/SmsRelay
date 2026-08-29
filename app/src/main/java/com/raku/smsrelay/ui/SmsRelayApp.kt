@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)

package com.raku.smsrelay.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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
    smsPermissionDenied: Boolean = false,
    notificationPermissionDenied: Boolean = false,
    openOnboardingSystemSettings: () -> Unit = {},
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val relayMessages by viewModel.messages.collectAsStateWithLifecycle()
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val threadMessages by viewModel.threadMessages.collectAsStateWithLifecycle()
    val selectedThreadId by viewModel.selectedThreadId.collectAsStateWithLifecycle()
    val composeRecipient by viewModel.composeRecipient.collectAsStateWithLifecycle()
    val onboarding by viewModel.onboarding.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val navController = rememberNavController()
    val navEntry by navController.currentBackStackEntryAsState()
    val route = navEntry?.destination?.route ?: AppRoute.Status.route
    val activeConversationThreadId = navEntry?.arguments?.getLong("threadId")
    var composerVisible by rememberSaveable { mutableStateOf(false) }
    val selectedDestination = when (route) {
        AppRoute.Messages.route, AppRoute.Conversation.route -> AppDestination.MESSAGES
        AppRoute.Relay.route -> AppDestination.RELAY
        AppRoute.Settings.route, AppRoute.MailSettings.route -> AppDestination.SETTINGS
        else -> AppDestination.STATUS
    }
    val deepRoute = route == AppRoute.Conversation.route || route == AppRoute.MailSettings.route
    val showNavigation = !deepRoute && !composerVisible && !onboarding.visible

    LaunchedEffect(viewModel) {
        viewModel.events.collect { snackbarHostState.showSnackbar(it) }
    }
    LaunchedEffect(route, hasSmsRole, permissions.canReadSms) {
        if (
            (route == AppRoute.Messages.route || route == AppRoute.Conversation.route) &&
            hasSmsRole &&
            permissions.canReadSms
        ) {
            viewModel.refreshSms()
        }
    }
    LaunchedEffect(composeRecipient) {
        if (composeRecipient.isNotBlank() && route != AppRoute.Messages.route) {
            navController.navigate(AppRoute.Messages.route) { launchSingleTop = true }
        }
    }
    LaunchedEffect(selectedThreadId, onboarding.visible) {
        val threadId = selectedThreadId ?: return@LaunchedEffect
        if (shouldNavigateToConversation(route, activeConversationThreadId, threadId, onboarding.visible)) {
            navController.navigate(AppRoute.Conversation.create(threadId)) {
                if (route == AppRoute.Conversation.route) {
                    popUpTo(AppRoute.Conversation.route) { inclusive = true }
                }
                launchSingleTop = true
            }
        }
    }
    LaunchedEffect(route) {
        if (route != AppRoute.Conversation.route && selectedThreadId != null) {
            viewModel.closeConversation()
        }
    }
    LaunchedEffect(onboarding.visible, onboarding.step) {
        if (onboarding.visible) {
            val target = if (onboarding.step == OnboardingStep.SETTINGS) {
                AppRoute.Settings.route
            } else {
                AppRoute.Status.route
            }
            if (route != target) {
                navController.navigate(target) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }

    SmsRelayTheme {
        val motion = RelayTheme.motion
        val hazeState = rememberHazeState()
        val tourTargetRegistry = rememberTourTargetRegistry()
        val windowWidth = with(LocalDensity.current) {
            LocalWindowInfo.current.containerSize.width.toDp()
        }
        val useNavigationRail = windowWidth >= 600.dp
        CompositionLocalProvider(LocalTourTargetRegistry provides tourTargetRegistry) {
            Box(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxSize()) {
                    AnimatedVisibility(
                        visible = useNavigationRail && showNavigation,
                        enter = slideInHorizontally(
                            tween(motion.duration(motion.stateMillis)),
                            initialOffsetX = { if (motion.reducedMotion) 0 else -it },
                        ) + fadeIn(tween(motion.duration(160))),
                        exit = slideOutHorizontally(
                            tween(motion.duration(motion.stateMillis)),
                            targetOffsetX = { if (motion.reducedMotion) 0 else -it },
                        ) + fadeOut(tween(motion.duration(140))),
                    ) {
                        SmsRelayNavigationRail(
                            selected = selectedDestination,
                            hasUnreadMessages = conversations.any { it.unread },
                            onSelect = { destination ->
                                navController.navigate(destination.route.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }
            RelayScaffold(
                modifier = Modifier.weight(1f),
                includeBottomInset = useNavigationRail && showNavigation,
                bottomBar = {
                    AnimatedVisibility(
                        visible = showNavigation && !useNavigationRail,
                        enter = slideInVertically(
                            animationSpec = tween(motion.duration(motion.stateMillis)),
                            initialOffsetY = { if (motion.reducedMotion) 0 else it },
                        ) + fadeIn(tween(motion.duration(160))),
                        exit = slideOutVertically(
                            animationSpec = tween(motion.duration(motion.stateMillis)),
                            targetOffsetY = { if (motion.reducedMotion) 0 else it },
                        ) + fadeOut(tween(motion.duration(140))),
                    ) {
                        RelayTabBar(
                            selected = selectedDestination,
                            hasUnreadMessages = conversations.any { it.unread },
                            onSelect = { destination ->
                                navController.navigate(destination.route.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
            ) { padding ->
                SharedTransitionLayout(Modifier.fillMaxSize()) {
                    val sharedTransitionScope = this
                    NavHost(
                        navController = navController,
                        startDestination = AppRoute.Status.route,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (onboarding.visible) Modifier.hazeSource(hazeState, zIndex = 0f)
                                else Modifier,
                            ),
                    ) {
                    composable(
                        route = AppRoute.Status.route,
                        enterTransition = { fadeIn(tween(motion.duration(160))) },
                        exitTransition = { fadeOut(tween(motion.duration(160))) },
                    ) {
                        StatusScreen(
                            settings = settings,
                            relayMessages = relayMessages,
                            hasSmsPermission = permissions.hasCoreSmsPermissions,
                            hasSmsRole = hasSmsRole,
                            requestPermissions = requestPermissions,
                            requestSmsRole = requestSmsRole,
                            sendTest = viewModel::sendTest,
                            contentPadding = padding,
                        )
                    }
                    composable(
                        route = AppRoute.Messages.route,
                        enterTransition = { fadeIn(tween(motion.duration(160))) },
                        exitTransition = { fadeOut(tween(motion.duration(160))) },
                    ) {
                        val animatedVisibilityScope = this
                        MessagesScreen(
                            conversations = conversations,
                            messages = emptyList(),
                            selectedThreadId = null,
                            composeRecipient = composeRecipient,
                            hasSmsRole = hasSmsRole,
                            permissions = permissions,
                            requestSmsRole = requestSmsRole,
                            requestSmsPermissions = requestPermissions,
                            openConversation = { threadId ->
                                viewModel.openConversation(threadId)
                            },
                            closeConversation = {},
                            sendSms = viewModel::sendSms,
                            onComposerVisibilityChanged = { composerVisible = it },
                            contentPadding = padding,
                            sharedTransitionScope = if (motion.reducedMotion) null else sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                        )
                    }
                    composable(
                        route = AppRoute.Conversation.route,
                        arguments = listOf(navArgument("threadId") { type = NavType.LongType }),
                        enterTransition = {
                            slideInHorizontally(
                                tween(motion.duration(motion.pageMillis), easing = motion.standardEasing),
                                initialOffsetX = { if (motion.reducedMotion) 0 else it * 18 / 100 },
                            ) + fadeIn(tween(motion.duration(180)))
                        },
                        exitTransition = { fadeOut(tween(motion.duration(140))) },
                        popEnterTransition = { fadeIn(tween(motion.duration(160))) },
                        popExitTransition = {
                            slideOutHorizontally(
                                tween(motion.duration(motion.pageMillis), easing = motion.standardEasing),
                                targetOffsetX = { if (motion.reducedMotion) 0 else it * 18 / 100 },
                            ) + fadeOut(tween(motion.duration(180)))
                        },
                    ) { backStackEntry ->
                        val animatedVisibilityScope = this
                        val routeThreadId = backStackEntry.arguments?.getLong("threadId") ?: return@composable
                        LaunchedEffect(routeThreadId) {
                            if (selectedThreadId != routeThreadId) viewModel.openConversation(routeThreadId)
                        }
                        MessagesScreen(
                            conversations = emptyList(),
                            messages = threadMessages,
                            selectedThreadId = routeThreadId,
                            composeRecipient = composeRecipient,
                            hasSmsRole = hasSmsRole,
                            permissions = permissions,
                            requestSmsRole = requestSmsRole,
                            requestSmsPermissions = requestPermissions,
                            openConversation = {},
                            closeConversation = { navController.popBackStack() },
                            sendSms = viewModel::sendSms,
                            contentPadding = padding,
                            sharedTransitionScope = if (motion.reducedMotion) null else sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                        )
                    }
                    composable(
                        route = AppRoute.Relay.route,
                        enterTransition = { fadeIn(tween(motion.duration(160))) },
                        exitTransition = { fadeOut(tween(motion.duration(160))) },
                    ) {
                        RelayScreen(messages = relayMessages, retry = viewModel::retry, contentPadding = padding)
                    }
                    composable(
                        route = AppRoute.Settings.route,
                        enterTransition = { fadeIn(tween(motion.duration(160))) },
                        exitTransition = { fadeOut(tween(motion.duration(160))) },
                    ) {
                        SettingsScreen(
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
                            openMailSettings = { navController.navigate(AppRoute.MailSettings.route) },
                            onForwardingChange = viewModel::setForwardingEnabled,
                            contentPadding = padding,
                        )
                    }
                    composable(
                        route = AppRoute.MailSettings.route,
                        enterTransition = {
                            slideInHorizontally(
                                tween(motion.duration(motion.pageMillis), easing = motion.standardEasing),
                                initialOffsetX = { if (motion.reducedMotion) 0 else it * 18 / 100 },
                            ) + fadeIn(tween(motion.duration(180)))
                        },
                        popExitTransition = {
                            slideOutHorizontally(
                                tween(motion.duration(motion.pageMillis), easing = motion.standardEasing),
                                targetOffsetX = { if (motion.reducedMotion) 0 else it * 18 / 100 },
                            ) + fadeOut(tween(motion.duration(180)))
                        },
                    ) {
                        MailSettingsScreen(
                            settings = settings,
                            save = viewModel::saveSettings,
                            clearAuthorizationCode = viewModel::clearAuthorizationCode,
                            onBack = { navController.popBackStack() },
                            contentPadding = padding,
                        )
                    }
                }
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
                smsPermissionDenied = smsPermissionDenied,
                notificationPermissionDenied = notificationPermissionDenied,
                openSystemSettings = openOnboardingSystemSettings,
                hazeState = hazeState,
                )
            }
        }
    }
}
