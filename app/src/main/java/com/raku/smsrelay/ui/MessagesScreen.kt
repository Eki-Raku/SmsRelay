@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)

package com.raku.smsrelay.ui

import android.provider.Telephony
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.raku.smsrelay.onboarding.MessagingPermissionState
import com.raku.smsrelay.onboarding.SmsInboxBlockReason
import com.raku.smsrelay.onboarding.smsInboxBlockReason
import com.raku.smsrelay.sms.SmsConversation
import com.raku.smsrelay.sms.SmsPresentationFactory
import com.raku.smsrelay.sms.SystemSmsMessage

@Composable
fun MessagesScreen(
    conversations: List<SmsConversation>,
    messages: List<SystemSmsMessage>,
    selectedThreadId: Long?,
    composeRecipient: String,
    hasSmsRole: Boolean,
    permissions: MessagingPermissionState,
    requestSmsRole: () -> Unit,
    requestSmsPermissions: () -> Unit,
    openConversation: (Long) -> Unit,
    closeConversation: () -> Unit,
    sendSms: (String, String) -> Unit,
    onComposerVisibilityChanged: (Boolean) -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    DisposableEffect(Unit) {
        onDispose { onComposerVisibilityChanged(false) }
    }
    val blockReason = smsInboxBlockReason(hasSmsRole, permissions)
    if (blockReason != null) {
        PermissionRequired(blockReason, requestSmsRole, requestSmsPermissions, contentPadding)
        return
    }

    if (selectedThreadId == null) {
        ConversationList(
            conversations,
            composeRecipient,
            openConversation,
            sendSms,
            onComposerVisibilityChanged,
            contentPadding,
            sharedTransitionScope,
            animatedVisibilityScope,
        )
    } else {
        ConversationDetail(
            messages,
            composeRecipient,
            closeConversation,
            sendSms,
            contentPadding,
            selectedThreadId,
            sharedTransitionScope,
            animatedVisibilityScope,
        )
    }
}

