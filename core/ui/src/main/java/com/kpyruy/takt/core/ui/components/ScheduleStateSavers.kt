package com.kpyruy.takt.core.ui.components

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import com.kpyruy.takt.core.model.*
import java.time.LocalDate
import java.time.LocalTime

val LocalDateStateSaver = Saver<LocalDate, Long>(save = { it.toEpochDay() }, restore = LocalDate::ofEpochDay)
val LocalTimeStateSaver = Saver<LocalTime, Int>(save = { it.toSecondOfDay() }, restore = { LocalTime.ofSecondOfDay(it.toLong()) })

/** Only scalar event metadata enters saved state; no database snapshots or image bytes. */
val ResolvedEventStateSaver = listSaver<ResolvedScheduleEvent?, String>(
    save = { event -> event?.let { listOf(it.id, it.courseId.orEmpty(), it.title, it.date.toString(),
        it.startTime.toString(), it.endTime.toString(), it.room.orEmpty(), it.status.name,
        it.isAbsent.toString(), it.exceptionId.orEmpty(), it.sourceDate?.toString().orEmpty(), it.lessonType.name) } ?: emptyList() },
    restore = { v -> if (v.isEmpty()) null else ResolvedScheduleEvent(v[0], v[1].ifEmpty { null }, v[2],
        LocalDate.parse(v[3]), LocalTime.parse(v[4]), LocalTime.parse(v[5]), v[6].ifEmpty { null },
        ScheduleEventStatus.valueOf(v[7]), v[8].toBoolean(), v[9].ifEmpty { null },
        v[10].ifEmpty { null }?.let(LocalDate::parse), LessonType.valueOf(v[11])) },
)

val ScheduleRuleStateSaver = listSaver<ScheduleRule?, String>(
    save = { v -> v?.let { listOf(it.id, it.courseId.orEmpty(), it.title, it.dayOfWeek.value.toString(),
        it.startTime.toString(), it.endTime.toString(), it.recurrence.name, it.room.orEmpty(), it.lessonType.name) } ?: emptyList() },
    restore = { v -> if (v.isEmpty()) null else ScheduleRule(v[0], v[1].ifEmpty { null }, v[2],
        java.time.DayOfWeek.of(v[3].toInt()), LocalTime.parse(v[4]), LocalTime.parse(v[5]),
        ScheduleRecurrence.valueOf(v[6]), v[7].ifEmpty { null }, LessonType.valueOf(v[8])) },
)
val OneOffEventStateSaver = listSaver<OneOffScheduleEvent?, String>(
    save = { v -> v?.let { listOf(it.id, it.courseId.orEmpty(), it.title, it.date.toString(),
        it.startTime.toString(), it.endTime.toString(), it.room.orEmpty(), it.type.name, it.lessonType.name) } ?: emptyList() },
    restore = { v -> if (v.isEmpty()) null else OneOffScheduleEvent(v[0], v[1].ifEmpty { null }, v[2],
        LocalDate.parse(v[3]), LocalTime.parse(v[4]), LocalTime.parse(v[5]), v[6].ifEmpty { null },
        OneOffScheduleEventType.valueOf(v[7]), LessonType.valueOf(v[8])) },
)
