package com.loopwise.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.loopwise.app.data.local.entities.JournalEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalEntryDao {
    @Query(
        """
        SELECT * FROM journal_entries
        WHERE ownerId = :ownerId AND isDeleted = 0
        AND (:habitId IS NULL OR linkedHabitId = :habitId)
        AND (:type IS NULL OR type = :type)
        AND (:category IS NULL OR category = :category)
        ORDER BY createdAt DESC
        """,
    )
    fun observeEntries(ownerId: String, habitId: String?, type: String?, category: String?): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE isSynced = 0")
    suspend fun getUnsynced(): List<JournalEntryEntity>

    @Upsert
    suspend fun upsert(entry: JournalEntryEntity)

    @Upsert
    suspend fun upsertAll(entries: List<JournalEntryEntity>)

    @Query("UPDATE journal_entries SET isDeleted = 1, isSynced = 0 WHERE id = :id")
    suspend fun markDeleted(id: String)

    @Query("UPDATE journal_entries SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)
}
