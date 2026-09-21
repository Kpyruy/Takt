package com.kpyruy.takt.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CourseEntity::class,
        ScheduleRuleEntity::class,
        OneOffScheduleEventEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class TaktDatabase : RoomDatabase() {
    abstract fun courseDao(): CourseDao
    abstract fun scheduleDao(): ScheduleDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS schedule_rules (
                        id TEXT NOT NULL PRIMARY KEY,
                        courseId TEXT,
                        title TEXT NOT NULL,
                        dayOfWeek INTEGER NOT NULL,
                        startMinute INTEGER NOT NULL,
                        endMinute INTEGER NOT NULL,
                        recurrence TEXT NOT NULL,
                        room TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS schedule_one_off (
                        id TEXT NOT NULL PRIMARY KEY,
                        courseId TEXT,
                        title TEXT NOT NULL,
                        dateEpochDay INTEGER NOT NULL,
                        startMinute INTEGER NOT NULL,
                        endMinute INTEGER NOT NULL,
                        room TEXT,
                        type TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
