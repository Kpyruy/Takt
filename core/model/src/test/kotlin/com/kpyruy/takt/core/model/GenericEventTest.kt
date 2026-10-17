package com.kpyruy.takt.core.model

import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.*
import org.junit.Test

class GenericEventTest {
    private val date = LocalDate.of(2026, 11, 19)

    @Test fun storedEventNameAndLegacyNamesRemainDistinctWithUnknownFallback() {
        assertEquals(LessonType.EVENT, LessonType.fromStorage("EVENT"))
        for (name in listOf("UNSPECIFIED", "LECTURE", "SEMINAR", "PRACTICE", "LAB")) {
            assertEquals(name, LessonType.fromStorage(name).name)
        }
        assertEquals(LessonType.UNSPECIFIED, LessonType.fromStorage("future-type"))
    }

    @Test fun unlinkedTournamentResolvesOnlyOnItsDateAndKeepsItsCategory() {
        val event = OneOffScheduleEvent("chess", null, "Chess tournament", date,
            LocalTime.of(16, 0), LocalTime.of(18, 0), "Club",
            OneOffScheduleEventType.EXTRA, LessonType.EVENT)
        val resolved = ScheduleResolver.eventsForDate(emptyList(), emptyList(), listOf(event), date).single()
        assertEquals(event.title, resolved.title)
        assertNull(resolved.courseId)
        assertEquals(date, resolved.date)
        assertEquals(LessonType.EVENT, resolved.lessonType)
        assertEquals(event.room, resolved.room)
        assertTrue(ScheduleResolver.eventsForDate(emptyList(), emptyList(), listOf(event), date.plusDays(1)).isEmpty())
    }

    @Test fun recurringEventUsesExistingRecurrenceAndOnceConversionKeepsCategoryAndId() {
        fun prepare(recurrence: ScheduleRecurrence?) = prepareLessonSchedule(
            id = "chess", courseId = null, title = "  Chess club  ", lessonType = LessonType.EVENT,
            room = "  Club  ", startTime = LocalTime.of(16, 0), endTime = LocalTime.of(18, 0),
            day = date.dayOfWeek, recurrence = recurrence, date = date)
        val recurring = (prepare(ScheduleRecurrence.WEEKLY) as LessonScheduleSave.Recurring).rule
        assertEquals(LessonType.EVENT, recurring.lessonType)
        assertNull(recurring.courseId)
        assertTrue(recurring.occursOn(date.plusWeeks(1)))
        val once = (prepare(null) as LessonScheduleSave.Once).event
        assertEquals(recurring.id, once.id)
        assertEquals(recurring.title, once.title)
        assertEquals(LessonType.EVENT, once.lessonType)
        assertEquals(date, once.date)
    }
}
