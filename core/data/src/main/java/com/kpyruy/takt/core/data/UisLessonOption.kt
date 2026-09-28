package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.LessonType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

data class UisLessonOption(
    val key: String,
    val day: DayOfWeek,
    val start: LocalTime,
    val end: LocalTime,
    val room: String?,
    val lessonType: LessonType,
    val date: LocalDate? = null,
)
