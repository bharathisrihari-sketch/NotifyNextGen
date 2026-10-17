package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.AutoDelete
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.ui.theme.Coral40
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.Indigo40

@Composable
fun SettingsScreen(
    hasPermission: Boolean,
    onOpenPermissionSettings: () -> Unit,
    onRequestSimulateAlert: () -> Unit,
    onExportLogs: () -> Unit,
    onMarkAllRead: () -> Unit,
    onCleanOlderThanDays: (Int) -> Unit,
    onClearAllLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showClearConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Erase All Logged Notifications?") },
            text = { Text("This permanently deletes your entire notification history across all applications. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllLogs()
                        showClearConfirm = false
                        Toast.makeText(context, "All notification logs wiped", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen_column"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Permission Status Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (hasPermission) {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                    } else {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (hasPermission) EmeraldSuccess.copy(alpha = 0.15f)
                                else Coral40.copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (hasPermission) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (hasPermission) EmeraldSuccess else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (hasPermission) "Notification Access Active" else "Access Permission Required",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (hasPermission) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = if (hasPermission) "NotifyVault is actively listening and archiving incoming alerts." else "Android requires explicit permission to intercept notifications.",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (hasPermission) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onOpenPermissionSettings,
                        colors = if (hasPermission) {
                            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        } else {
                            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (hasPermission) "Settings" else "Enable",
                            color = if (hasPermission) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onError,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (hasPermission) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onError
                        )
                    }
                }
            }
        }

        // Tools Section
        item {
            Text(
                text = "Diagnostics & Testing",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SettingActionRow(
                        title = "Simulate Incoming Alert",
                        subtitle = "Trigger a realistic test notification from WhatsApp, Slack, or Chase",
                        icon = Icons.Default.AddAlert,
                        onClick = onRequestSimulateAlert
                    )

                    SettingActionRow(
                        title = "Export Notification History",
                        subtitle = "Generate a formatted summary and share via any app",
                        icon = Icons.Default.Share,
                        onClick = onExportLogs
                    )

                    SettingActionRow(
                        title = "Mark All Alerts As Read",
                        subtitle = "Clear unread badges across all recorded notifications",
                        icon = Icons.Default.DoneAll,
                        onClick = {
                            onMarkAllRead()
                            Toast.makeText(context, "All marked as read", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }

        // Data Retention Section
        item {
            Text(
                text = "Data Retention & Storage",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SettingActionRow(
                        title = "Clean Alerts Older Than 7 Days",
                        subtitle = "Delete historical records older than one week",
                        icon = Icons.Default.AutoDelete,
                        onClick = {
                            onCleanOlderThanDays(7)
                            Toast.makeText(context, "Cleaned alerts older than 7 days", Toast.LENGTH_SHORT).show()
                        }
                    )

                    SettingActionRow(
                        title = "Clean Alerts Older Than 30 Days",
                        subtitle = "Keep only the past month of notifications",
                        icon = Icons.Default.AutoDelete,
                        onClick = {
                            onCleanOlderThanDays(30)
                            Toast.makeText(context, "Cleaned alerts older than 30 days", Toast.LENGTH_SHORT).show()
                        }
                    )

                    SettingActionRow(
                        title = "Wipe All Notification Data",
                        subtitle = "Permanently clear the SQLite database",
                        icon = Icons.Default.DeleteForever,
                        iconTint = MaterialTheme.colorScheme.error,
                        onClick = { showClearConfirm = true }
                    )
                }
            }
        }

        // Privacy Guarantee Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Indigo40.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Indigo40,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "100% On-Device & Private",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "NotifyVault never uploads or transmits your notifications. All data is securely kept in an encrypted local database on your phone.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingActionRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
