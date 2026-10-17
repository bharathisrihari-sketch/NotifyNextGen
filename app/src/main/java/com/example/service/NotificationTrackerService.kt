package com.example.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.example.data.AppDatabase
import com.example.data.NotificationCategory
import com.example.data.NotificationRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class NotificationTrackerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val recentAlertDebounce = ConcurrentHashMap<String, Long>()

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: return

        // Skip our own app's status tracker notifications to avoid circular logging
        if (packageName == applicationContext.packageName) {
            // Unless it is explicitly marked as a simulated alert for testing
            val extras = sbn.notification?.extras
            val isTestAlert = extras?.getBoolean("is_test_alert", false) ?: false
            if (!isTestAlert) return
        }

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim() ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim() ?: ""

        val finalContent = when {
            bigText.isNotBlank() -> bigText
            text.isNotBlank() -> text
            else -> ""
        }

        // Ignore empty or purely internal notifications
        if (title.isBlank() && finalContent.isBlank()) return

        // Debounce identical alerts received within 2 seconds
        val dedupeKey = "$packageName|$title|$finalContent"
        val now = System.currentTimeMillis()
        val lastSeen = recentAlertDebounce[dedupeKey] ?: 0L
        if (now - lastSeen < 2000L) {
            return
        }
        recentAlertDebounce[dedupeKey] = now
        // Clean up old dedupe keys periodically
        if (recentAlertDebounce.size > 200) {
            recentAlertDebounce.entries.removeIf { now - it.value > 10000L }
        }

        val appName = try {
            val pm = packageManager
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, android.content.pm.PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
        }

        val category = NotificationCategory.classify(
            packageName = packageName,
            categoryTag = notification.category,
            title = title,
            content = finalContent
        ).name

        val postTime = if (sbn.postTime > 0) sbn.postTime else now

        val record = NotificationRecord(
            packageName = packageName,
            appName = appName,
            title = title.ifBlank { appName },
            content = finalContent,
            subText = subText,
            timestamp = postTime,
            category = category,
            isStarred = false,
            isRead = false,
            notificationKey = sbn.key ?: "",
            channelId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) notification.channelId ?: "" else ""
        )

        serviceScope.launch {
            try {
                AppDatabase.getDatabase(applicationContext).notificationDao().insert(record)
                Log.d("NotificationTracker", "Logged notification from $appName: $title")
            } catch (e: Exception) {
                Log.e("NotificationTracker", "Error persisting notification", e)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        // Kept in log history even when removed from system shade
    }

    companion object {
        fun isNotificationAccessGranted(context: Context): Boolean {
            val componentName = ComponentName(context, NotificationTrackerService::class.java)
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            return flat != null && flat.contains(componentName.flattenToString())
        }

        fun getSettingsIntent(): Intent {
            return Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }
}
