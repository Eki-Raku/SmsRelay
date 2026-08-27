package com.raku.smsrelay.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Outbox
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

enum class AppDestination(val label: String, val icon: ImageVector) {
    STATUS("状态", Icons.Outlined.Home),
    MESSAGES("短信", Icons.Outlined.ChatBubbleOutline),
    RELAY("转发", Icons.Outlined.Outbox),
    SETTINGS("设置", Icons.Outlined.Settings),
}

@Composable
fun SmsRelayNavigationBar(
    selected: AppDestination,
    hasUnreadMessages: Boolean,
    onSelect: (AppDestination) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        AppDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = selected == destination,
                onClick = { onSelect(destination) },
                icon = {
                    BadgedBox(
                        badge = {
                            if (destination == AppDestination.MESSAGES && hasUnreadMessages) Badge()
                        },
                    ) {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = destination.label,
                        )
                    }
                },
                label = { Text(destination.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = RelayPalette.Indigo,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = RelayPalette.Violet,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}
