package com.kpyruy.takt.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "grade_overrides")
data class GradeOverrideEntity(
    @PrimaryKey val courseId: String,
    val grade: String,
)
