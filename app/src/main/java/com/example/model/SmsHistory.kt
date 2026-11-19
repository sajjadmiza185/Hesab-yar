package com.example.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sms_history",
    indices = [
        Index(value = ["customerId"]),
        Index(value = ["sentAt"]),
        Index(value = ["status"]),
        Index(value = ["batchId"])
    ]
)
data class SmsHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customerId: Long,
    val customerName: String,
    val phoneNumber: String,
    val message: String,
    val amount: Long,
    val sentAt: Long = System.currentTimeMillis(),
    val status: String = "PENDING",
    val simSubscriptionId: Int = -1,
    val expectedParts: Int = 1,
    val completedSentParts: Int = 0,
    val failedSentParts: Int = 0,
    val completedDeliveryParts: Int = 0,
    val failedDeliveryParts: Int = 0,
    val errorReason: String = "",
    val batchId: Long? = null
)
