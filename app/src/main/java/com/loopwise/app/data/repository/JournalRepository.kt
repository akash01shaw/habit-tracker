package com.loopwise.app.data.repository

import com.loopwise.app.data.local.dao.JournalEntryDao
import com.loopwise.app.data.local.entities.toDomain
import com.loopwise.app.data.local.entities.toEntity
import com.loopwise.app.data.remote.dto.JournalEntryDto
import com.loopwise.app.data.remote.dto.toDomain
import com.loopwise.app.data.remote.dto.toDto
import com.loopwise.app.domain.model.JournalEntry
import com.loopwise.app.domain.model.JournalEntryType
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class JournalRepository(
    private val client: SupabaseClient,
    private val journalEntryDao: JournalEntryDao,
) {
    fun observeEntries(ownerId: String, habitId: String?, type: JournalEntryType?): Flow<List<JournalEntry>> =
        journalEntryDao.observeEntries(ownerId, habitId, type?.wireValue).map { list -> list.map { it.toDomain() } }

    suspend fun upsertEntry(entry: JournalEntry) {
        journalEntryDao.upsert(entry.toEntity(isSynced = false))
        pushEntry(entry)
    }

    suspend fun deleteEntry(id: String) {
        journalEntryDao.markDeleted(id)
        runCatching {
            client.postgrest.from("journal_entries").delete { filter { eq("id", id) } }
        }
    }

    suspend fun refreshFromRemote(ownerId: String) {
        runCatching {
            val remote = client.postgrest.from("journal_entries")
                .select { filter { eq("owner_id", ownerId) } }
                .decodeList<JournalEntryDto>()
            journalEntryDao.upsertAll(remote.map { it.toDomain().toEntity(isSynced = true) })
        }
    }

    suspend fun syncPending() {
        journalEntryDao.getUnsynced().forEach { pushEntry(it.toDomain()) }
    }

    private suspend fun pushEntry(entry: JournalEntry) {
        runCatching {
            client.postgrest.from("journal_entries").upsert(entry.toDto())
            journalEntryDao.markSynced(entry.id)
        }
    }
}