@Composable
private fun PermissionRequired(
    blockReason: SmsInboxBlockReason,
    requestSmsRole: () -> Unit,
    requestSmsPermissions: () -> Unit,
    contentPadding: PaddingValues,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(contentPadding).padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        MessagesTitle()
        when (blockReason) {
            SmsInboxBlockReason.DEFAULT_ROLE -> EmptyState(
                "需要默认短信角色",
                "设为默认短信应用后，才能安全保存、展示和发送系统短信。",
            )
            SmsInboxBlockReason.READ_PERMISSION -> EmptyState(
                "需要短信读取权限",
                "当前已经是默认短信应用，还需要允许读取短信才能展示会话。",
            )
        }
        Button(
            onClick = if (blockReason == SmsInboxBlockReason.DEFAULT_ROLE) requestSmsRole else requestSmsPermissions,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (blockReason == SmsInboxBlockReason.DEFAULT_ROLE) "设为默认短信应用" else "授权短信权限")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationList(
    conversations: List<SmsConversation>,
    initialRecipient: String,
    openConversation: (Long) -> Unit,
    sendSms: (String, String) -> Unit,
    onComposerVisibilityChanged: (Boolean) -> Unit,
    contentPadding: PaddingValues,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var showComposer by rememberSaveable { mutableStateOf(initialRecipient.isNotBlank()) }
    val filtered = remember(conversations, query) {
        val keyword = query.trim()
        if (keyword.isEmpty()) conversations else conversations.filter { conversation ->
            val presentation = SmsPresentationFactory.from(conversation.address, conversation.snippet)
            presentation.displaySender.contains(keyword, ignoreCase = true) ||
                conversation.address.contains(keyword, ignoreCase = true) ||
                conversation.snippet.contains(keyword, ignoreCase = true)
        }
    }

    LaunchedEffect(initialRecipient) {
        if (initialRecipient.isNotBlank()) showComposer = true
    }
    LaunchedEffect(showComposer) {
        onComposerVisibilityChanged(showComposer)
    }
    val listState = rememberLazyListState()
    val collapsedTitle by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 52
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().padding(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding(),
        ),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = RelaySpacing.lg,
                            top = RelaySpacing.lg,
                            end = RelaySpacing.md,
                            bottom = RelaySpacing.sm,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MessagesTitle(Modifier.weight(1f))
                    IconButton(onClick = { showComposer = true }) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = "新建短信",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            item { SearchField(query = query, onQueryChange = { query = it }) }
            if (filtered.isEmpty()) {
                item {
                    Box(
                        Modifier.fillMaxWidth().height(360.dp).padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (query.isBlank()) {
                                "还没有短信\n收到或发送第一条短信后，会话会出现在这里。"
                            } else {
                                "没有匹配的短信"
                            },
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            } else {
                items(filtered, key = { it.threadId }) { conversation ->
                    ConversationRow(
                        conversation,
                        onClick = { openConversation(conversation.threadId) },
                        modifier = Modifier.animateItem(),
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 84.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
        }
        AnimatedVisibility(
            visible = collapsedTitle,
            enter = fadeIn(tween(RelayTheme.motion.duration(140))),
            exit = fadeOut(tween(RelayTheme.motion.duration(120))),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                shadowElevation = 4.dp,
            ) {
                Row(
                    Modifier.fillMaxWidth().height(52.dp).padding(start = 20.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "信息",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    IconButton(onClick = { showComposer = true }) {
                        Icon(Icons.Outlined.Edit, contentDescription = "新建短信")
                    }
                }
            }
        }
    }

    if (showComposer) {
        ComposeMessageSheet(
            initialRecipient = initialRecipient,
            dismiss = { showComposer = false },
            sendSms = { recipient, body ->
                sendSms(recipient, body)
                showComposer = false
            },
        )
    }
}

@Composable
private fun MessagesTitle(modifier: Modifier = Modifier) {
    Text(
        "信息",
        modifier = modifier,
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().padding(horizontal = RelaySpacing.md, vertical = RelaySpacing.xs),
        placeholder = { Text("搜索") },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}

@Composable
private fun ConversationRow(
    conversation: SmsConversation,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    val presentation = SmsPresentationFactory.from(conversation.address, conversation.snippet)
    val avatarModifier = Modifier.sharedSenderElement(
        key = "sender-avatar-${conversation.threadId}",
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
    )
    val titleModifier = Modifier.sharedSenderElement(
        key = "sender-title-${conversation.threadId}",
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .relayClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SenderAvatar(presentation.displaySender, modifier = avatarModifier)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (conversation.unread) {
                    Box(Modifier.size(9.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                    Spacer(Modifier.width(RelaySpacing.xs))
                }
                Text(
                    presentation.displaySender,
                    modifier = titleModifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (conversation.unread) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    formatTime(conversation.dateEpochMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                presentation.notificationPreview,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun SenderAvatar(sender: String, modifier: Modifier = Modifier, size: Int = 52) {
    Box(
        modifier = modifier.size(size.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            sender.trim().take(1).ifBlank { "?" },
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComposeMessageSheet(
    initialRecipient: String,
    dismiss: () -> Unit,
    sendSms: (String, String) -> Unit,
) {
    var recipient by rememberSaveable(initialRecipient) { mutableStateOf(initialRecipient) }
    var body by rememberSaveable { mutableStateOf("") }
    RelaySheet(
        onDismissRequest = dismiss,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().imePadding().padding(horizontal = 16.dp).padding(bottom = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "新信息",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "取消",
                    modifier = Modifier.relayClickable(onClick = dismiss).padding(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            HorizontalDivider(Modifier.padding(top = RelaySpacing.sm))
            TextField(
                value = recipient,
                onValueChange = { recipient = it },
                modifier = Modifier.fillMaxWidth(),
                prefix = { Text("收件人：", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                singleLine = true,
                colors = transparentTextFieldColors(),
            )
            HorizontalDivider()
            Spacer(Modifier.height(20.dp))
            MessageComposer(
                body = body,
                onBodyChange = { body = it },
                enabled = recipient.isNotBlank() && body.isNotBlank(),
                send = { sendSms(recipient, body) },
            )
        }
    }
}

@Composable
private fun ConversationDetail(
    messages: List<SystemSmsMessage>,
    fallbackRecipient: String,
    closeConversation: () -> Unit,
    sendSms: (String, String) -> Unit,
    contentPadding: PaddingValues,
    threadId: Long,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
) {
    val recipient = messages.firstOrNull()?.address.orEmpty().ifBlank { fallbackRecipient }
    val displaySender = messages.lastOrNull { it.type == Telephony.Sms.MESSAGE_TYPE_INBOX }?.let {
        SmsPresentationFactory.from(it.address, it.body).displaySender
    } ?: recipient.ifBlank { "短信会话" }
    var body by rememberSaveable(threadId) { mutableStateOf("") }
    val avatarModifier = Modifier.sharedSenderElement(
        key = "sender-avatar-$threadId",
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
    )
    val titleModifier = Modifier.sharedSenderElement(
        key = "sender-title-$threadId",
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
    )
    Column(
        modifier = Modifier.fillMaxSize().padding(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding(),
        ).navigationBarsPadding().imePadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = closeConversation) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "返回",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                SenderAvatar(displaySender, modifier = avatarModifier, size = 38)
                Text(
                    displaySender,
                    modifier = titleModifier,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.size(48.dp))
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            items(messages, key = { it.id }) { MessageBubble(it, Modifier.animateItem()) }
        }
        MessageComposer(
            body = body,
            onBodyChange = { body = it },
            enabled = recipient.isNotBlank() && body.isNotBlank(),
            send = {
                sendSms(recipient, body)
                body = ""
            },
            modifier = Modifier.padding(horizontal = RelaySpacing.sm, vertical = RelaySpacing.xs),
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun Modifier.sharedSenderElement(
    key: String,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
): Modifier {
    val scope = sharedTransitionScope ?: return this
    val visibilityScope = animatedVisibilityScope ?: return this
    return with(scope) {
        this@sharedSenderElement.sharedElement(
            sharedContentState = rememberSharedContentState(key),
            animatedVisibilityScope = visibilityScope,
        )
    }
}

@Composable
private fun MessageComposer(
    body: String,
    onBodyChange: (String) -> Unit,
    enabled: Boolean,
    send: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(start = RelaySpacing.xs, end = RelaySpacing.xxs),
            verticalAlignment = Alignment.Bottom,
        ) {
            TextField(
                value = body,
                onValueChange = onBodyChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("短信") },
                maxLines = 5,
                colors = transparentTextFieldColors(),
            )
            IconButton(onClick = send, enabled = enabled) {
                Box(
                    modifier = Modifier.size(32.dp).background(
                        if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        CircleShape,
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.Send,
                        contentDescription = "发送",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: SystemSmsMessage, modifier: Modifier = Modifier) {
    val outgoing = message.type in setOf(
        Telephony.Sms.MESSAGE_TYPE_SENT,
        Telephony.Sms.MESSAGE_TYPE_OUTBOX,
        Telephony.Sms.MESSAGE_TYPE_FAILED,
        Telephony.Sms.MESSAGE_TYPE_QUEUED,
    )
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (outgoing) Alignment.End else Alignment.Start,
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 310.dp).animateContentSize(
                if (RelayTheme.motion.reducedMotion) snap() else {
                    spring(dampingRatio = 0.86f, stiffness = 480f)
                },
            ),
            color = if (outgoing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (outgoing) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (outgoing) 18.dp else 5.dp,
                bottomEnd = if (outgoing) 5.dp else 18.dp,
            ),
        ) {
            Text(message.body, modifier = Modifier.padding(horizontal = RelaySpacing.sm, vertical = RelaySpacing.xs))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (message.type == Telephony.Sms.MESSAGE_TYPE_FAILED) {
                Text("未送达 · ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
            Text(
                formatTime(message.dateEpochMs),
                modifier = Modifier.padding(horizontal = RelaySpacing.xxs, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun transparentTextFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
)
