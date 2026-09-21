package com.kpyruy.takt.core.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ScheduleResolverTest {
    private val date = LocalDate.of(2026, 9, 25)

    private val physics = ScheduleRule(
        id = "physics-lecture",
        courseId = "FYZI_6B",
        title = "Fyzika",
        dayOfWeek = DayOfWeek.FRIDAY,
        startTime = LocalTime.of(8, 0),
        endTime = LocalTime.of(9, 50),
        recurrence = ScheduleRecurrence.WEEKLY,
        room = "T-aula",
    )

    @Test
    fun cancellation_keepsOccurrenceButMarksItCancelled() {
        val result = ScheduleResolver.eventsForDate(
            rules = listOf(physics),
            exceptions = listOf(
                ScheduleException(
                    id = "cancel-physics",
                    ruleId = physics.id,
                    date = date,
                    type = ScheduleExceptionType.CANCELLED,
                )
            ),
            oneOffEvents = emptyList(),
            date = date,
        )

        assertEquals(1, result.size)
        assertEquals(ScheduleEventStatus.CANCELLED, result.single().status)
        assertEquals(LocalTime.of(8, 0), result.single().startTime)
    }

    @Test
    fun movedOccurrence_usesReplacementTimeAndRoom() {
        val result = ScheduleResolver.eventsForDate(
            rules = listOf(physics),
            exceptions = listOf(
                ScheduleException(
                    id = "move-physics",
                    ruleId = physics.id,
                    date = date,
                    type = ScheduleExceptionType.MOVED,
                    replacementStartTime = LocalTime.of(12, 0),
                    replacementEndTime = LocalTime.of(13, 50),
                    replacementRoom = "T-068",
                )
            ),
            oneOffEvents = emptyList(),
            date = date,
        )

        val event = result.single()
        assertEquals(ScheduleEventStatus.MOVED, event.status)
        assertEquals(LocalTime.of(12, 0), event.startTime)
        assertEquals(LocalTime.of(13, 50), event.endTime)
        assertEquals("T-068", event.room)
    }

    @Test
    fun movedOccurrence_toAnotherDate_disappearsFromOriginalAndAppearsOnReplacementDate() {
        val movedDate = date.plusDays(1)
        val exception = ScheduleException(
            id = "move-physics-next-day",
            ruleId = physics.id,
            date = date,
            type = ScheduleExceptionType.MOVED,
            replacementDate = movedDate,
            replacementStartTime = LocalTime.of(12, 0),
            replacementEndTime = LocalTime.of(13, 50),
            replacementRoom = "T-068",
        )

        val originalDay = ScheduleResolver.eventsForDate(
            rules = listOf(physics),
            exceptions = listOf(exception),
            oneOffEvents = emptyList(),
            date = date,
        )
        val movedDay = ScheduleResolver.eventsForDate(
            rules = listOf(physics),
            exceptions = listOf(exception),
            oneOffEvents = emptyList(),
            date = movedDate,
        )

        assertEquals(0, originalDay.size)
        assertEquals(1, movedDay.size)
        assertEquals(ScheduleEventStatus.MOVED, movedDay.single().status)
        assertEquals(movedDate, movedDay.single().date)
        assertEquals(LocalTime.of(12, 0), movedDay.single().startTime)
    }

    @Test
    fun blockAction_isAddedOnItsConcreteDate() {
        val block = OneOffScheduleEvent(
            id = "vr-block",
            courseId = "DIVR_6B",
            title = "Digitalizácia a virtuálna realita",
            date = date,
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(12, 50),
            room = "T-aula",
            type = OneOffScheduleEventType.BLOCK_ACTION,
        )

        val result = ScheduleResolver.eventsForDate(
            rules = listOf(physics),
            exceptions = emptyList(),
            oneOffEvents = listOf(block),
            date = date,
        )

        assertEquals(2, result.size)
        assertEquals(
            listOf("Digitalizácia a virtuálna realita", "Fyzika"),
            result.map { it.title },
        )
        assertEquals(ScheduleEventStatus.ONE_OFF, result.first().status)
    }
}
