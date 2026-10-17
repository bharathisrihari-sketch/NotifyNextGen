package com.example.ui

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.MainActivity
import com.example.NotifyVaultApp
import com.example.data.AppNotificationCount
import com.example.data.CategoryCount
import com.example.data.NotificationCategory
import com.example.data.NotificationRecord
import com.example.data.NotificationRepository
import com.example.service.NotificationTrackerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class HubTab(val label: String) {
    TIMELINE("Timeline"),
    APPS("By App"),
    ANALYTICS("Insights"),
    SETTINGS("Tools")
}

enum class GroupByMode {
    DATE,
    APP
}

data class FilterCriteria(
    val query: String = "",
    val category: NotificationCategory? = null,
    val packageName: String? = null,
    val onlyStarred: Boolean = false,
    val unreadOnly: Boolean = false
)

data class AnalyticsState(
    val totalCount: Int = 0,
    val todayCount: Int = 0,
    val starredCount: Int = 0,
    val topAppName: String = "None",
    val topAppCount: Int = 0,
    val hourlyCounts: List<Int> = List(24) { 0 },
    val categoryDistribution: Map<String, Int> = emptyMap()
)

class NotificationHubViewModel(
    private val repository: NotificationRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<NotificationCategory?>(null)
    val selectedCategory: StateFlow<NotificationCategory?> = _selectedCategory.asStateFlow()

    private val _selectedPackage = MutableStateFlow<String?>(null)
    val selectedPackage: StateFlow<String?> = _selectedPackage.asStateFlow()

    private val _onlyStarred = MutableStateFlow(false)
    val onlyStarred: StateFlow<Boolean> = _onlyStarred.asStateFlow()

    private val _unreadOnly = MutableStateFlow(false)
    val unreadOnly: StateFlow<Boolean> = _unreadOnly.asStateFlow()

    private val _groupByMode = MutableStateFlow(GroupByMode.DATE)
    val groupByMode: StateFlow<GroupByMode> = _groupByMode.asStateFlow()

    private val _activeTab = MutableStateFlow(HubTab.TIMELINE)
    val activeTab: StateFlow<HubTab> = _activeTab.asStateFlow()

    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    val isServiceConnected: StateFlow<Boolean> = NotificationTrackerService.isServiceConnected

    val appCounts: StateFlow<List<AppNotificationCount>> = repository.appCounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categoryCounts: StateFlow<List<CategoryCount>> = repository.categoryCounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filter criteria flow combining the 5 filters cleanly
    private val filterCriteria = combine(
        _searchQuery,
        _selectedCategory,
        _selectedPackage,
        _onlyStarred,
        _unreadOnly
    ) { query, cat, pkg, starred, unread ->
        FilterCriteria(
            query = query,
            category = cat,
            packageName = pkg,
            onlyStarred = starred,
            unreadOnly = unread
        )
    }

    // Filtered notifications combining repository.allNotifications with filterCriteria
    val filteredNotifications: StateFlow<List<NotificationRecord>> = combine(
        repository.allNotifications,
        filterCriteria
    ) { notifications, filter ->
        notifications.filter { item ->
            val matchesQuery = if (filter.query.isBlank()) true else {
                item.title.contains(filter.query, ignoreCase = true) ||
                item.content.contains(filter.query, ignoreCase = true) ||
                item.appName.contains(filter.query, ignoreCase = true) ||
                item.subText.contains(filter.query, ignoreCase = true)
            }
            val matchesCategory = filter.category == null || item.category == filter.category.name
            val matchesPackage = filter.packageName == null || item.packageName == filter.packageName
            val matchesStarred = !filter.onlyStarred || item.isStarred
            val matchesUnread = !filter.unreadOnly || !item.isRead

            matchesQuery && matchesCategory && matchesPackage && matchesStarred && matchesUnread
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Analytics computation
    val analyticsState: StateFlow<AnalyticsState> = combine(
        repository.allNotifications,
        appCounts
    ) { all, apps ->
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val todayNotifications = all.filter { it.timestamp >= startOfToday }
        val starred = all.count { it.isStarred }

        val hourly = IntArray(24)
        val cal = Calendar.getInstance()
        all.forEach { record ->
            cal.timeInMillis = record.timestamp
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            if (hour in 0..23) {
                hourly[hour]++
            }
        }

        val topApp = apps.maxByOrNull { it.totalCount }
        val catMap = all.groupingBy { it.category }.eachCount()

        AnalyticsState(
            totalCount = all.size,
            todayCount = todayNotifications.size,
            starredCount = starred,
            topAppName = topApp?.appName ?: "None",
            topAppCount = topApp?.totalCount ?: 0,
            hourlyCounts = hourly.toList(),
            categoryDistribution = catMap
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsState())

    fun refreshPermission(context: Context) {
        _hasPermission.value = NotificationTrackerService.isNotificationAccessGranted(context)
    }

    fun rebindService(context: Context) {
        NotificationTrackerService.requestServiceRebind(context)
        refreshPermission(context)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: NotificationCategory?) {
        _selectedCategory.value = if (_selectedCategory.value == category) null else category
    }

    fun selectPackage(packageName: String?) {
        _selectedPackage.value = if (_selectedPackage.value == packageName) null else packageName
    }

    fun toggleOnlyStarred() {
        _onlyStarred.value = !_onlyStarred.value
    }

    fun toggleUnreadOnly() {
        _unreadOnly.value = !_unreadOnly.value
    }

    fun setGroupByMode(mode: GroupByMode) {
        _groupByMode.value = mode
    }

    fun setActiveTab(tab: HubTab) {
        _activeTab.value = tab
    }

    fun toggleStar(record: NotificationRecord) {
        viewModelScope.launch {
            repository.toggleStar(record.id, record.isStarred)
        }
    }

    fun toggleReadStatus(record: NotificationRecord) {
        viewModelScope.launch {
            repository.setReadStatus(record.id, !record.isRead)
        }
    }

    fun deleteNotification(id: Long) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun deleteByPackage(packageName: String) {
        viewModelScope.launch {
            repository.deleteByPackage(packageName)
            if (_selectedPackage.value == packageName) {
                _selectedPackage.value = null
            }
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            repository.markAllAsRead()
        }
    }

    fun cleanupOlderThanDays(days: Int, onComplete: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val deleted = repository.deleteOlderThanDays(days)
            onComplete(deleted)
        }
    }

    fun postSimulatedNotification(
        context: Context,
        appName: String,
        title: String,
        content: String,
        category: NotificationCategory
    ) {
        viewModelScope.launch {
            val record = NotificationRecord(
                packageName = "com.simulated.${appName.lowercase().replace(" ", "")}",
                appName = appName,
                title = title,
                content = content,
                subText = "Simulated Alert",
                timestamp = System.currentTimeMillis(),
                category = category.name,
                isStarred = false,
                isRead = false,
                channelId = NotifyVaultApp.TEST_CHANNEL_ID
            )
            repository.insert(record)
        }

        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (notificationManager != null) {
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    System.currentTimeMillis().toInt(),
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                val builder = NotificationCompat.Builder(context, NotifyVaultApp.TEST_CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("$appName: $title")
                    .setContentText(content)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(content))
                    .setSubText(appName)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)

                notificationManager.notify((System.currentTimeMillis() % 100000).toInt(), builder.build())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun buildShareExportText(items: List<NotificationRecord>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val sb = StringBuilder()
        sb.append("=== NotifyVault Notification Log Export ===\n")
        sb.append("Exported: ${dateFormat.format(Date())}\n")
        sb.append("Total Records: ${items.size}\n\n")

        items.forEachIndexed { index, item ->
            sb.append("${index + 1}. [${item.appName}] ${item.title}\n")
            sb.append("   Date: ${dateFormat.format(Date(item.timestamp))}\n")
            sb.append("   Category: ${item.category}\n")
            if (item.content.isNotBlank()) {
                sb.append("   Message: ${item.content}\n")
            }
            sb.append("-------------------------------------------\n")
        }
        return sb.toString()
    }

    companion object {
        fun provideFactory(repository: NotificationRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return NotificationHubViewModel(repository) as T
                }
            }
    }
}
