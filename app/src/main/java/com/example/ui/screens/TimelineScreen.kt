package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NotificationRecord
import com.example.ui.components.NotificationCard
import java.util.Calendar

@Composable
fun TimelineScreen(
    notifications: List<NotificationRecord>,
    onToggleStar: (NotificationRecord) -> Unit,
    onDeleteNotification: (Long) -> Unit,
    onMarkAllRead: () -> Unit,
    onRequestSimulateAlert: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inspectingRecord by remember { mutableStateOf<NotificationRecord?>(null) }

    inspectingRecord?.let { record ->
        DetailDialog(
            record = record,
            onDismiss = { inspectingRecord = null },
            onToggleStar = { onToggleStar(record) },
            onDelete = { onDeleteNotification(record.id) }
        )
    }

    if (notifications.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "No Notifications Found",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "New alerts from your apps will automatically appear here once captured.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onRequestSimulateAlert,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("empty_state_simulate_button")
                ) {
                    Icon(Icons.Default.AddAlert, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Trigger Test Alert")
                }
            }
        }
    } else {
        // Group notifications into Today, Yesterday, Earlier
        val grouped = remember(notifications) {
            val startOfToday = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val startOfYesterday = startOfToday - (24 * 60 * 60 * 1000L)

            val map = linkedMapOf<String, MutableList<NotificationRecord>>()
            notifications.forEach { item ->
                val groupKey = when {
                    item.timestamp >= startOfToday -> "Today"
                    item.timestamp >= startOfYesterday -> "Yesterday"
                    else -> "Earlier"
                }
                map.getOrPut(groupKey) { mutableListOf() }.add(item)
            }
            map
        }

        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .testTag("notifications_timeline_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header stats & mark read row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${notifications.size} ${if (notifications.size == 1) "alert" else "alerts"} logged",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedButton(
                        onClick = onMarkAllRead,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mark all read", fontSize = 11.sp)
                    }
                }
            }

            grouped.forEach { (header, items) ->
                item(key = "header_$header") {
                    Text(
                        text = header,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                    )
                }

                items(items, key = { it.id }) { record ->
                    NotificationCard(
                        record = record,
                        onToggleStar = { onToggleStar(record) },
                        onDelete = { onDeleteNotification(record.id) },
                        onClick = { inspectingRecord = record }
                    )
                }
            }
        }
    }
}
