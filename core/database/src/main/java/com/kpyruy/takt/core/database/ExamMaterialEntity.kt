package com.kpyruy.takt.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exam_materials")
data class ExamMaterialEntity(
    @PrimaryKey val id: String,
    val courseId: String,
    val title: String,
    val uri: String,
)
