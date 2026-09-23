package com.kpyruy.takt.app

import java.time.LocalDate
import java.time.LocalTime

data class CreateItemDraft(
    val type: CreateItemType,
    val lessonType: com.kpyruy.takt.core.model.LessonType = com.kpyruy.takt.core.model.LessonType.UNSPECIFIED,
    val courseId: String? = null,
    val title: String = "",
    val details: String = "",
    val dueDate: LocalDate? = null,
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
)
