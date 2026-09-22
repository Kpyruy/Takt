package com.kpyruy.takt.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exam_info")
data class ExamInfoEntity(
    @PrimaryKey val courseId: String,
    val gradeItemId: String?,
    val dateEpochDay: Long?,
    val startMinute: Int?,
    val endMinute: Int?,
    val room: String?,
    val attemptNumber: Int,
    val maxAttempts: Int,
    val notes: String,
)
