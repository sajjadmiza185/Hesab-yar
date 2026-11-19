package com.example.model

data class CustomerWithGroup(
    val customer: Customer,
    val group: CustomerGroup?
)

data class CustomerAccountSummary(
    val customer: Customer,
    val group: CustomerGroup?,
    val totalCharges: Long,
    val totalPayments: Long,
    val totalDiscounts: Long,
    val balance: Long, // totalCharges - totalPayments - totalDiscounts (positive = debtor بدهکار)
    val monthlyCharges: Map<Int, Long> = emptyMap(), // Month (1..12) -> Amount
    val monthlyPayments: Map<Int, Long> = emptyMap() // Month (1..12) -> Amount
)

data class MonthFinancialSummary(
    val month: Int,
    val monthName: String,
    val totalCharges: Long,
    val totalPayments: Long,
    val balance: Long
)

data class DashboardStatistics(
    val totalCustomersCount: Int = 0,
    val activeDebtorsCount: Int = 0,
    val totalChargesAmount: Long = 0L,
    val totalPaymentsAmount: Long = 0L,
    val totalOutstandingDebt: Long = 0L,
    val currentMonthCharges: Long = 0L,
    val currentMonthPayments: Long = 0L
)
