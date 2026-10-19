package com.loopwise.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.loopwise.app.data.local.entities.HabitLogEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface HabitLogDao {
    @Query("SELECT * FROM habit_logs WHERE userId = :userId AND date = :date")
    fun observeLogsForDate(userId: String, date: LocalDate): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND userId = :userId AND date BETWEEN :from AND :to")
    fun observeLogsInRange(habitId: String, userId: String, from: LocalDate, to: LocalDate): Flow<List<HabitLogEntity>>

    /** Same range query but across every habit — used to compute each habit's current streak in one shot. */
    @Query("SELECT * FROM habit_logs WHERE userId = :userId AND date BETWEEN :from AND :to")
    fun observeLogsInRangeForUser(userId: String, from: LocalDate, to: LocalDate): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND userId = :userId AND date = :date LIMIT 1")
    suspend fun getLog(habitId: String, userId: String, date: LocalDate): HabitLogEntity?

    @Query("SELECT * FROM habit_logs WHERE isSynced = 0")
    suspend fun getUnsynced(): List<HabitLogEntity>

    /** One-shot full history for CSV export — not reactive, this is a single user action. */
    @Query("SELECT * FROM habit_logs WHERE userId = :userId ORDER BY date ASC, loggedAt ASC")
    suspend fun getAllForUser(userId: String): List<HabitLogEntity>

    /**
     * The most recent log row per habit for this user — used as the "since" timestamp
     * for an avoidance habit's live streak clock (the latest log's loggedAt is the last
     * reset/slip time; logProgress()'s upsert-by-date naturally advances it on each slip).
     */
    @Query(
        """
        SELECT hl.* FROM habit_logs hl
        INNER JOIN (
            SELECT habitId, MAX(loggedAt) AS maxLoggedAt
            FROM habit_logs
            WHERE userId = :userId
            GROUP BY habitId
        ) latest ON hl.habitId = latest.habitId AND hl.loggedAt = latest.maxLoggedAt
        WHERE hl.userId = :userId
        """,
    )
    fun observeLatestLogPerHabit(userId: String): Flow<List<HabitLogEntity>>

    @Upsert
    suspend fun upsert(log: HabitLogEntity)

    @Upsert
    suspend fun upsertAll(logs: List<HabitLogEntity>)

    @Query("UPDATE habit_logs SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)
}
