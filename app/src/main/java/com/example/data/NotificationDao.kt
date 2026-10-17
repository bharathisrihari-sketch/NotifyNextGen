package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class AppNotificationCount(
    val packageName: String,
    val appName: String,
    val totalCount: Int,
    val latestTimestamp: Long
)

data class CategoryCount(
    val category: String,
    val count: Int
)

@Dao
interface NotificationDao {

    @Query("SELECT * FROM notification_records ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationRecord>>

    @Query("SELECT * FROM notification_records WHERE isStarred = 1 ORDER BY timestamp DESC")
    fun getStarredNotifications(): Flow<List<NotificationRecord>>

    @Query("SELECT * FROM notification_records WHERE packageName = :packageName ORDER BY timestamp DESC")
    fun getByPackageName(packageName: String): Flow<List<NotificationRecord>>

    @Query("SELECT * FROM notification_records WHERE category = :category ORDER BY timestamp DESC")
    fun getByCategory(category: String): Flow<List<NotificationRecord>>

    @Query("""
        SELECT * FROM notification_records 
        WHERE title LIKE '%' || :query || '%' 
           OR content LIKE '%' || :query || '%' 
           OR appName LIKE '%' || :query || '%'
        ORDER BY timestamp DESC
    """)
    fun searchNotifications(query: String): Flow<List<NotificationRecord>>

    @Query("""
        SELECT packageName, appName, COUNT(*) as totalCount, MAX(timestamp) as latestTimestamp
        FROM notification_records
        GROUP BY packageName, appName
        ORDER BY totalCount DESC
    """)
    fun getAppCounts(): Flow<List<AppNotificationCount>>

    @Query("""
        SELECT category, COUNT(*) as count
        FROM notification_records
        GROUP BY category
    """)
    fun getCategoryCounts(): Flow<List<CategoryCount>>

    @Query("SELECT COUNT(*) FROM notification_records")
    suspend fun getTotalCount(): Int

    @Query("SELECT COUNT(*) FROM notification_records WHERE timestamp >= :sinceTimestamp")
    fun getCountSince(sinceTimestamp: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: NotificationRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<NotificationRecord>)

    @Update
    suspend fun update(record: NotificationRecord)

    @Query("UPDATE notification_records SET isStarred = :isStarred WHERE id = :id")
    suspend fun setStarred(id: Long, isStarred: Boolean)

    @Query("UPDATE notification_records SET isRead = 1")
    suspend fun markAllAsRead()

    @Query("DELETE FROM notification_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM notification_records WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOlderThan(cutoffTimestamp: Long): Int

    @Query("DELETE FROM notification_records WHERE packageName = :packageName")
    suspend fun deleteByPackage(packageName: String): Int

    @Query("DELETE FROM notification_records")
    suspend fun clearAll()
}
