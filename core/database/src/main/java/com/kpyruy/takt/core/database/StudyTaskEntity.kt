package com.kpyruy.takt.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_tasks")
data class StudyTaskEntity(
    @PrimaryKey val id: String,
    val courseId: String,
    val title: String,
    val description: String?,
    val dueDateEpochDay: Long?,
    val completed: Boolean,
    @ColumnInfo(defaultValue = "0")
    val requiredForExam: Boolean = false,
    val earnedPoints: Double? = null,
    val maxPoints: Double? = null,
    val minimumPointsForExam: Double? = null,
)
