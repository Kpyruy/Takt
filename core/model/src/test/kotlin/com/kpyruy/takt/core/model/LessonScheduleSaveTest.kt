package com.kpyruy.takt.core.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.*
import org.junit.Test

class LessonScheduleSaveTest {
    private val date = LocalDate.of(2026, 11, 19)
    private fun prepare(recurrence: ScheduleRecurrence?, selectedDate: LocalDate? = date) = prepareLessonSchedule(
        id = "existing-id", courseId = "course", title = "  Lab  ", lessonType = LessonType.LAB,
        room = "  207  ", startTime = LocalTime.of(8, 0), endTime = LocalTime.of(9, 50),
        day = DayOfWeek.MONDAY, recurrence = recurrence, date = selectedDate,
    )

    @Test fun onceSavesOnlyOneOffPayloadOnArbitraryDateWithLessonFieldsAndId() {
        val saved = prepare(null) as LessonScheduleSave.Once
        assertEquals("existing-id", saved.event.id)
        assertEquals(date, saved.event.date) // Thursday, independent of recurring weekday
        assertEquals(LessonType.LAB, saved.event.lessonType)
        assertEquals(OneOffScheduleEventType.BLOCK_ACTION, saved.event.type)
        assertEquals("207", saved.event.room)
        assertEquals("Lab", saved.event.title)
        assertEquals(1, ScheduleResolver.eventsForDate(emptyList(), emptyList(), listOf(saved.event), date).size)
        assertTrue(ScheduleResolver.eventsForDate(emptyList(), emptyList(), listOf(saved.event), date.plusWeeks(1)).isEmpty())
    }

    @Test fun everyExistingRecurrenceRemainsARecurringRuleWithStableId() {
        ScheduleRecurrence.entries.forEach { recurrence ->
            val saved = prepare(recurrence) as LessonScheduleSave.Recurring
            assertEquals(recurrence, saved.rule.recurrence)
            assertEquals("existing-id", saved.rule.id)
            assertEquals(DayOfWeek.MONDAY, saved.rule.dayOfWeek)
            assertEquals(LessonType.LAB, saved.rule.lessonType)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun onceWithoutDateCannotSilentlyBecomeWeekly() { prepare(null, null) }
}
