package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.MonthlyCharge
import kotlinx.coroutines.flow.Flow

@Dao
interface MonthlyChargeDao {

    @Query("SELECT * FROM monthly_charges WHERE year = :year")
    fun getChargesForYear(year: Int): Flow<List<MonthlyCharge>>

    @Query("SELECT * FROM monthly_charges WHERE year = :year")
    suspend fun getChargesForYearList(year: Int): List<MonthlyCharge>

    @Query("SELECT * FROM monthly_charges WHERE customerId = :customerId AND year = :year ORDER BY month ASC")
    fun getCustomerChargesForYear(customerId: Long, year: Int): Flow<List<MonthlyCharge>>

    @Query("SELECT * FROM monthly_charges WHERE customerId = :customerId AND year = :year ORDER BY month ASC")
    suspend fun getCustomerChargesForYearList(customerId: Long, year: Int): List<MonthlyCharge>

    @Query("SELECT * FROM monthly_charges WHERE customerId = :customerId")
    fun getAllCustomerCharges(customerId: Long): Flow<List<MonthlyCharge>>

    @Query("SELECT * FROM monthly_charges WHERE year = :year AND month = :month")
    fun getChargesForMonth(year: Int, month: Int): Flow<List<MonthlyCharge>>

    @Query("SELECT * FROM monthly_charges WHERE customerId = :customerId AND year = :year AND month = :month LIMIT 1")
    suspend fun getCharge(customerId: Long, year: Int, month: Int): MonthlyCharge?

    @Query("SELECT * FROM monthly_charges")
    suspend fun getAllChargesList(): List<MonthlyCharge>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(charge: MonthlyCharge): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(charges: List<MonthlyCharge>)

    @Update
    suspend fun update(charge: MonthlyCharge)

    @Delete
    suspend fun delete(charge: MonthlyCharge)

    @Query("DELETE FROM monthly_charges WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM monthly_charges WHERE customerId = :customerId")
    suspend fun deleteByCustomerId(customerId: Long)

    @Query("SELECT SUM(amount) FROM monthly_charges WHERE year = :year")
    fun getTotalChargesAmountForYear(year: Int): Flow<Long?>

    @Query("SELECT SUM(amount) FROM monthly_charges WHERE year = :year AND month = :month")
    fun getTotalChargesAmountForMonth(year: Int, month: Int): Flow<Long?>
}
