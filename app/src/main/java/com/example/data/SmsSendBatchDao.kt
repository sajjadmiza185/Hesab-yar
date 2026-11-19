package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.model.SmsSendBatch
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsSendBatchDao {
    @Query("SELECT * FROM sms_send_batches ORDER BY id ASC")
    suspend fun getAllList(): List<SmsSendBatch>

    @Query("SELECT * FROM sms_send_batches ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<SmsSendBatch>>

    @Query("SELECT * FROM sms_send_batches WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): SmsSendBatch?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(batch: SmsSendBatch): Long

    @Query("DELETE FROM sms_send_batches WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM sms_send_batches WHERE id IN (:ids)")
    suspend fun deleteMany(ids: List<Long>): Int

    @Query("DELETE FROM sms_send_batches")
    suspend fun deleteAll(): Int
}
