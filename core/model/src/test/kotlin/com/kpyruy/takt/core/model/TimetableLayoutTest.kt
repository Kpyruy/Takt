package com.kpyruy.takt.core.model

import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.*
import org.junit.Test

class TimetableLayoutTest {
    private fun event(id: String, start: String, end: String) = ResolvedScheduleEvent(
        id, "course", id, LocalDate.of(2026, 10, 13), LocalTime.parse(start), LocalTime.parse(end),
        null, ScheduleEventStatus.NORMAL,
    )

    @Test fun overlapsUseSeparateLanesButLaterIndependentClassGetsFullWidth() {
        val result = TimetableLayout.lanes(listOf(event("a", "08:00", "10:00"),
            event("b", "09:00", "11:00"), event("c", "12:00", "14:00")))
        assertEquals(listOf(2, 2, 1), result.map { it.laneCount })
        assertNotEquals(result[0].lane, result[1].lane)
    }

    @Test fun chainOverlapKeepsLaneCountConsistentAcrossConnectedCluster() {
        val result = TimetableLayout.lanes(listOf(event("a", "08:00", "09:30"),
            event("b", "09:00", "10:30"), event("c", "10:00", "11:00")))
        assertEquals(listOf(2, 2, 2), result.map { it.laneCount })
        assertEquals(result[0].lane, result[2].lane)
    }

    @Test fun adjacentClassesDoNotOverlap() {
        val result = TimetableLayout.lanes(listOf(event("a", "08:00", "09:00"), event("b", "09:00", "10:00")))
        assertEquals(listOf(1, 1), result.map { it.laneCount })
    }

    @Test fun shortLessonsIncreaseTimeScaleRatherThanOverlapNextClass() {
        val events = listOf(event("a", "08:00", "08:15"), event("b", "08:15", "08:30"))
        val hourHeight = TimetableLayout.hourHeight(events, fontScale = 1.5f)
        val firstHeight = TimetableLayout.durationMinutes(events[0]) / 60f * hourHeight
        val secondTop = 15f / 60f * hourHeight
        assertEquals(firstHeight, secondTop, 0.001f)
        assertTrue(firstHeight >= 128f * 1.5f)
    }
}
