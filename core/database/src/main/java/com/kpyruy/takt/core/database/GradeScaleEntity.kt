package com.kpyruy.takt.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "grade_scales")
data class GradeScaleEntity(
    @PrimaryKey val courseId: String,
    val aMin: Double,
    val bMin: Double,
    val cMin: Double,
    val dMin: Double,
    val eMin: Double,
)
