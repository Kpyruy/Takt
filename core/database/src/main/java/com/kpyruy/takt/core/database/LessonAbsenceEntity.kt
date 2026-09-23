package com.kpyruy.takt.core.database

import androidx.room.Entity

@Entity(tableName = "lesson_absences", primaryKeys = ["eventId", "dateEpochDay", "isOneOff"])
data class LessonAbsenceEntity(val eventId: String, val dateEpochDay: Long, val isOneOff: Boolean)
