package com.raku.smsrelay

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.raku.smsrelay.data.DedupeKey
import com.raku.smsrelay.data.ForwardMessageEntity
import com.raku.smsrelay.data.SmtpSettings
import com.raku.smsrelay.service.RelayForegroundService
import com.raku.smsrelay.mail.SmtpConfig
import com.raku.smsrelay.onboarding.OnboardingStep
import com.raku.smsrelay.onboarding.OnboardingUiState
import com.raku.smsrelay.onboarding.smsReadFailureMessage
import com.raku.smsrelay.sms.SmsConversation
import com.raku.smsrelay.sms.SmsSendRequestResult
import com.raku.smsrelay.sms.SystemSmsMessage
import java.util.UUID
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as SmsRelayApplication).container
    private val dao = container.database.forwardMessageDao()

    val messages = dao.observeRecent(limit = 200).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )
    val settings = container.settingsRepository.settings
    private val mutableConversations = MutableStateFlow<List<SmsConversation>>(emptyList())
    val conversations = mutableConversations.asStateFlow()
    private val mutableThreadMessages = MutableStateFlow<List<SystemSmsMessage>>(emptyList())
    val threadMessages = mutableThreadMessages.asStateFlow()
    private val mutableSelectedThreadId = MutableStateFlow<Long?>(null)
    val selectedThreadId = mutableSelectedThreadId.asStateFlow()
    private val mutableComposeRecipient = MutableStateFlow("")
    val composeRecipient = mutableComposeRecipient.asStateFlow()
    private val mutableOnboarding = MutableStateFlow(
        OnboardingUiState.initial(container.onboardingRepository.isCompleted()),
    )
    val onboarding = mutableOnboarding.asStateFlow()

    private val mutableEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val events = mutableEvents.asSharedFlow()

    fun saveSettings(
        enabled: Boolean,
        senderEmail: String,
        recipientEmail: String,
        authorizationCode: String,
    ) {
        val current = settings.value
        val normalizedSender = SmtpConfig.normalizeQqEmail(senderEmail)
        val normalizedRecipient = SmtpConfig.normalizeRecipientEmail(recipientEmail)
        if (normalizedSender == null || normalizedRecipient == null) {
            mutableEvents.tryEmit("请检查发件和收件邮箱格式")
            return
        }
        if (
            current.hasAuthorizationCode &&
            current.senderEmail.isNotBlank() &&
            normalizedSender != current.senderEmail &&
            authorizationCode.isBlank()
        ) {
            mutableEvents.tryEmit("更换发件账号时需要填写对应的新授权码")
            return
        }
        container.settingsRepository.save(
            enabled = enabled,
            senderEmail = senderEmail,
            recipientEmail = recipientEmail,
            newAuthorizationCode = authorizationCode,
        )
        mutableEvents.tryEmit("设置已保存")
    }

    fun refreshSms() {
        viewModelScope.launch {
            if (!container.smsRoleManager.isHeld()) {
                mutableConversations.value = emptyList()
                mutableThreadMessages.value = emptyList()
                return@launch
            }
            runCatching { container.systemSmsRepository.conversations() }
                .onSuccess { mutableConversations.value = it }
                .onFailure { mutableEvents.emit(smsReadFailureMessage(it)) }
            mutableSelectedThreadId.value?.let { threadId ->
                runCatching { container.systemSmsRepository.messages(threadId) }
                    .onSuccess { mutableThreadMessages.value = it }
            }
        }
    }

    fun openConversation(threadId: Long) {
        mutableSelectedThreadId.value = threadId
        viewModelScope.launch {
            val result = runCatching {
                container.systemSmsRepository.markThreadRead(threadId)
                container.incomingSmsNotifier.dismiss(threadId)
                container.systemSmsRepository.messages(threadId)
            }
            result.onSuccess { mutableThreadMessages.value = it }
                .onFailure { mutableEvents.emit("无法打开这条短信会话") }
            if (result.isSuccess) {
                runCatching { container.systemSmsRepository.conversations() }
                    .onSuccess { conversations -> mutableConversations.value = conversations }
            }
        }
    }

    fun closeConversation() {
        mutableSelectedThreadId.value = null
        mutableThreadMessages.value = emptyList()
    }

    fun prepareSms(destination: String) {
        mutableComposeRecipient.value = destination
    }

    fun nextOnboarding(expectedStep: OnboardingStep? = null) {
        val current = mutableOnboarding.value
        if (!current.visible || (expectedStep != null && current.step != expectedStep)) return
        val next = current.next()
        mutableOnboarding.value = next
        if (!next.visible) container.onboardingRepository.markCompleted()
    }

    fun previousOnboarding() {
        mutableOnboarding.value = mutableOnboarding.value.previous()
    }

    fun skipOnboarding() {
        container.onboardingRepository.markCompleted()
        mutableOnboarding.value = mutableOnboarding.value.dismiss()
    }

    fun restartOnboarding() {
        container.onboardingRepository.reset()
        mutableOnboarding.value = mutableOnboarding.value.restart()
    }

    fun sendSms(destination: String, body: String) {
        viewModelScope.launch {
            when (val result = runCatching {
                container.smsSendController.send(destination = destination, body = body)
            }.getOrElse { SmsSendRequestResult.Rejected("短信发送失败") }) {
                is SmsSendRequestResult.Queued -> {
                    mutableEvents.emit("短信已提交发送")
                    mutableComposeRecipient.value = destination
                    refreshSms()
                }
                is SmsSendRequestResult.Rejected -> mutableEvents.emit(result.reason)
            }
        }
    }

    fun clearAuthorizationCode() {
        container.settingsRepository.clearAuthorizationCode()
        mutableEvents.tryEmit("SMTP 授权码已清除")
    }

    fun setAutoStart(enabled: Boolean) {
        container.settingsRepository.setAutoStart(enabled)
        mutableEvents.tryEmit(if (enabled) "开机自启已开启" else "开机自启已关闭")
    }

    fun setBackgroundResident(enabled: Boolean) {
        container.settingsRepository.setBackgroundResident(enabled)
        if (enabled) {
            RelayForegroundService.start(getApplication())
        } else {
            RelayForegroundService.stop(getApplication())
        }
        mutableEvents.tryEmit(if (enabled) "后台常驻已开启" else "后台常驻已关闭")
    }

    fun retry(messageId: String) {
        viewModelScope.launch {
            dao.resetForRetry(messageId)
            container.scheduler.enqueue(messageId, replace = true)
            mutableEvents.emit("已重新加入发送队列")
        }
    }

    fun sendTest() {
        viewModelScope.launch {
            val currentSettings: SmtpSettings = settings.value
            if (
                !currentSettings.enabled ||
                currentSettings.senderEmail.isBlank() ||
                currentSettings.recipientEmail.isBlank() ||
                !currentSettings.hasAuthorizationCode
            ) {
                mutableEvents.emit("请先完成 QQ SMTP 配置并开启转发")
                return@launch
            }

            val now = System.currentTimeMillis()
            val message = ForwardMessageEntity(
                id = UUID.randomUUID().toString(),
                dedupeKey = DedupeKey.create("短信信使", now, "这是一条链路测试消息。", null),
                sender = "短信信使",
                body = "这是一条链路测试消息。如果你在 QQ 邮箱里看到它，说明 Android 到 QQ SMTP 的链路工作正常。",
                receivedAtEpochMs = now,
                subscriptionId = null,
                simLabel = "测试",
                isTest = true,
            )
            if (dao.insert(message) != -1L) {
                container.scheduler.enqueue(message.id, replace = true)
                mutableEvents.emit("测试消息已进入发送队列")
            }
        }
    }
}
