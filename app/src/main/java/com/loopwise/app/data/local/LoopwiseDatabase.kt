package com.loopwise.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.loopwise.app.data.local.dao.HabitDao
import com.loopwise.app.data.local.dao.HabitLogDao
import com.loopwise.app.data.local.dao.JournalEntryDao
import com.loopwise.app.data.local.dao.TaskDao
import com.loopwise.app.data.local.dao.TimerSessionDao
import com.loopwise.app.data.local.entities.HabitEntity
import com.loopwise.app.data.local.entities.HabitLogEntity
import com.loopwise.app.data.local.entities.JournalEntryEntity
import com.loopwise.app.data.local.entities.TaskEntity
import com.loopwise.app.data.local.entities.TimerSessionEntity

@Database(
    entities = [
        HabitEntity::class,
        TaskEntity::class,
        HabitLogEntity::class,
        JournalEntryEntity::class,
        TimerSessionEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class LoopwiseDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun taskDao(): TaskDao
    abstract fun habitLogDao(): HabitLogDao
    abstract fun journalEntryDao(): JournalEntryDao
    abstract fun timerSessionDao(): TimerSessionDao

    companion object {
        const val NAME = "loopwise.db"
    }
}
