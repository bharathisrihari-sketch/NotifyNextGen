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
}
