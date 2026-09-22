package com.kpyruy.takt.core.model

import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScheduleTimelineTest {
    private val date = LocalDate.of(2026, 9, 21)

    private fun event(
        id: String,
        start: LocalTime,
        end: LocalTime,
    ) = ResolvedScheduleEvent(
        id = id,
        courseId = null,
        title = id,
        date = date,
        startTime = start,
        endTime = end,
        room = null,
        status = ScheduleEventStatus.NORMAL,
    )

    @Test
    fun nextEventReturnsFirstEventThatHasNotEnded() {
        val events = listOf(
            event("a", LocalTime.of(8, 0), LocalTime.of(9, 0)),
            event("b", LocalTime.of(10, 0), LocalTime.of(11, 0)),
        )

        assertEquals(
            "b",
            ScheduleTimeline.nextEvent(events, LocalTime.of(9, 30))?.id,
        )
    }

    @Test
    fun nextEventReturnsNullAfterLastEvent() {
        val events = listOf(event("a", LocalTime.of(8, 0), LocalTime.of(9, 0)))

        assertNull(ScheduleTimeline.nextEvent(events, LocalTime.of(10, 0)))
    }

    @Test
    fun gapsReturnsOnlyPositiveFreeTime() {
        val events = listOf(
            event("a", LocalTime.of(8, 0), LocalTime.of(9, 0)),
            event("b", LocalTime.of(9, 30), LocalTime.of(10, 30)),
            event("c", LocalTime.of(10, 15), LocalTime.of(11, 0)),
        )

        assertEquals(
            listOf(ScheduleGap(LocalTime.of(9, 0), LocalTime.of(9, 30))),
            ScheduleTimeline.gaps(events),
        )
    }
}
