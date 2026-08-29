package com.raku.smsrelay.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
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
    val motion = RelayTheme.motion
    val fontScale = LocalDensity.current.fontScale
    val filterHeight = (62f + ((fontScale - 1f).coerceAtLeast(0f) * 28f)).dp
    var filter by rememberSaveable { mutableStateOf(RelayFilter.ALL) }
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
                BoxWithConstraints(Modifier.fillMaxWidth().height(filterHeight).padding(4.dp)) {
                    val itemWidth = maxWidth / RelayFilter.entries.size
                    val indicatorOffset by animateDpAsState(
                        targetValue = itemWidth * filter.ordinal,
                        animationSpec = if (RelayTheme.motion.reducedMotion) snap() else {
                            androidx.compose.animation.core.tween(
                                durationMillis = RelayTheme.motion.duration(RelayTheme.motion.iconMillis),
                                easing = RelayTheme.motion.standardEasing,
                            )
                        },
                        label = "relay filter indicator",
                    )
                    Box(
                        Modifier
                            .offset { IntOffset(indicatorOffset.roundToPx(), 0) }
                            .width(itemWidth)
                            .fillMaxHeight()
                            .testTag("relay-filter-selection")
                            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.extraLarge),
                    )
                    Row(Modifier.fillMaxWidth()) {
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
        }
        item {
            AnimatedContent(
                targetState = visible.isEmpty(),
                transitionSpec = {
                    fadeIn(androidx.compose.animation.core.tween(motion.duration(180))) togetherWith
                        fadeOut(androidx.compose.animation.core.tween(motion.duration(180)))
                },
                label = "relay empty state",
            ) { empty ->
                if (empty) {
                    EmptyState(
                        if (messages.isEmpty()) "队列是空的" else "这里还没有记录",
                        if (messages.isEmpty()) "新短信进入后会自动出现在这里。" else "切换到其他状态查看投递记录。",
                    )
                }
            }
        }
        if (visible.isNotEmpty()) {
            items(visible, key = { it.id }) { message ->
                RelayMessageCard(message, Modifier.animateItem(), retry)
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
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
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
