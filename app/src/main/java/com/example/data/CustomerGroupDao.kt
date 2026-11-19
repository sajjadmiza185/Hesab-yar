package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.CustomerGroup
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerGroupDao {

    @Query("SELECT * FROM customer_groups ORDER BY name ASC")
    fun getAllGroups(): Flow<List<CustomerGroup>>

    @Query("SELECT * FROM customer_groups ORDER BY name ASC")
    suspend fun getAllGroupsList(): List<CustomerGroup>

    @Query("SELECT * FROM customer_groups WHERE id = :id LIMIT 1")
    suspend fun getGroupById(id: Long): CustomerGroup?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: CustomerGroup): Long

    @Update
    suspend fun updateGroup(group: CustomerGroup)

    @Delete
    suspend fun deleteGroup(group: CustomerGroup)

    @Query("DELETE FROM customer_groups WHERE id = :id")
    suspend fun deleteGroupById(id: Long)
}
