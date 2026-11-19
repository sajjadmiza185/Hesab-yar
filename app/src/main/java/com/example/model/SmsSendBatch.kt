package com.example.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sms_send_batches",
    indices = [Index(value = ["createdAt"])]
)
data class SmsSendBatch(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val title: String = ""
)
