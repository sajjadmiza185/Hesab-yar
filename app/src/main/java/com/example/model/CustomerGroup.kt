package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customer_groups")
data class CustomerGroup(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val colorHex: String = "#1E88E5",
    val leaderCustomerId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
