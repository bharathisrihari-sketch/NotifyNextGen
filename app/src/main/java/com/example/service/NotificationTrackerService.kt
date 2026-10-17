package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Android NotificationListenerService to intercept incoming device notifications
 * across all installed apps and persist them into the local Room database.
 */
class NotificationTrackerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val recentAlertDebounce = ConcurrentHashMap<String, Long>()

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "NotificationTrackerService created")
    }

    override fun onDestroy() {
        super.onDestroy()
        _isServiceConnected.value = false
        serviceScope.cancel()
        Log.d(TAG, "NotificationTrackerService destroyed")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        _isServiceConnected.value = true
        Log.d(TAG, "NotificationTrackerService connected to system notification pipeline")

        // Intercept all currently active notifications on connection
        try {
            val activeNotifs = activeNotifications
            if (activeNotifs != null && activeNotifs.isNotEmpty()) {
                Log.d(TAG, "Syncing ${activeNotifs.size} existing active notifications from status bar")
                for (sbn in activeNotifs) {
                    processNotification(sbn)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching active notifications on listener connected", e)
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        _isServiceConnected.value = false
        Log.w(TAG, "NotificationTrackerService disconnected. Attempting rebind...")

        // Request rebind if system dropped connection
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                requestRebind(ComponentName(this, NotificationTrackerService::class.java))
            } catch (e: Exception) {
                Log.e(TAG, "Error requesting rebind", e)
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return
        processNotification(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        // Kept in log history even when dismissed from system notification shade
    }

    private fun processNotification(sbn: StatusBarNotification) {
        val packageName = sbn.packageName ?: return

        // Skip our own app's internal status notifications to avoid circular loops
        if (packageName == applicationContext.packageName) {
            val extras = sbn.notification?.extras
            val isTestAlert = extras?.getBoolean("is_test_alert", false) ?: false
            if (!isTestAlert) return
        }

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        // Extract Title and Conversation/Group Title
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim() ?: ""
        val conversationTitle = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()?.trim() ?: ""
        val resolvedTitle = when {
            conversationTitle.isNotBlank() && title.isNotBlank() -> "$conversationTitle • $title"
            conversationTitle.isNotBlank() -> conversationTitle
            else -> title
        }

        // Extract Message Body, BigText, SubText, and InboxStyle Lines
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim() ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim() ?: ""
        val summaryText = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString()?.trim() ?: ""

        // Handle InboxStyle (e.g. multiple email lines or stacked chat messages)
        val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
        val formattedLines = textLines?.joinToString("\n") { it.toString().trim() } ?: ""

        val finalContent = when {
            bigText.isNotBlank() -> bigText
            formattedLines.isNotBlank() -> formattedLines
            text.isNotBlank() -> text
            summaryText.isNotBlank() -> summaryText
            else -> ""
        }

        // Ignore empty / purely blank system broadcast ping notifications
        if (resolvedTitle.isBlank() && finalContent.isBlank()) return

        // Debounce identical alerts received within 2.5 seconds (prevents duplicate spam on progress/media updates)
        val dedupeKey = "$packageName|$resolvedTitle|$finalContent"
        val now = System.currentTimeMillis()
        val lastSeen = recentAlertDebounce[dedupeKey] ?: 0L
        if (now - lastSeen < 2500L) {
            return
        }
        recentAlertDebounce[dedupeKey] = now

        // Periodically purge old debounce entries
        if (recentAlertDebounce.size > 200) {
            recentAlertDebounce.entries.removeIf { now - it.value > 15000L }
        }

        // Extract readable App Name via PackageManager
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

        // Auto-classify notification into smart category
        val category = NotificationCategory.classify(
            packageName = packageName,
            categoryTag = notification.category,
            title = resolvedTitle,
            content = finalContent
        ).name

        val postTime = if (sbn.postTime > 0) sbn.postTime else now

        val channelId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification.channelId ?: ""
        } else {
            ""
        }

        val record = NotificationRecord(
            packageName = packageName,
            appName = appName,
            title = resolvedTitle.ifBlank { appName },
            content = finalContent,
            subText = if (subText.isNotBlank()) subText else summaryText,
            timestamp = postTime,
            category = category,
            isStarred = false,
            isRead = false,
            notificationKey = sbn.key ?: "",
            channelId = channelId
        )

        // Store into Room Database via background coroutine
        serviceScope.launch {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                val id = db.notificationDao().insert(record)
                Log.d(TAG, "Successfully intercepted & saved notification [id=$id] from $appName: $resolvedTitle")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist intercepted notification into Room", e)
            }
        }
    }

    companion object {
        private const val TAG = "NotificationTracker"

        private val _isServiceConnected = MutableStateFlow(false)
        val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

        /**
         * Checks whether the user has enabled Notification Listener access for this app in Android Settings.
         */
        fun isNotificationAccessGranted(context: Context): Boolean {
            val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
            if (enabledPackages.contains(context.packageName)) {
                return true
            }

            // Fallback check against Settings.Secure
            val componentName = ComponentName(context, NotificationTrackerService::class.java)
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            return flat != null && flat.contains(componentName.flattenToString())
        }

        /**
         * Generates intent directly opening the Notification Listener Settings page.
         */
        fun getSettingsIntent(): Intent {
            return Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        /**
         * Requests rebind of the NotificationListenerService if access is granted.
         */
        fun requestServiceRebind(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isNotificationAccessGranted(context)) {
                try {
                    requestRebind(ComponentName(context, NotificationTrackerService::class.java))
                } catch (e: Exception) {
                    Log.e(TAG, "Error in manual requestRebind", e)
                }
            }
        }
    }
}
