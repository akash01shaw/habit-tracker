package com.loopwise.app.data.repository

import com.loopwise.app.data.local.dao.HabitDao
import com.loopwise.app.data.local.dao.HabitLogDao
import com.loopwise.app.data.local.entities.toDomain
import com.loopwise.app.data.local.entities.toEntity
import com.loopwise.app.data.remote.dto.HabitDto
import com.loopwise.app.data.remote.dto.toDomain
import com.loopwise.app.data.remote.dto.toDto
import com.loopwise.app.domain.model.Habit
import com.loopwise.app.domain.model.HabitLog
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.UUID

/**
 * Offline-first: every write lands in Room immediately (instant UI feedback via the
 * observe* Flows) and is pushed to Supabase right after. A push failure just leaves
 * the row's isSynced=false; syncPending() retries it next time the app is foregrounded.
 * Full conflict resolution / realtime listeners are Phase 2 (shared habits) territory.
 */
class HabitRepository(
    private val client: SupabaseClient,
    private val habitDao: HabitDao,
    private val habitLogDao: HabitLogDao,
) {
    fun observeHabits(ownerId: String): Flow<List<Habit>> =
        habitDao.observeHabits(ownerId).map { list -> list.map { it.toDomain() } }

    suspend fun getHabit(id: String): Habit? = habitDao.getById(id)?.toDomain()

    fun observeLogsForDate(userId: String, date: LocalDate): Flow<List<HabitLog>> =
        habitLogDao.observeLogsForDate(userId, date).map { list -> list.map { it.toDomain() } }

    /** Latest log per habit — the "since" timestamp for an avoidance habit's streak clock. */
    fun observeLatestLogPerHabit(userId: String): Flow<List<HabitLog>> =
        habitLogDao.observeLatestLogPerHabit(userId).map { list -> list.map { it.toDomain() } }

    fun observeLogsInRange(habitId: String, userId: String, from: LocalDate, to: LocalDate): Flow<List<HabitLog>> =
        habitLogDao.observeLogsInRange(habitId, userId, from, to).map { list -> list.map { it.toDomain() } }

    suspend fun upsertHabit(habit: Habit) {
        habitDao.upsert(habit.toEntity(isSynced = false))
        pushHabit(habit)
    }

    suspend fun deleteHabit(id: String) {
        habitDao.markDeleted(id)
        runCatching {
            client.postgrest.from("habits").delete { filter { eq("id", id) } }
        }
    }

    /** Increments/sets today's progress for a habit and pushes the resulting log row. */
    suspend fun logProgress(habitId: String, userId: String, date: LocalDate, value: Double, isDone: Boolean) {
        val existing = habitLogDao.getLog(habitId, userId, date)
        val log = HabitLog(
            id = existing?.id ?: UUID.randomUUID().toString(),
            habitId = habitId,
            userId = userId,
            date = date,
            value = value,
            isDone = isDone,
        )
        habitLogDao.upsert(log.toEntity(isSynced = false))
        pushLog(log)
    }

    suspend fun refreshFromRemote(ownerId: String) {
        runCatching {
            val remote = client.postgrest.from("habits")
                .select { filter { eq("owner_id", ownerId) } }
                .decodeList<HabitDto>()
            habitDao.upsertAll(remote.map { it.toDomain().toEntity(isSynced = true) })
        }
    }

    suspend fun syncPending() {
        habitDao.getUnsynced().forEach { pushHabit(it.toDomain()) }
        habitLogDao.getUnsynced().forEach { pushLog(it.toDomain()) }
    }

    private suspend fun pushHabit(habit: Habit) {
        runCatching {
            client.postgrest.from("habits").upsert(habit.toDto())
            habitDao.markSynced(habit.id)
        }
    }

    private suspend fun pushLog(log: HabitLog) {
        runCatching {
            client.postgrest.from("habit_logs").upsert(log.toDto()) {
                onConflict = "habit_id,user_id,date"
            }
            habitLogDao.markSynced(log.id)
        }
    }
}
