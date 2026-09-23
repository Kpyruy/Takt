package com.kpyruy.takt.core.model

import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.*
import org.junit.Test

class LessonAbsenceTest {
    private val date = LocalDate.of(2026, 9, 23)
    private val rule = ScheduleRule("r", "c", "Physics", date.dayOfWeek, LocalTime.of(9, 0), LocalTime.of(10, 0), ScheduleRecurrence.WEEKLY)
    private val absence = LessonAbsence("r", date, false)
    @Test fun absenceAffectsOnlyOneOccurrence() {
        assertTrue(resolve(date).single().isAbsent)
        assertFalse(resolve(date.plusWeeks(1)).single().isAbsent)
    }
    @Test fun absenceFollowsMovedOccurrenceWithoutCancellingIt() {
        val moved = ScheduleException("e", "r", date, ScheduleExceptionType.MOVED, date.plusDays(1))
        val event = resolve(date.plusDays(1), listOf(moved)).single()
        assertTrue(event.isAbsent)
        assertEquals(ScheduleEventStatus.MOVED, event.status)
        assertEquals(absence, event.absenceKey())
    }
    @Test fun oneOffAndRecurringIdsCannotCollide() {
        val oneOff = OneOffScheduleEvent("r", "c", "Extra", date, LocalTime.NOON, LocalTime.of(13,0), type = OneOffScheduleEventType.EXTRA)
        val events = ScheduleResolver.eventsForDate(listOf(rule), emptyList(), listOf(oneOff), date, absences = listOf(absence))
        assertTrue(events.first().isAbsent)
        assertFalse(events.last().isAbsent)
    }
    @Test fun removingAbsenceRestoresUnmarkedLesson() {
        assertFalse(ScheduleResolver.eventsForDate(listOf(rule), emptyList(), emptyList(), date).single().isAbsent)
    }
    private fun resolve(day: LocalDate, exceptions: List<ScheduleException> = emptyList()) =
        ScheduleResolver.eventsForDate(listOf(rule), exceptions, emptyList(), day, absences = listOf(absence))
}
