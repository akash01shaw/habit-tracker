package com.loopwise.app.data.repository

import com.loopwise.app.data.local.dao.TimerSessionDao
import com.loopwise.app.data.local.entities.toDomain
import com.loopwise.app.data.local.entities.toEntity
import com.loopwise.app.data.remote.dto.toDto
import com.loopwise.app.domain.model.TimerSession
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TimerRepository(
    private val client: SupabaseClient,
    private val timerSessionDao: TimerSessionDao,
) {
    fun observeRecentSessions(ownerId: String): Flow<List<TimerSession>> =
        timerSessionDao.observeRecentSessions(ownerId).map { list -> list.map { it.toDomain() } }

    /** Called on every start/pause/resume/stop so an in-progress session survives a process death. */
    suspend fun upsertSession(session: TimerSession) {
        timerSessionDao.upsert(session.toEntity(isSynced = false))
        pushSession(session)
    }

    suspend fun syncPending() {
        timerSessionDao.getUnsynced().forEach { pushSession(it.toDomain()) }
    }

    private suspend fun pushSession(session: TimerSession) {
        runCatching {
            client.postgrest.from("timer_sessions").upsert(session.toDto())
            timerSessionDao.markSynced(session.id)
        }
    }
}
