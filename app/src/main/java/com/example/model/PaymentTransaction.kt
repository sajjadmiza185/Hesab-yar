package com.example.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TransactionType(val persianTitle: String) {
    PAYMENT("پرداخت"),
    CHARGE("باربرگ"),
    DISCOUNT("تخفیف")
}

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["customerId"]),
        Index(value = ["shamsiDate"]),
        Index(value = ["forYear", "forMonth"])
    ]
)
data class PaymentTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customerId: Long,
    val type: String = TransactionType.PAYMENT.name,
    val amount: Long,
    val shamsiDate: String,
    val timestamp: Long = System.currentTimeMillis(),
    val paymentMethod: String = "کارت به کارت",
    val referenceNumber: String = "",
    val forMonth: Int? = null,
    val forYear: Int? = null,
    val notes: String = ""
)
