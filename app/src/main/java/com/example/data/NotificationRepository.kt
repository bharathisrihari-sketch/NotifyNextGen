package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class NotificationRepository(private val dao: NotificationDao) {

    val allNotifications: Flow<List<NotificationRecord>> = dao.getAllNotifications()
    val starredNotifications: Flow<List<NotificationRecord>> = dao.getStarredNotifications()
    val appCounts: Flow<List<AppNotificationCount>> = dao.getAppCounts()
    val categoryCounts: Flow<List<CategoryCount>> = dao.getCategoryCounts()

    fun getByPackage(packageName: String): Flow<List<NotificationRecord>> =
        dao.getByPackageName(packageName)

    fun getByCategory(category: String): Flow<List<NotificationRecord>> =
        dao.getByCategory(category)

    fun search(query: String): Flow<List<NotificationRecord>> =
        dao.searchNotifications(query)

    fun getCountSince(timestamp: Long): Flow<Int> =
        dao.getCountSince(timestamp)

    suspend fun insert(record: NotificationRecord): Long = withContext(Dispatchers.IO) {
        dao.insert(record)
    }

    suspend fun insertAll(records: List<NotificationRecord>) = withContext(Dispatchers.IO) {
        dao.insertAll(records)
    }

    suspend fun toggleStar(id: Long, currentStarred: Boolean) = withContext(Dispatchers.IO) {
        dao.setStarred(id, !currentStarred)
    }

    suspend fun setReadStatus(id: Long, isRead: Boolean) = withContext(Dispatchers.IO) {
        dao.setReadStatus(id, isRead)
    }

    suspend fun markAllAsRead() = withContext(Dispatchers.IO) {
        dao.markAllAsRead()
    }

    suspend fun deleteById(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }

    suspend fun deleteByPackage(packageName: String): Int = withContext(Dispatchers.IO) {
        dao.deleteByPackage(packageName)
    }

    suspend fun deleteOlderThanDays(days: Int): Int = withContext(Dispatchers.IO) {
        val cutoff = System.currentTimeMillis() - (days.toLong() * 24L * 60L * 60L * 1000L)
        dao.deleteOlderThan(cutoff)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        dao.clearAll()
    }

    suspend fun seedSampleDataIfEmpty() = withContext(Dispatchers.IO) {
        if (dao.getTotalCount() == 0) {
            val now = System.currentTimeMillis()
            val sampleItems = listOf(
                NotificationRecord(
                    packageName = "com.whatsapp",
                    appName = "WhatsApp",
                    title = "Sarah Jenkins",
                    content = "Hey! Did you review the quarterly budget deck? Let me know when you're free for a quick sync.",
                    subText = "Product Team",
                    timestamp = now - (3 * 60 * 1000L), // 3 min ago
                    category = NotificationCategory.MESSAGES.name,
                    isStarred = true,
                    isRead = false,
                    channelId = "messages"
                ),
                NotificationRecord(
                    packageName = "com.slack",
                    appName = "Slack",
                    title = "#dev-engineering • Alex Rivera",
                    content = "Deployment to staging succeeded! All automated E2E tests passed cleanly. Ready for QA sign-off.",
                    subText = "Acme Corp",
                    timestamp = now - (14 * 60 * 1000L), // 14 min ago
                    category = NotificationCategory.WORK.name,
                    isStarred = true,
                    isRead = false,
                    channelId = "channel_mentions"
                ),
                NotificationRecord(
                    packageName = "com.chase.sig.android",
                    appName = "Chase Mobile",
                    title = "Transaction Alert: $42.50",
                    content = "Your card ending in 4821 was charged $42.50 at Trader Joe's #104. Available balance: $4,280.12.",
                    subText = "Debit Card Alerts",
                    timestamp = now - (48 * 60 * 1000L), // 48 min ago
                    category = NotificationCategory.FINANCE.name,
                    isStarred = false,
                    isRead = true,
                    channelId = "fraud_alerts"
                ),
                NotificationRecord(
                    packageName = "com.amazon.mShop.android.shopping",
                    appName = "Amazon",
                    title = "Package Delivered!",
                    content = "Your package with 'Anker USB-C Fast Charger' was left near the front door or porch.",
                    subText = "Order #112-9481923",
                    timestamp = now - (2 * 60 * 60 * 1000L), // 2 hours ago
                    category = NotificationCategory.SHOPPING.name,
                    isStarred = false,
                    isRead = true,
                    channelId = "shipping_updates"
                ),
                NotificationRecord(
                    packageName = "com.google.android.calendar",
                    appName = "Google Calendar",
                    title = "Design Architecture Review in 15 mins",
                    content = "10:30 AM - 11:30 AM • Google Meet link attached. Attendees: Design & Core Mobile team.",
                    subText = "Work Calendar",
                    timestamp = now - (3 * 60 * 60 * 1000L),
                    category = NotificationCategory.WORK.name,
                    isStarred = false,
                    isRead = true,
                    channelId = "event_reminders"
                ),
                NotificationRecord(
                    packageName = "com.instagram.android",
                    appName = "Instagram",
                    title = "emily_travels tagged you in a story",
                    content = "Take a look at the memory from Kyoto: 'Best ramen spot hidden in the alleys!'",
                    subText = "Direct Messages",
                    timestamp = now - (5 * 60 * 60 * 1000L),
                    category = NotificationCategory.SOCIAL.name,
                    isStarred = false,
                    isRead = true,
                    channelId = "social_activity"
                ),
                NotificationRecord(
                    packageName = "com.ubercab",
                    appName = "Uber",
                    title = "Receipt for your trip with Marcus",
                    content = "Total $18.40 was charged to your Apple Pay. Rate your driver and add a tip.",
                    subText = "Trips",
                    timestamp = now - (7 * 60 * 60 * 1000L),
                    category = NotificationCategory.FINANCE.name,
                    isStarred = false,
                    isRead = true,
                    channelId = "receipts"
                ),
                NotificationRecord(
                    packageName = "com.google.android.youtube",
                    appName = "YouTube",
                    title = "MKBHD uploaded a new video",
                    content = "The Smartphone Awards 2026: The Best & Worst Devices Reviewed!",
                    subText = "Subscriptions",
                    timestamp = now - (12 * 60 * 60 * 1000L),
                    category = NotificationCategory.SOCIAL.name,
                    isStarred = false,
                    isRead = true,
                    channelId = "subscriptions"
                ),
                NotificationRecord(
                    packageName = "android",
                    appName = "Android System",
                    title = "System Update Completed",
                    content = "Security patch version 2026-10-01 has been applied successfully. Your device is up to date.",
                    subText = "System Alerts",
                    timestamp = now - (24 * 60 * 60 * 1000L), // Yesterday
                    category = NotificationCategory.SYSTEM.name,
                    isStarred = false,
                    isRead = true,
                    channelId = "system_status"
                ),
                NotificationRecord(
                    packageName = "org.telegram.messenger",
                    appName = "Telegram",
                    title = "Tech Insights Daily",
                    content = "Kotlin 2.2 introduces blazing fast compile times and updated Jetpack Compose optimizations.",
                    subText = "Channel Broadcast",
                    timestamp = now - (28 * 60 * 60 * 1000L),
                    category = NotificationCategory.MESSAGES.name,
                    isStarred = false,
                    isRead = true,
                    channelId = "channels"
                )
            )
            dao.insertAll(sampleItems)
        }
    }
}
