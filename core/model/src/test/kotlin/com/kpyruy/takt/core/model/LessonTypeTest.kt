package com.kpyruy.takt.core.model

import java.time.*
import org.junit.Assert.*
import org.junit.Test

class LessonTypeTest {
    private val date = LocalDate.of(2026, 9, 22)
    private val rule = ScheduleRule("r", "c", "Physics", date.dayOfWeek,
        LocalTime.of(9, 0), LocalTime.of(10, 0), ScheduleRecurrence.WEEKLY,
        lessonType = LessonType.SEMINAR)

    @Test fun typeSurvivesNormalCancelledAndMovedOccurrences() {
        assertEquals(LessonType.SEMINAR, ScheduleResolver.eventsForDate(listOf(rule), emptyList(), emptyList(), date).single().lessonType)
        for (kind in ScheduleExceptionType.entries) {
            val exception = ScheduleException("e", "r", date, kind, replacementDate = date.plusDays(1))
            val target = if (kind == ScheduleExceptionType.MOVED) date.plusDays(1) else date
            assertEquals(LessonType.SEMINAR, ScheduleResolver.eventsForDate(listOf(rule), listOf(exception), emptyList(), target).single().lessonType)
        }
    }

    @Test fun oneOffRetainsItsOwnType() {
        val event = OneOffScheduleEvent("o", "c", "Lab", date, LocalTime.NOON, LocalTime.of(14,0),
            type = OneOffScheduleEventType.EXTRA, lessonType = LessonType.LAB)
        assertEquals(LessonType.LAB, ScheduleResolver.eventsForDate(emptyList(), emptyList(), listOf(event), date).single().lessonType)
    }

    @Test fun unknownStoredTypeDoesNotInventLecture() {
        assertEquals(LessonType.UNSPECIFIED, LessonType.fromStorage("unknown"))
    }
}
