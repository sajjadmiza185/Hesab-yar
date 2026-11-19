package com.example.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "monthly_charges",
    indices = [
        Index(value = ["customerId", "year", "month"], unique = true),
        Index(value = ["year", "month"])
    ]
)
data class MonthlyCharge(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customerId: Long,
    val year: Int,
    val month: Int, // 1 to 12 (فروردین تا اسفند)
    val amount: Long, // Saved as Long (Rial or Toman) to avoid float rounding errors
    val title: String = "",
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
