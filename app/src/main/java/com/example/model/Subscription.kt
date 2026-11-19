package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subscription")
data class Subscription(
    @PrimaryKey
    val id: Int = 1,
    val planCode: String = "FREE",
    val customerLimit: Int = 5,
    val purchaseToken: String = "",
    val productId: String = "",
    val purchasedAt: Long = 0L,
    val isVerified: Boolean = false
)
