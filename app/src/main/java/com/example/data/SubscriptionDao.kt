package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.model.Subscription
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscription WHERE id = 1")
    fun getSubscription(): Flow<Subscription?>

    @Query("SELECT * FROM subscription WHERE id = 1")
    suspend fun getSubscriptionOnce(): Subscription?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSubscription(subscription: Subscription)
}
