package com.loopwise.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.loopwise.app.data.local.entities.TimerSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TimerSessionDao {
    @Query("SELECT * FROM timer_sessions WHERE ownerId = :ownerId ORDER BY createdAt DESC LIMIT 20")
    fun observeRecentSessions(ownerId: String): Flow<List<TimerSessionEntity>>

    @Query("SELECT * FROM timer_sessions WHERE isSynced = 0")
    suspend fun getUnsynced(): List<TimerSessionEntity>

    @Upsert
    suspend fun upsert(session: TimerSessionEntity)

    @Query("UPDATE timer_sessions SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)
}
