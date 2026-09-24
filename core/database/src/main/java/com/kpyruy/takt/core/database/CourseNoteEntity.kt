package com.kpyruy.takt.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "course_notes")
data class CourseNoteEntity(
    @PrimaryKey val id: String,
    val courseId: String,
    val title: String,
    val content: String,
    val updatedAtEpochMillis: Long,
    val attachmentsJson: String = "[]",
)
