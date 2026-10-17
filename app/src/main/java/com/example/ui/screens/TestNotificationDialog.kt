package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.NotificationCategory

data class AlertPreset(
    val appName: String,
    val title: String,
    val content: String,
    val category: NotificationCategory
)

@Composable
fun TestNotificationDialog(
    onDismiss: () -> Unit,
    onSendAlert: (appName: String, title: String, content: String, category: NotificationCategory) -> Unit
) {
    val presets = remember {
        listOf(
            AlertPreset(
                appName = "WhatsApp",
                title = "Jessica Miller",
                content = "Let's meet at 5 PM at Blue Bottle Coffee!",
                category = NotificationCategory.MESSAGES
            ),
            AlertPreset(
                appName = "Slack",
                title = "#product-launch • David Kim",
                content = "Release v2.4 has been published to all users worldwide.",
                category = NotificationCategory.WORK
            ),
            AlertPreset(
                appName = "Bank of America",
                title = "Security Alert: New Sign-in",
                content = "New sign-in detected on Chrome macOS near San Francisco.",
                category = NotificationCategory.FINANCE
            ),
            AlertPreset(
                appName = "Amazon",
                title = "Order Shipped: Sony WH-1000XM5",
                content = "Your package is on its way and will arrive tomorrow by 8 PM.",
                category = NotificationCategory.SHOPPING
            )
        )
    }

    var selectedPreset by remember { mutableStateOf<AlertPreset?>(presets[0]) }
    var appName by remember { mutableStateOf(presets[0].appName) }
    var title by remember { mutableStateOf(presets[0].title) }
    var content by remember { mutableStateOf(presets[0].content) }
    var selectedCategory by remember { mutableStateOf(presets[0].category) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("test_alert_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Simulate Incoming Alert",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Text(
                    text = "Verify tracking immediately by triggering a realistic test alert.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Presets row
                Text(
                    text = "Quick Presets",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { preset ->
                        FilterChip(
                            selected = selectedPreset == preset,
                            onClick = {
                                selectedPreset = preset
                                appName = preset.appName
                                title = preset.title
                                content = preset.content
                                selectedCategory = preset.category
                            },
                            label = { Text(preset.appName) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(14.dp))

                // App Name Field
                OutlinedTextField(
                    value = appName,
                    onValueChange = {
                        appName = it
                        selectedPreset = null
                    },
                    label = { Text("App Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("test_alert_app_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Title Field
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        selectedPreset = null
                    },
                    label = { Text("Notification Title / Sender") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("test_alert_title_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Content Field
                OutlinedTextField(
                    value = content,
                    onValueChange = {
                        content = it
                        selectedPreset = null
                    },
                    label = { Text("Message Body / Content") },
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("test_alert_content_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Category selector
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    NotificationCategory.values().filter { it != NotificationCategory.ALL }.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.displayName, fontSize = 12.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Fire button
                Button(
                    onClick = {
                        onSendAlert(
                            appName.ifBlank { "TestApp" },
                            title.ifBlank { "New Alert" },
                            content.ifBlank { "Test notification message payload." },
                            selectedCategory
                        )
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("send_test_alert_confirm_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Trigger Alert & Log", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
