package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.data.AppDatabase
import com.example.data.NotificationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NotifyVaultApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: NotificationRepository
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getDatabase(this)
        repository = NotificationRepository(database.notificationDao())

        createNotificationChannels()

        // Seed realistic notifications on initial launch
        appScope.launch {
            repository.seedSampleDataIfEmpty()
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                TEST_CHANNEL_ID,
                "Demo & Test Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel used for testing notification tracking and instant simulations"
                enableLights(true)
                enableVibration(true)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val TEST_CHANNEL_ID = "notify_vault_test_channel"
        lateinit var instance: NotifyVaultApp
            private set
    }
}
