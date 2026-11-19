package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.model.SmsHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsHistoryDao {
    @Query("SELECT * FROM sms_history ORDER BY id ASC")
    suspend fun getAllList(): List<SmsHistory>

    @Query("SELECT * FROM sms_history ORDER BY sentAt DESC")
    fun observeAll(): Flow<List<SmsHistory>>

    @Query("SELECT * FROM sms_history WHERE customerId = :customerId ORDER BY sentAt DESC")
    fun observeByCustomer(customerId: Long): Flow<List<SmsHistory>>

    @Query("SELECT * FROM sms_history WHERE batchId = :batchId ORDER BY sentAt DESC")
    fun observeByBatch(batchId: Long): Flow<List<SmsHistory>>

    @Query("SELECT * FROM sms_history WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): SmsHistory?

    @Query("SELECT COUNT(*) FROM sms_history WHERE status = 'SENT'")
    fun observeSuccessfulCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sms_history WHERE status = 'FAILED'")
    fun observeFailedCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: SmsHistory): Long

    @Query("""
        UPDATE sms_history
        SET completedSentParts = :completed,
            failedSentParts = :failed,
            status = :status,
            errorReason = :errorReason
        WHERE id = :id
    """)
    suspend fun updateSentResult(
        id: Long, completed: Int, failed: Int, status: String, errorReason: String
    ): Int

    @Query("""
        UPDATE sms_history
        SET completedDeliveryParts = :completed,
            failedDeliveryParts = :failed,
            status = CASE
                WHEN failedSentParts > 0 THEN 'FAILED'
                WHEN completedSentParts < expectedParts THEN 'PENDING'
                WHEN failedDeliveryParts + :failed > 0 THEN 'DELIVERY_FAILED'
                WHEN completedDeliveryParts + :completed >= expectedParts THEN 'DELIVERED'
                ELSE status
            END
        WHERE id = :id
    """)
    suspend fun updateDeliveryResult(id: Long, completed: Int, failed: Int): Int

    @Query("""
        UPDATE sms_history
        SET completedSentParts = completedSentParts + :succeeded,
            failedSentParts = failedSentParts + :failed,
            status = CASE
                WHEN failedSentParts + :failed > 0 THEN 'FAILED'
                WHEN completedSentParts + :succeeded < expectedParts THEN 'PENDING'
                WHEN failedDeliveryParts > 0 THEN 'DELIVERY_FAILED'
                WHEN completedDeliveryParts >= expectedParts THEN 'DELIVERED'
                ELSE 'SENT'
            END,
            errorReason = CASE
                WHEN :failed > 0 THEN :errorReason
                ELSE errorReason
            END
        WHERE id = :id
    """)
    suspend fun recordSentPart(
        id: Long, succeeded: Int, failed: Int, errorReason: String
    ): Int

    @Query("""
        UPDATE sms_history
        SET completedDeliveryParts = completedDeliveryParts + :succeeded,
            failedDeliveryParts = failedDeliveryParts + :failed,
            status = CASE
                WHEN failedSentParts > 0 THEN 'FAILED'
                WHEN completedSentParts < expectedParts THEN 'PENDING'
                WHEN failedDeliveryParts + :failed > 0 THEN 'DELIVERY_FAILED'
                WHEN completedDeliveryParts + :succeeded >= expectedParts THEN 'DELIVERED'
                ELSE 'SENT'
            END
        WHERE id = :id
    """)
    suspend fun recordDeliveryPart(id: Long, succeeded: Int, failed: Int): Int

    @Query("DELETE FROM sms_history WHERE batchId = :batchId")
    suspend fun deleteByBatchId(batchId: Long): Int

    @Query("DELETE FROM sms_history WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM sms_history WHERE id IN (:ids)")
    suspend fun deleteMany(ids: List<Long>): Int

    @Query("DELETE FROM sms_history WHERE sentAt >= :start AND sentAt < :end")
    suspend fun deleteInDateRange(start: Long, end: Long): Int

    @Query("DELETE FROM sms_history")
    suspend fun deleteAll(): Int
}
