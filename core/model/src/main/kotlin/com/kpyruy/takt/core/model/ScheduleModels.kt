package com.kpyruy.takt.core.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.WeekFields

enum class LessonType(val label: String, val shortLabel: String) {
    UNSPECIFIED("Не вказано", ""),
    LECTURE("Лекція", "Лек."),
    SEMINAR("Семінар", "Сем."),
    PRACTICE("Практика", "Практ."),
    LAB("Лабораторна", "Лаб.");

    companion object {
        fun fromStorage(value: String): LessonType = entries.firstOrNull { it.name == value } ?: UNSPECIFIED
    }
}

enum class ScheduleRecurrence {
    WEEKLY,
    EVEN_WEEKS,
    ODD_WEEKS,
}

data class ScheduleRule(
    val id: String,
    val courseId: String?,
    val title: String,
    val dayOfWeek: DayOfWeek,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val recurrence: ScheduleRecurrence,
    val room: String? = null,
    val lessonType: LessonType = LessonType.UNSPECIFIED,
) {
    init {
        require(endTime > startTime) { "endTime must be after startTime" }
    }

    fun occursOn(date: LocalDate, parityOverride: WeekParity? = null): Boolean {
        if (date.dayOfWeek != dayOfWeek) return false

        val parity = parityOverride ?: WeekParity.fromIsoWeek(
            date.get(WeekFields.ISO.weekOfWeekBasedYear())
        )

        return when (recurrence) {
            ScheduleRecurrence.WEEKLY -> true
            ScheduleRecurrence.EVEN_WEEKS -> parity == WeekParity.EVEN
            ScheduleRecurrence.ODD_WEEKS -> parity == WeekParity.ODD
        }
    }
}

enum class ScheduleExceptionType {
    CANCELLED,
    MOVED,
}

data class ScheduleException(
    val id: String,
    val ruleId: String,
    val date: LocalDate,
    val type: ScheduleExceptionType,
    val replacementDate: LocalDate? = null,
    val replacementStartTime: LocalTime? = null,
    val replacementEndTime: LocalTime? = null,
    val replacementRoom: String? = null,
)

enum class OneOffScheduleEventType {
    BLOCK_ACTION,
    EXTRA,
    REMINDER,
}

data class OneOffScheduleEvent(
    val id: String,
    val courseId: String?,
    val title: String,
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val room: String? = null,
    val type: OneOffScheduleEventType,
    val lessonType: LessonType = LessonType.UNSPECIFIED,
) {
    init {
        require(endTime > startTime) { "endTime must be after startTime" }
    }
}

enum class ScheduleEventStatus {
    NORMAL,
    CANCELLED,
    MOVED,
    ONE_OFF,
}

/** Original occurrence date keeps attendance stable when a recurring lesson is moved. */
data class LessonAbsence(val eventId: String, val date: LocalDate, val isOneOff: Boolean)

fun ResolvedScheduleEvent.absenceKey() = LessonAbsence(id, sourceDate ?: date, status == ScheduleEventStatus.ONE_OFF)

data class ResolvedScheduleEvent(
    val id: String,
    val courseId: String?,
    val title: String,
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val room: String?,
    val status: ScheduleEventStatus,
    val isAbsent: Boolean = false,
    val exceptionId: String? = null,
    val sourceDate: LocalDate? = null,
    val lessonType: LessonType = LessonType.UNSPECIFIED,
)

fun ResolvedScheduleEvent.isVisuallyMuted(at: LocalDateTime): Boolean =
    isAbsent || status == ScheduleEventStatus.CANCELLED ||
        date.isBefore(at.toLocalDate()) ||
        (date == at.toLocalDate() && !endTime.isAfter(at.toLocalTime()))

object ScheduleResolver {
    fun eventsForDate(
        rules: List<ScheduleRule>,
        exceptions: List<ScheduleException>,
        oneOffEvents: List<OneOffScheduleEvent>,
        date: LocalDate,
        parityOverride: WeekParity? = null,
        absences: List<LessonAbsence> = emptyList(),
        ruleAllowed: (ScheduleRule, LocalDate) -> Boolean = { _, _ -> true },
    ): List<ResolvedScheduleEvent> {
        val recurring = rules
            .filter { it.occursOn(date, parityOverride) && ruleAllowed(it, date) }
            .mapNotNull { rule ->
                val exception = exceptions.firstOrNull {
                    it.ruleId == rule.id && it.date == date
                }

                when (exception?.type) {
                    ScheduleExceptionType.CANCELLED -> rule.toResolved(
                        date = date,
                        status = ScheduleEventStatus.CANCELLED,
                        exceptionId = exception.id,
                        sourceDate = exception.date,
                    )

                    ScheduleExceptionType.MOVED -> {
                        val targetDate = exception.replacementDate ?: date
                        if (targetDate != date) {
                            null
                        } else {
                            ResolvedScheduleEvent(
                                id = rule.id,
                                courseId = rule.courseId,
                                title = rule.title,
                                lessonType = rule.lessonType,
                                date = date,
                                startTime = exception.replacementStartTime ?: rule.startTime,
                                endTime = exception.replacementEndTime ?: rule.endTime,
                                room = exception.replacementRoom ?: rule.room,
                                status = ScheduleEventStatus.MOVED,
                                exceptionId = exception.id,
                                sourceDate = exception.date,
                            )
                        }
                    }

                    null -> rule.toResolved(
                        date = date,
                        status = ScheduleEventStatus.NORMAL,
                    )
                }
            }

        val movedIntoDate = exceptions
            .filter {
                it.type == ScheduleExceptionType.MOVED &&
                    it.replacementDate == date &&
                    it.date != date
            }
            .mapNotNull { exception ->
                val rule = rules.firstOrNull { it.id == exception.ruleId } ?: return@mapNotNull null
                if (!ruleAllowed(rule, exception.date)) return@mapNotNull null
                ResolvedScheduleEvent(
                    id = rule.id,
                    courseId = rule.courseId,
                    title = rule.title,
                    lessonType = rule.lessonType,
                    date = date,
                    startTime = exception.replacementStartTime ?: rule.startTime,
                    endTime = exception.replacementEndTime ?: rule.endTime,
                    room = exception.replacementRoom ?: rule.room,
                    status = ScheduleEventStatus.MOVED,
                    exceptionId = exception.id,
                    sourceDate = exception.date,
                )
            }

        val oneOff = oneOffEvents
            .filter { it.date == date }
            .map {
                ResolvedScheduleEvent(
                    id = it.id,
                    courseId = it.courseId,
                    title = it.title,
                    lessonType = it.lessonType,
                    date = it.date,
                    startTime = it.startTime,
                    endTime = it.endTime,
                    room = it.room,
                    status = ScheduleEventStatus.ONE_OFF,
                )
            }

        val missed = absences.toHashSet()
        return (recurring + movedIntoDate + oneOff).map { it.copy(isAbsent = it.absenceKey() in missed) }.sortedWith(
            compareBy<ResolvedScheduleEvent> { it.startTime }.thenBy { it.title }
        )
    }
}

private fun ScheduleRule.toResolved(
    date: LocalDate,
    status: ScheduleEventStatus,
    exceptionId: String? = null,
    sourceDate: LocalDate? = null,
) = ResolvedScheduleEvent(
    id = id,
    courseId = courseId,
    title = title,
    date = date,
    startTime = startTime,
    endTime = endTime,
    room = room,
    status = status,
    exceptionId = exceptionId,
    sourceDate = sourceDate,
    lessonType = lessonType,
)
