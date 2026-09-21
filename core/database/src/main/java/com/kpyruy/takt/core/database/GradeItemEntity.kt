package com.kpyruy.takt.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "grade_items")
data class GradeItemEntity(
    @PrimaryKey val id: String,
    val courseId: String,
    val title: String,
    val type: String,
    val earnedPoints: Double,
    val maxPoints: Double,
    val recordedAtEpochMillis: Long,
    val dueDateEpochDay: Long? = null,
    @ColumnInfo(defaultValue = "1")
    val completed: Boolean = true,
    @ColumnInfo(defaultValue = "0")
    val requiredForExam: Boolean = false,
)
