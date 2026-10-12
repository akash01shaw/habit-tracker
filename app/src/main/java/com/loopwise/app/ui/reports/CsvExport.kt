package com.loopwise.app.ui.reports

import com.loopwise.app.domain.model.Habit
import com.loopwise.app.domain.model.HabitLog

/**
 * Spec §2.7: "Export button (CSV) for people who want to own their data" — exports
 * full raw log history (not just the Reports screen's computed percentages), since
 * "own their data" reads as data portability, not a summary printout.
 */
fun buildHabitLogsCsv(habits: List<Habit>, logs: List<HabitLog>): String {
    val habitsById = habits.associateBy { it.id }
    val header = "Habit,Type,Date,Value,Unit,Completed,Logged At"
    val rows = logs.map { log ->
        val habit = habitsById[log.habitId]
        listOf(
            csvField(habit?.name ?: "(deleted habit)"),
            csvField(habit?.type?.wireValue.orEmpty()),
            csvField(log.date.toString()),
            csvField(formatCsvNumber(log.value)),
            csvField(habit?.unit.orEmpty()),
            csvField(if (log.isDone) "yes" else "no"),
            csvField(log.loggedAt.toString()),
        ).joinToString(",")
    }
    return (listOf(header) + rows).joinToString("\n")
}

private fun csvField(value: String): String =
    if (value.contains(',') || value.contains('"') || value.contains('\n')) {
        "\"${value.replace("\"", "\"\"")}\""
    } else {
        value
    }

private fun formatCsvNumber(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
