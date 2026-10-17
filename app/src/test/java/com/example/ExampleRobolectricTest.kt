package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.NotificationCategory
import com.example.data.NotificationRecord
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("NotifyVault", appName)
    }

    @Test
    fun `category classifier identifies messaging apps`() {
        val category = NotificationCategory.classify(
            packageName = "com.whatsapp",
            categoryTag = "msg",
            title = "John Doe",
            content = "Hey there, are we still meeting?"
        )
        assertEquals(NotificationCategory.MESSAGES, category)
    }

    @Test
    fun `category classifier identifies finance notifications`() {
        val category = NotificationCategory.classify(
            packageName = "com.chase.sig.android",
            categoryTag = null,
            title = "Debit Card Transaction",
            content = "You spent $25.00 at Grocery Store"
        )
        assertEquals(NotificationCategory.FINANCE, category)
    }

    @Test
    fun `insert and retrieve notification record from database`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getDatabase(context)
        val dao = db.notificationDao()

        val item = NotificationRecord(
            packageName = "com.slack",
            appName = "Slack",
            title = "#dev",
            content = "Build passed",
            category = NotificationCategory.WORK.name
        )
        val id = dao.insert(item)
        assertTrue(id > 0)

        val all = dao.getAllNotifications().first()
        assertTrue(all.any { it.packageName == "com.slack" })
    }

    @Test
    fun `notification tracker service provides valid settings intent`() {
        val intent = com.example.service.NotificationTrackerService.getSettingsIntent()
        assertEquals(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS, intent.action)
    }

    @Test
    fun `notification access check executes safely`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // In Robolectric test environment, listener is not enabled by default
        val isGranted = com.example.service.NotificationTrackerService.isNotificationAccessGranted(context)
        assertEquals(false, isGranted)
    }

    @Test
    fun `search notifications filters by app name and notification content`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getDatabase(context)
        val dao = db.notificationDao()

        dao.insert(
            NotificationRecord(
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "Alice",
                content = "Meeting at 4pm tomorrow",
                category = NotificationCategory.MESSAGES.name
            )
        )
        dao.insert(
            NotificationRecord(
                packageName = "com.spotify",
                appName = "Spotify",
                title = "Now Playing",
                content = "Bohemian Rhapsody",
                category = NotificationCategory.OTHER.name
            )
        )

        // Search by app name
        val appSearchResults = dao.searchNotifications("WhatsApp").first()
        assertTrue(appSearchResults.any { it.appName == "WhatsApp" })
        assertTrue(appSearchResults.none { it.appName == "Spotify" })

        // Search by content keyword
        val contentSearchResults = dao.searchNotifications("Bohemian").first()
        assertTrue(contentSearchResults.any { it.content.contains("Bohemian") })
        assertTrue(contentSearchResults.none { it.appName == "WhatsApp" })
    }

    @Test
    fun `toggle read status updates record in database`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getDatabase(context)
        val dao = db.notificationDao()

        val id = dao.insert(
            NotificationRecord(
                packageName = "com.telegram",
                appName = "Telegram",
                title = "News Channel",
                content = "Latest update published",
                isRead = false
            )
        )

        dao.setReadStatus(id, true)
        val updatedList = dao.getAllNotifications().first()
        val record = updatedList.find { it.id == id }
        assertTrue(record != null && record.isRead)
    }
}
