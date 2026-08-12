package com.raku.smsrelay.ui

import android.provider.Telephony
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.raku.smsrelay.sms.SmsConversation
import com.raku.smsrelay.sms.SystemSmsMessage
import com.raku.smsrelay.onboarding.MessagingPermissionState
import com.raku.smsrelay.onboarding.SmsInboxBlockReason
import com.raku.smsrelay.onboarding.smsInboxBlockReason

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
    contentPadding: PaddingValues = PaddingValues(),
) {
    val blockReason = smsInboxBlockReason(hasSmsRole, permissions)
    if (blockReason != null) {
        LazyColumn(
            contentPadding = PaddingValues(
                start = 20.dp,
                top = contentPadding.calculateTopPadding() + 24.dp,
                end = 20.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item { ScreenHeader("INBOX", "短信", "系统短信收件箱与会话。") }
            item {
                when (blockReason) {
                    SmsInboxBlockReason.DEFAULT_ROLE -> EmptyState(
                        "需要默认短信角色",
                        "启用后才能安全读取、保存和发送系统短信。",
                    )
                    SmsInboxBlockReason.READ_PERMISSION -> EmptyState(
                        "需要短信读取权限",
                        "当前已经是默认短信应用，但系统尚未允许读取短信。",
                    )
                }
            }
            item {
                Button(
                    onClick = if (blockReason == SmsInboxBlockReason.DEFAULT_ROLE) requestSmsRole else requestSmsPermissions,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (blockReason == SmsInboxBlockReason.DEFAULT_ROLE) "设为默认短信应用" else "授权短信权限")
                }
            }
        }
        return
    }

    if (selectedThreadId == null) {
        ConversationList(
            conversations = conversations,
            initialRecipient = composeRecipient,
            openConversation = openConversation,
            sendSms = sendSms,
            contentPadding = contentPadding,
        )
    } else {
        ConversationDetail(
            messages = messages,
            fallbackRecipient = composeRecipient,
            closeConversation = closeConversation,
            sendSms = sendSms,
            contentPadding = contentPadding,
        )
    }
}

@Composable
private fun ConversationList(
    conversations: List<SmsConversation>,
    initialRecipient: String,
    openConversation: (Long) -> Unit,
    sendSms: (String, String) -> Unit,
    contentPadding: PaddingValues,
) {
    var recipient by remember(initialRecipient) { mutableStateOf(initialRecipient) }
    var body by remember { mutableStateOf("") }
    LazyColumn(
        contentPadding = PaddingValues(
            start = 20.dp,
            top = contentPadding.calculateTopPadding() + 24.dp,
            end = 20.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScreenHeader("INBOX", "短信", "安静地收取，也可以从这里回复。") }
        item {
            HairlineCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("新短信", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text("COMPOSE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    OutlinedTextField(
                        value = recipient,
                        onValueChange = { recipient = it },
                        label = { Text("收件号码") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = body,
                        onValueChange = { body = it },
                        label = { Text("短信内容") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = {
                            sendSms(recipient, body)
                            body = ""
                        },
                        enabled = recipient.isNotBlank() && body.isNotBlank(),
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RelayPalette.Indigo,
                            contentColor = androidx.compose.ui.graphics.Color.White,
                        ),
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = null)
                        Text(" 发送")
                    }
                }
            }
        }
        if (conversations.isEmpty()) {
            item { EmptyState("还没有短信", "收到或发送第一条短信后，会话会出现在这里。") }
        } else {
            items(conversations, key = { it.threadId }) { conversation ->
                ConversationRow(conversation, onClick = { openConversation(conversation.threadId) })
            }
        }
    }
}

@Composable
private fun ConversationRow(conversation: SmsConversation, onClick: () -> Unit) {
    HairlineCard(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        conversation.address.ifBlank { "未知号码" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (conversation.unread) FontWeight.SemiBold else FontWeight.Medium,
                    )
                    if (conversation.unread) {
                        Spacer(Modifier.size(8.dp))
                        Box(Modifier.size(7.dp).background(RelayPalette.Violet, RoundedCornerShape(50)))
                    }
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    conversation.snippet,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                formatTime(conversation.dateEpochMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
) {
    val recipient = messages.firstOrNull()?.address.orEmpty().ifBlank { fallbackRecipient }
    var body by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = contentPadding.calculateTopPadding(), bottom = contentPadding.calculateBottomPadding()),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = closeConversation) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回会话")
            }
            Column {
                Text(recipient.ifBlank { "短信会话" }, fontWeight = FontWeight.SemiBold)
                Text("系统短信", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(messages, key = { it.id }) { MessageBubble(it) }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("短信内容") },
                maxLines = 4,
            )
            IconButton(
                onClick = {
                    sendSms(recipient, body)
                    body = ""
                },
                enabled = recipient.isNotBlank() && body.isNotBlank(),
            ) { Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "发送") }
        }
    }
}

@Composable
private fun MessageBubble(message: SystemSmsMessage) {
    val outgoing = message.type in setOf(
        Telephony.Sms.MESSAGE_TYPE_SENT,
        Telephony.Sms.MESSAGE_TYPE_OUTBOX,
        Telephony.Sms.MESSAGE_TYPE_FAILED,
        Telephony.Sms.MESSAGE_TYPE_QUEUED,
    )
    Box(Modifier.fillMaxWidth(), contentAlignment = if (outgoing) Alignment.CenterEnd else Alignment.CenterStart) {
        Column(
            modifier = Modifier
                .widthIn(max = 296.dp)
                .background(
                    color = if (outgoing) RelayPalette.VioletSoft else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(
                        topStart = 14.dp,
                        topEnd = 14.dp,
                        bottomStart = if (outgoing) 14.dp else 4.dp,
                        bottomEnd = if (outgoing) 4.dp else 14.dp,
                    ),
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(message.body)
            Spacer(Modifier.height(4.dp))
            Text(
                formatTime(message.dateEpochMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
