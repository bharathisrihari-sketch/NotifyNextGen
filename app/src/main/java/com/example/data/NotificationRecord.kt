package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notification_records",
    indices = [
        Index(value = ["packageName"]),
        Index(value = ["timestamp"]),
        Index(value = ["category"])
    ]
)
data class NotificationRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val appName: String,
    val title: String,
    val content: String,
    val subText: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val category: String = NotificationCategory.OTHER.name,
    val isStarred: Boolean = false,
    val isRead: Boolean = false,
    val notificationKey: String = "",
    val channelId: String = ""
)
