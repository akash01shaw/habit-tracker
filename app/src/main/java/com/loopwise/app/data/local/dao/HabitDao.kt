package com.loopwise.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.loopwise.app.data.local.entities.HabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE ownerId = :ownerId AND isDeleted = 0 ORDER BY createdAt ASC")
    fun observeHabits(ownerId: String): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): HabitEntity?

    @Query("SELECT * FROM habits WHERE isSynced = 0")
    suspend fun getUnsynced(): List<HabitEntity>

    @Upsert
    suspend fun upsert(habit: HabitEntity)

    @Upsert
    suspend fun upsertAll(habits: List<HabitEntity>)

    @Query("UPDATE habits SET isDeleted = 1, isSynced = 0 WHERE id = :id")
    suspend fun markDeleted(id: String)

    @Query("UPDATE habits SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)
}
