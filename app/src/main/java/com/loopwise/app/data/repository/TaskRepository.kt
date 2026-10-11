package com.loopwise.app.data.repository

import com.loopwise.app.data.local.dao.TaskDao
import com.loopwise.app.data.local.entities.toDomain
import com.loopwise.app.data.local.entities.toEntity
import com.loopwise.app.data.remote.dto.TaskDto
import com.loopwise.app.data.remote.dto.toDomain
import com.loopwise.app.data.remote.dto.toDto
import com.loopwise.app.domain.model.Task
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate

class TaskRepository(
    private val client: SupabaseClient,
    private val taskDao: TaskDao,
) {
    fun observeTasksForDate(ownerId: String, date: LocalDate): Flow<List<Task>> =
        taskDao.observeTasksForDate(ownerId, date).map { list -> list.map { it.toDomain() } }

    suspend fun getUpcomingReminders(ownerId: String): List<Task> =
        taskDao.getUpcomingReminders(ownerId, Instant.now()).map { it.toDomain() }

    suspend fun upsertTask(task: Task) {
        taskDao.upsert(task.toEntity(isSynced = false))
        pushTask(task)
    }

    suspend fun setCompleted(task: Task, completed: Boolean) {
        upsertTask(task.copy(completedAt = if (completed) Instant.now() else null))
    }

    suspend fun deleteTask(id: String) {
        taskDao.markDeleted(id)
        runCatching {
            client.postgrest.from("tasks").delete { filter { eq("id", id) } }
        }
    }

    suspend fun refreshFromRemote(ownerId: String, date: LocalDate) {
        runCatching {
            val remote = client.postgrest.from("tasks")
                .select { filter { eq("owner_id", ownerId); eq("due_date", date.toString()) } }
                .decodeList<TaskDto>()
            taskDao.upsertAll(remote.map { it.toDomain().toEntity(isSynced = true) })
        }
    }

    suspend fun syncPending() {
        taskDao.getUnsynced().forEach { pushTask(it.toDomain()) }
    }

    private suspend fun pushTask(task: Task) {
        runCatching {
            client.postgrest.from("tasks").upsert(task.toDto())
            taskDao.markSynced(task.id)
        }
    }
}
