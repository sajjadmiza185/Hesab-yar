package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.PaymentTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC, id DESC")
    fun getAllTransactions(): Flow<List<PaymentTransaction>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC, id DESC")
    suspend fun getAllTransactionsList(): List<PaymentTransaction>

    @Query("SELECT * FROM transactions WHERE customerId = :customerId ORDER BY timestamp DESC, id DESC")
    fun getTransactionsForCustomer(customerId: Long): Flow<List<PaymentTransaction>>

    @Query("SELECT * FROM transactions WHERE customerId = :customerId ORDER BY timestamp DESC, id DESC")
    suspend fun getTransactionsForCustomerList(customerId: Long): List<PaymentTransaction>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): PaymentTransaction?

    @Query("SELECT * FROM transactions WHERE forYear = :year ORDER BY timestamp DESC")
    fun getTransactionsForYear(year: Int): Flow<List<PaymentTransaction>>

    @Query("SELECT * FROM transactions WHERE forYear = :year AND forMonth = :month ORDER BY timestamp DESC")
    fun getTransactionsForMonth(year: Int, month: Int): Flow<List<PaymentTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: PaymentTransaction): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<PaymentTransaction>)

    @Update
    suspend fun updateTransaction(transaction: PaymentTransaction)

    @Delete
    suspend fun deleteTransaction(transaction: PaymentTransaction)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("DELETE FROM transactions WHERE customerId = :customerId")
    suspend fun deleteTransactionsByCustomerId(customerId: Long)

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'PAYMENT'")
    fun getTotalPaymentsAmount(): Flow<Long?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'PAYMENT' AND forYear = :year")
    fun getTotalPaymentsAmountForYear(year: Int): Flow<Long?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'PAYMENT' AND forYear = :year AND forMonth = :month")
    fun getTotalPaymentsAmountForMonth(year: Int, month: Int): Flow<Long?>
}
