package com.raku.smsrelay.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.raku.smsrelay.data.ForwardMessageEntity

@Composable
fun RelayScreen(
    messages: List<ForwardMessageEntity>,
    retry: (String) -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    LazyColumn(
        contentPadding = PaddingValues(
            start = 20.dp,
            top = contentPadding.calculateTopPadding() + 24.dp,
            end = 20.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScreenHeader("DELIVERY", "转发记录", "邮件投递队列、结果与失败原因。") }
        if (messages.isEmpty()) {
            item { EmptyState("队列是空的", "新短信进入后会自动出现在这里。") }
        } else {
            items(messages, key = { it.id }) { RelayMessageCard(it, retry) }
        }
    }
}
