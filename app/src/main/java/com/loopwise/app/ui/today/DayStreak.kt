package com.loopwise.app.ui.today

import com.loopwise.app.domain.model.HabitLog
import com.loopwise.app.domain.model.Task
import java.time.LocalDate
import java.time.ZoneId

/**
 * The one headline streak for the whole app (Duolingo-style single number, not per-habit):
 * consecutive days with at least one completed habit OR task. Deliberately a low bar —
 * completing one thing keeps it alive, same as Duolingo counting "did one lesson" rather
 * than "did everything." A strict all-or-nothing bar is how streaks get abandoned after one
 * bad day; a generous one is what keeps people coming back daily.
 *
 * Today not having a completion yet doesn't break the streak (the day isn't over) — it just
 * means today isn't counted yet. The streak only actually breaks once a full day passes with
 * nothing done.
 */
fun computeDayStreak(
    habitLogs: List<HabitLog>,
    completedTasks: List<Task>,
    today: LocalDate,
    lookbackDays: Int = 400,
): Int {
    val activeDates = buildSet {
        habitLogs.forEach { if (it.isDone) add(it.date) }
        completedTasks.forEach { task ->
            task.completedAt?.let { add(it.atZone(ZoneId.systemDefault()).toLocalDate()) }
        }
    }

    var date = if (today in activeDates) today else today.minusDays(1)
    var streak = 0
    repeat(lookbackDays) {
        if (date !in activeDates) return streak
        streak++
        date = date.minusDays(1)
    }
    return streak
}
