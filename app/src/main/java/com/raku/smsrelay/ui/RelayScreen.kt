package com.raku.smsrelay.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.raku.smsrelay.data.ForwardMessageEntity
import com.raku.smsrelay.data.ForwardStatus

private enum class RelayFilter(val label: String) {
    ALL("全部"), QUEUED("队列"), SENT("已发送"), FAILED("失败")
}

@Composable
fun RelayScreen(
    messages: List<ForwardMessageEntity>,
    retry: (String) -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    var filter by remember { mutableStateOf(RelayFilter.ALL) }
    val visible = remember(messages, filter) {
        messages.filter { message ->
            when (filter) {
                RelayFilter.ALL -> true
                RelayFilter.QUEUED -> message.status in setOf(
                    ForwardStatus.PENDING,
                    ForwardStatus.SENDING,
                    ForwardStatus.RETRY,
                )
                RelayFilter.SENT -> message.status == ForwardStatus.SENT
                RelayFilter.FAILED -> message.status == ForwardStatus.FAILED
            }
        }
    }

    LazyColumn(
        modifier = Modifier.padding(top = contentPadding.calculateTopPadding()),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 22.dp,
            end = 20.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScreenHeader("DELIVERY", "转发记录", "每一次邮件投递的实时状态与失败原因。") }
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Row(Modifier.fillMaxWidth().padding(4.dp)) {
                    RelayFilter.entries.forEach { item ->
                        RelayFilterItem(
                            filter = item,
                            selected = item == filter,
                            count = messages.count { message ->
                                when (item) {
                                    RelayFilter.ALL -> true
                                    RelayFilter.QUEUED -> message.status in setOf(
                                        ForwardStatus.PENDING,
                                        ForwardStatus.SENDING,
                                        ForwardStatus.RETRY,
                                    )
                                    RelayFilter.SENT -> message.status == ForwardStatus.SENT
                                    RelayFilter.FAILED -> message.status == ForwardStatus.FAILED
                                }
                            },
                            onClick = { filter = item },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        if (visible.isEmpty()) {
            item {
                EmptyState(
                    if (messages.isEmpty()) "队列是空的" else "这里还没有记录",
                    if (messages.isEmpty()) "新短信进入后会自动出现在这里。" else "切换到其他状态查看投递记录。",
                )
            }
        } else {
            items(visible, key = { it.id }) { message ->
                RelayMessageCard(message, retry)
            }
        }
    }
}

@Composable
private fun RelayFilterItem(
    filter: RelayFilter,
    selected: Boolean,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
        label = "relay filter background",
    )
    Column(
        modifier = modifier
            .background(background, CircleShape)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
    ) {
        Text(
            count.toString(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            filter.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
