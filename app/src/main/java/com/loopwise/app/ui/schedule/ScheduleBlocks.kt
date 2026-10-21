package com.loopwise.app.ui.schedule

import com.loopwise.app.domain.model.Habit
import com.loopwise.app.domain.model.Task
import com.loopwise.app.ui.today.isScheduledOn
import java.time.LocalDate
import java.time.LocalTime

enum class ScheduleBlockKind { HABIT, TASK }

data class ScheduleBlock(
    val key: String,
    val date: LocalDate,
    val time: LocalTime,
    val label: String,
    val color: Long,
    val kind: ScheduleBlockKind,
    val sourceId: String,
)

/**
 * Spec §2.5: the week grid is auto-populated from habits/tasks that have a time
 * window — a habit with no reminder set, or a task with no due time, has nothing to
 * place on the grid, so both are skipped rather than guessed at.
 */
fun buildScheduleBlocks(habits: List<Habit>, tasks: List<Task>, weekDates: List<LocalDate>): List<ScheduleBlock> {
    val habitBlocks = weekDates.flatMap { date ->
        habits
            .filter { it.isScheduledOn(date) }
            .flatMap { habit -> habit.reminders.map { time -> habit to time } }
            .map { (habit, time) ->
                ScheduleBlock(
                    key = "habit:${habit.id}:$date:$time",
                    date = date,
                    time = time,
                    label = habit.name,
                    color = habit.color,
                    kind = ScheduleBlockKind.HABIT,
                    sourceId = habit.id,
                )
            }
    }

    val taskBlocks = tasks.mapNotNull { task ->
        val dueDate = task.dueDate ?: return@mapNotNull null
        val dueTime = task.dueTime ?: return@mapNotNull null
        if (dueDate !in weekDates) return@mapNotNull null
        ScheduleBlock(
            key = "task:${task.id}",
            date = dueDate,
            time = dueTime,
            label = task.title,
            color = task.color,
            kind = ScheduleBlockKind.TASK,
            sourceId = task.id,
        )
    }

    return (habitBlocks + taskBlocks).sortedWith(compareBy({ it.date }, { it.time }))
}
