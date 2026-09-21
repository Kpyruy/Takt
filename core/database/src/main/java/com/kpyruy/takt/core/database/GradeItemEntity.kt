package com.kpyruy.takt.core.database

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
)
