package com.kpyruy.takt.core.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.*
import org.junit.Test

class TeachingWeekScheduleTest {
    private val start = LocalDate.of(2026, 9, 14)
    private val period = SemesterPeriod(studyStart = start, studyEnd = start.plusWeeks(12).plusDays(4))
    private fun rule(id: String = "lecture", day: DayOfWeek = DayOfWeek.MONDAY,
        recurrence: ScheduleRecurrence = ScheduleRecurrence.WEEKLY) = ScheduleRule(
        id, "course", "Long course name", day, LocalTime.of(8, 0), LocalTime.of(9, 50), recurrence,
        lessonType = LessonType.LECTURE,
    )

    @Test fun seventhTeachingWeekUsesSavedSemesterStartAndCourseWeekday() {
        val result = TeachingWeekSchedule.resolve(period, "course", listOf(rule()))
        val week = result.weeks.single { it.number == 7 }
        assertEquals(LocalDate.of(2026, 10, 26), week.start)
        assertEquals(listOf(LocalDate.of(2026, 10, 26)), week.occurrences.map { it.date })
    }

    @Test fun partialFirstAndLastWeeksStayInsideSavedStudyPeriod() {
        val partial = SemesterPeriod(studyStart = start.plusDays(2), studyEnd = start.plusWeeks(1).plusDays(1))
        val result = TeachingWeekSchedule.resolve(partial, "course", listOf(rule(), rule("friday", DayOfWeek.FRIDAY)))
        assertEquals(2, result.weeks.size)
        assertEquals(start.plusDays(2), result.weeks[0].start)
        assertEquals(listOf(start.plusDays(4)), result.weeks[0].occurrences.map { it.date })
        assertEquals(start.plusWeeks(1).plusDays(1), result.weeks[1].end)
        assertEquals(listOf(start.plusWeeks(1)), result.weeks[1].occurrences.map { it.date })
    }

    @Test fun teachingWeekNumberDoesNotReplaceIsoParity() {
        val result = TeachingWeekSchedule.resolve(period, "course", listOf(rule(recurrence = ScheduleRecurrence.EVEN_WEEKS)))
        assertEquals(start, result.weeks[0].occurrences.single().date) // ISO week 38, teaching week 1
        assertTrue(result.weeks[1].occurrences.isEmpty())
    }

    @Test fun configuredParityOverrideIsRespectedForEachDate() {
        val result = TeachingWeekSchedule.resolve(period, "course",
            listOf(rule(recurrence = ScheduleRecurrence.ODD_WEEKS)), parityForDate = { WeekParity.ODD })
        assertFalse(result.weeks[0].occurrences.isEmpty())
        assertFalse(result.weeks[1].occurrences.isEmpty())
    }

    @Test fun multipleOccurrencesAreOfferedWithoutCollapsingToFirstLesson() {
        val result = TeachingWeekSchedule.resolve(period, "course", listOf(
            rule(), rule("seminar", DayOfWeek.THURSDAY), rule("other-course").copy(courseId = "other")))
        assertEquals(listOf("lecture", "seminar"), result.weeks[0].occurrences.map { it.id })
    }

    @Test fun cancelledLessonIsExcludedAndMovedLessonUsesActualDate() {
        val result = TeachingWeekSchedule.resolve(period, "course", listOf(rule(), rule("seminar", DayOfWeek.THURSDAY)),
            exceptions = listOf(
                ScheduleException("cancel", "lecture", start, ScheduleExceptionType.CANCELLED),
                ScheduleException("move", "seminar", start.plusDays(3), ScheduleExceptionType.MOVED,
                    replacementDate = start.plusDays(4)),
            ))
        assertEquals(listOf(start.plusDays(4)), result.weeks[0].occurrences.map { it.date })
        assertEquals(ScheduleEventStatus.MOVED, result.weeks[0].occurrences.single().status)
    }

    @Test fun missingOrInvalidPeriodNeverGuessesAWeekFromToday() {
        listOf(null, SemesterPeriod(), SemesterPeriod(studyStart = start),
            SemesterPeriod(studyStart = start, studyEnd = start.minusDays(1))).forEach {
            val result = TeachingWeekSchedule.resolve(it, "course", listOf(rule()))
            assertEquals(TeachingWeekSetupIssue.SEMESTER_PERIOD, result.setupIssue)
            assertTrue(result.weeks.isEmpty())
        }
    }

    @Test fun absentCourseScheduleRequestsSetupInsteadOfInventingOccurrence() {
        val result = TeachingWeekSchedule.resolve(period, "course", listOf(rule().copy(courseId = "other")))
        assertEquals(TeachingWeekSetupIssue.COURSE_SCHEDULE, result.setupIssue)
        assertTrue(result.weeks.isEmpty())
    }

    @Test fun academicWeekAcrossNewYearStillResolvesDates() {
        val crossYear = SemesterPeriod(studyStart = LocalDate.of(2026, 12, 28), studyEnd = LocalDate.of(2027, 1, 8))
        val result = TeachingWeekSchedule.resolve(crossYear, "course", listOf(rule()))
        assertEquals(LocalDate.of(2027, 1, 4), result.weeks[1].occurrences.single().date)
    }
}
