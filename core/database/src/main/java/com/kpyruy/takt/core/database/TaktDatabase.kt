package com.kpyruy.takt.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [CourseEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class TaktDatabase : RoomDatabase() {
    abstract fun courseDao(): CourseDao
}
