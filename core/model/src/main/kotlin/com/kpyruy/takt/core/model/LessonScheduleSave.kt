package com.kpyruy.takt.core.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

sealed interface LessonScheduleSave {
    data class Once(val event: OneOffScheduleEvent) : LessonScheduleSave
    data class Recurring(val rule: ScheduleRule) : LessonScheduleSave
}

/** A single save target: a dated lesson is never also emitted as a recurring rule. */
fun prepareLessonSchedule(
    id: String,
    courseId: String?,
    title: String,
    lessonType: LessonType,
    room: String,
    startTime: LocalTime,
    endTime: LocalTime,
    day: DayOfWeek,
    recurrence: ScheduleRecurrence?,
    date: LocalDate?,
): LessonScheduleSave {
    val normalizedRoom = room.trim().ifBlank { null }
    return if (recurrence == null) LessonScheduleSave.Once(OneOffScheduleEvent(
        id = id, courseId = courseId, title = title.trim(), date = requireNotNull(date),
        startTime = startTime, endTime = endTime, room = normalizedRoom,
        type = OneOffScheduleEventType.BLOCK_ACTION, lessonType = lessonType,
    )) else LessonScheduleSave.Recurring(ScheduleRule(
        id = id, courseId = courseId, title = title.trim(), dayOfWeek = day, startTime = startTime,
        endTime = endTime, recurrence = recurrence, room = normalizedRoom, lessonType = lessonType,
    ))
}
