package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.model.PendingSmsNotification
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingSmsDao {
    @Query("SELECT * FROM pending_sms_notifications ORDER BY id ASC")
    suspend fun getAllList(): List<PendingSmsNotification>

    @Query("SELECT * FROM pending_sms_notifications WHERE status = 'PENDING' ORDER BY receivedAt DESC")
    fun pending(): Flow<List<PendingSmsNotification>>
    @Query("SELECT COUNT(*) FROM pending_sms_notifications WHERE status = 'PENDING'")
    fun pendingCount(): Flow<Int>
    @Query("SELECT * FROM pending_sms_notifications WHERE id = :id LIMIT 1")
    suspend fun byId(id: Long): PendingSmsNotification?
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: PendingSmsNotification): Long
    @Query("UPDATE pending_sms_notifications SET status = :status WHERE id = :id AND status = 'PENDING'")
    suspend fun setStatus(id: Long, status: String): Int

    @Query("UPDATE pending_sms_notifications SET status = 'REJECTED' WHERE id IN (:ids) AND status = 'PENDING'")
    suspend fun rejectMany(ids: List<Long>): Int
}
