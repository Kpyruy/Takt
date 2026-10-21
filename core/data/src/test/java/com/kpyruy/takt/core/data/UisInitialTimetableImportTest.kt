package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.OneOffScheduleEvent
import com.kpyruy.takt.core.model.OneOffScheduleEventType
import com.kpyruy.takt.core.model.ScheduleRecurrence
import com.kpyruy.takt.core.model.ScheduleRule
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class UisInitialTimetableImportTest {
    private val lesson = ScheduleRule("uis:linked", "uis:study:subject", "Algebra", DayOfWeek.MONDAY,
        LocalTime.of(9, 0), LocalTime.of(10, 0), ScheduleRecurrence.WEEKLY)
    private val unmatched = lesson.copy(id = "uis:outside-plan", courseId = null, title = "Open seminar")
    private val event = OneOffScheduleEvent("uis:one-off", null, "University event",
        LocalDate.of(2026, 10, 22), LocalTime.of(12, 0), LocalTime.of(13, 0),
        type = OneOffScheduleEventType.BLOCK_ACTION)

    @Test fun firstConnectionImportsEntireTimetableEvenWhenOneSubjectIsAbsentFromThePlan() = runBlocking {
        var savedRules = emptyList<ScheduleRule>()
        var savedEvents = emptyList<OneOffScheduleEvent>()
        var writes = 0
        assertTrue(importInitialUisTimetable(true, emptyList(), emptyList(), listOf(lesson, unmatched), listOf(event)) { rules, events ->
            writes++
            savedRules = rules
            savedEvents = events
        })
        assertEquals(1, writes)
        assertEquals(listOf(lesson, unmatched), savedRules)
        assertEquals(listOf(event), savedEvents)
        assertEquals("Open seminar", savedRules.last().title)
        assertNull(savedRules.last().courseId)
    }

    @Test fun initialConnectionPreservesExistingRecurringOrOneOffPersonalSchedule() = runBlocking {
        for ((localRules, localEvents) in listOf(
            listOf(lesson.copy(id = "personal")) to emptyList(),
            emptyList<ScheduleRule>() to listOf(event.copy(id = "personal-event")),
        )) {
            var writes = 0
            assertFalse(importInitialUisTimetable(true, localRules, localEvents, listOf(unmatched), listOf(event)) { _, _ -> writes++ })
            assertEquals(0, writes)
        }
    }

    @Test fun laterManualChecksKeepTimetableChangesForReviewEvenWhenLocalScheduleIsEmpty() = runBlocking {
        var writes = 0
        assertFalse(importInitialUisTimetable(false, emptyList(), emptyList(), listOf(lesson), listOf(event)) { _, _ -> writes++ })
        assertEquals(0, writes)
    }
}
