package com.loopwise.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.loopwise.app.data.local.dao.HabitDao
import com.loopwise.app.data.local.dao.HabitLogDao
import com.loopwise.app.data.local.dao.JournalEntryDao
import com.loopwise.app.data.local.dao.TaskDao
import com.loopwise.app.data.local.entities.HabitEntity
import com.loopwise.app.data.local.entities.HabitLogEntity
import com.loopwise.app.data.local.entities.JournalEntryEntity
import com.loopwise.app.data.local.entities.TaskEntity

@Database(
    entities = [HabitEntity::class, TaskEntity::class, HabitLogEntity::class, JournalEntryEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class LoopwiseDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun taskDao(): TaskDao
    abstract fun habitLogDao(): HabitLogDao
    abstract fun journalEntryDao(): JournalEntryDao

    companion object {
        const val NAME = "loopwise.db"
    }
}
