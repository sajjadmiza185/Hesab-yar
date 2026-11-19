package com.example.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "pending_sms_notifications", indices = [Index(value = ["dedupeKey"], unique = true)])
data class PendingSmsNotification(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val customerName: String,
    val senderNumber: String,
    val body: String,
    val amountRial: Long,
    val shamsiDate: String,
    val forMonth: Int,
    val forYear: Int,
    val trackingNumber: String,
    val receivedAt: Long = System.currentTimeMillis(),
    val status: String = "PENDING",
    val dedupeKey: String
)
