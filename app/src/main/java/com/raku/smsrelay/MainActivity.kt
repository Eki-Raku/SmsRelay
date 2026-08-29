package com.raku.smsrelay

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.raku.smsrelay.onboarding.MessagingPermissionState
import com.raku.smsrelay.onboarding.OnboardingStep
import com.raku.smsrelay.onboarding.isSatisfiedBy
import com.raku.smsrelay.ui.SmsRelayApp
import com.raku.smsrelay.sms.IncomingSmsNotifier

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private var permissions by mutableStateOf(emptyPermissionState())
    private var hasSmsRole by mutableStateOf(false)
    private lateinit var smsPermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var roleLauncher: ActivityResultLauncher<android.content.Intent>
    private var pendingOnboardingStep: OnboardingStep? = null
    private var pendingSettingsRecoveryStep: OnboardingStep? = null
    private var smsPermissionDenied by mutableStateOf(false)
    private var notificationPermissionDenied by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        smsPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) {
            refreshSystemState()
            smsPermissionDenied = !permissions.hasAllMessagingPermissions
            finishPendingOnboardingStep(OnboardingStep.SMS_PERMISSIONS)
        }
        notificationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) {
            refreshSystemState()
            notificationPermissionDenied = !permissions.canPostNotifications
            finishPendingOnboardingStep(OnboardingStep.NOTIFICATIONS)
        }
        roleLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) {
            refreshSystemState()
            if (pendingOnboardingStep == OnboardingStep.DEFAULT_SMS) {
                finishPendingOnboardingStep(OnboardingStep.DEFAULT_SMS)
            } else if (hasSmsRole) {
                requestSmsPermissions()
            }
        }
        refreshSystemState()
        handleSmsIntent(intent)
        setContent {
            SmsRelayApp(
                viewModel = viewModel,
                permissions = permissions,
                hasSmsRole = hasSmsRole,
                requestPermissions = ::requestSmsPermissions,
                requestSmsRole = ::requestSmsRole,
                requestOnboardingSmsRole = ::requestOnboardingSmsRole,
                requestOnboardingSmsPermissions = ::requestOnboardingSmsPermissions,
                requestOnboardingNotificationPermission = ::requestOnboardingNotificationPermission,
                smsPermissionDenied = smsPermissionDenied,
                notificationPermissionDenied = notificationPermissionDenied,
                openOnboardingSystemSettings = ::openOnboardingSystemSettings,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        refreshSystemState()
        pendingSettingsRecoveryStep?.let { step ->
            pendingSettingsRecoveryStep = null
            if (step.isSatisfiedBy(hasSmsRole, permissions)) {
                if (step == OnboardingStep.SMS_PERMISSIONS) smsPermissionDenied = false
                if (step == OnboardingStep.NOTIFICATIONS) notificationPermissionDenied = false
                viewModel.nextOnboarding(step)
            }
        }
        if (hasSmsRole && permissions.canReadSms) viewModel.refreshSms()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleSmsIntent(intent)
    }

    private fun handleSmsIntent(intent: Intent?) {
        if (intent?.action == IncomingSmsNotifier.ACTION_OPEN_CONVERSATION) {
            intent.getLongExtra(IncomingSmsNotifier.EXTRA_THREAD_ID, -1L)
                .takeIf { it >= 0L }
                ?.let(viewModel::openConversation)
            return
        }
        if (intent?.action != Intent.ACTION_SENDTO) return
        val scheme = intent.data?.scheme.orEmpty()
        if (scheme !in setOf("sms", "smsto", "mms", "mmsto")) return
        val destination = intent.data?.schemeSpecificPart
            ?.substringBefore('?')
            ?.substringBefore(';')
            .orEmpty()
        viewModel.prepareSms(destination)
    }

    private fun refreshSystemState() {
        permissions = MessagingPermissionState(
            canReceiveSms = isGranted(Manifest.permission.RECEIVE_SMS),
            canReadSms = isGranted(Manifest.permission.READ_SMS),
            canSendSms = isGranted(Manifest.permission.SEND_SMS),
            canReceiveMms = isGranted(Manifest.permission.RECEIVE_MMS),
            canReceiveWapPush = isGranted(Manifest.permission.RECEIVE_WAP_PUSH),
            canPostNotifications = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                isGranted(Manifest.permission.POST_NOTIFICATIONS),
        )
        hasSmsRole = (application as SmsRelayApplication).container.smsRoleManager.isHeld()
    }

    private fun isGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private fun requestSmsRole() {
        val roleManager = (application as SmsRelayApplication).container.smsRoleManager
        if (roleManager.isHeld()) {
            refreshSystemState()
            requestSmsPermissions()
        } else if (roleManager.isAvailable()) {
            roleLauncher.launch(roleManager.createRequestIntent())
        }
    }

    private fun requestSmsPermissions() {
        smsPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.RECEIVE_MMS,
                Manifest.permission.RECEIVE_WAP_PUSH,
            ),
        )
    }

    private fun requestOnboardingSmsRole() {
        if (hasSmsRole) {
            viewModel.nextOnboarding(OnboardingStep.DEFAULT_SMS)
            return
        }
        pendingOnboardingStep = OnboardingStep.DEFAULT_SMS
        val roleManager = (application as SmsRelayApplication).container.smsRoleManager
        if (roleManager.isAvailable()) {
            roleLauncher.launch(roleManager.createRequestIntent())
        } else {
            finishPendingOnboardingStep(OnboardingStep.DEFAULT_SMS)
        }
    }

    private fun requestOnboardingSmsPermissions() {
        if (permissions.hasAllMessagingPermissions) {
            smsPermissionDenied = false
            viewModel.nextOnboarding(OnboardingStep.SMS_PERMISSIONS)
            return
        }
        pendingOnboardingStep = OnboardingStep.SMS_PERMISSIONS
        smsPermissionDenied = false
        requestSmsPermissions()
    }

    private fun requestOnboardingNotificationPermission() {
        if (permissions.canPostNotifications || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionDenied = false
            viewModel.nextOnboarding(OnboardingStep.NOTIFICATIONS)
            return
        }
        pendingOnboardingStep = OnboardingStep.NOTIFICATIONS
        notificationPermissionDenied = false
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun openOnboardingSystemSettings() {
        pendingSettingsRecoveryStep = viewModel.onboarding.value.step
        startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:$packageName"),
            ),
        )
    }

    private fun finishPendingOnboardingStep(step: OnboardingStep) {
        if (pendingOnboardingStep != step) return
        pendingOnboardingStep = null
        if (step.isSatisfiedBy(hasSmsRole, permissions)) viewModel.nextOnboarding(step)
    }

    private companion object {
        fun emptyPermissionState() = MessagingPermissionState(
            canReceiveSms = false,
            canReadSms = false,
            canSendSms = false,
            canReceiveMms = false,
            canReceiveWapPush = false,
            canPostNotifications = false,
        )
    }
}
