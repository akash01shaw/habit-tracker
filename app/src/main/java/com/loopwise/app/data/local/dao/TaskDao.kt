package com.loopwise.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.loopwise.app.data.local.entities.TaskEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE ownerId = :ownerId AND dueDate = :date AND isDeleted = 0 ORDER BY dueTime ASC")
    fun observeTasksForDate(ownerId: String, date: LocalDate): Flow<List<TaskEntity>>

    /** Used to re-arm AlarmManager reminders after a reboot (alarms don't survive one). */
    @Query("SELECT * FROM tasks WHERE ownerId = :ownerId AND isDeleted = 0 AND reminderTime IS NOT NULL AND reminderTime > :after")
    suspend fun getUpcomingReminders(ownerId: String, after: Instant): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE isSynced = 0")
    suspend fun getUnsynced(): List<TaskEntity>

    @Upsert
    suspend fun upsert(task: TaskEntity)

    @Upsert
    suspend fun upsertAll(tasks: List<TaskEntity>)

    @Query("UPDATE tasks SET isDeleted = 1, isSynced = 0 WHERE id = :id")
    suspend fun markDeleted(id: String)

    @Query("UPDATE tasks SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)
}
