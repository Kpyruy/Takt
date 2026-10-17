package com.kpyruy.takt.core.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

enum class TeachingWeekSetupIssue { SEMESTER_PERIOD, COURSE_SCHEDULE }

data class TeachingWeek(
    val number: Int,
    val start: LocalDate,
    val end: LocalDate,
    val occurrences: List<ResolvedScheduleEvent>,
)

data class TeachingWeekOptions(
    val weeks: List<TeachingWeek> = emptyList(),
    val setupIssue: TeachingWeekSetupIssue? = null,
)

/** Week 1 is the calendar week containing the saved study start, clipped to study dates.
 * Academic numbering never replaces the schedule's ISO/configured parity. */
object TeachingWeekSchedule {
    fun resolve(
        period: SemesterPeriod?,
        courseId: String,
        rules: List<ScheduleRule>,
        exceptions: List<ScheduleException> = emptyList(),
        parityForDate: (LocalDate) -> WeekParity? = { null },
    ): TeachingWeekOptions {
        val start = period?.studyStart
        val end = period?.studyEnd
        if (start == null || end == null || end < start) {
            return TeachingWeekOptions(setupIssue = TeachingWeekSetupIssue.SEMESTER_PERIOD)
        }
        val courseRules = rules.filter { it.courseId == courseId }
        if (courseRules.isEmpty()) return TeachingWeekOptions(setupIssue = TeachingWeekSetupIssue.COURSE_SCHEDULE)
        val firstMonday = start.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val count = (ChronoUnit.DAYS.between(firstMonday, end) / 7 + 1).toInt()
        val weeks = (0 until count).map { index ->
            val monday = firstMonday.plusWeeks(index.toLong())
            val weekStart = maxOf(start, monday)
            val weekEnd = minOf(end, monday.plusDays(6))
            val occurrences = (0..ChronoUnit.DAYS.between(weekStart, weekEnd)).flatMap { offset ->
                val date = weekStart.plusDays(offset)
                ScheduleResolver.eventsForDate(courseRules, exceptions, emptyList(), date, parityForDate(date),
                    ruleAllowed = { _, originalDate -> originalDate in start..end })
                    .filter { it.status != ScheduleEventStatus.CANCELLED }
            }.sortedWith(compareBy<ResolvedScheduleEvent> { it.date }.thenBy { it.startTime }.thenBy { it.id })
            TeachingWeek(index + 1, weekStart, weekEnd, occurrences)
        }
        return TeachingWeekOptions(weeks)
    }
}
