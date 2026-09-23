package com.kpyruy.takt.core.model

import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class DayTimelineLayoutTest {
    @Test
    fun `keeps proportional offsets durations and chronological order`() {
        val result = DayTimelineLayout.calculate(
            intervals = listOf(
                DayTimelineInterval("late", LocalTime.of(10, 0), LocalTime.of(11, 30)),
                DayTimelineInterval("early", LocalTime.of(8, 15), LocalTime.of(9, 0)),
            ),
            rangeStart = LocalTime.of(8, 0),
        )

        assertEquals(listOf("early", "late"), result.map { it.id })
        assertEquals(listOf(15L, 120L), result.map { it.offsetMinutes })
        assertEquals(listOf(45L, 90L), result.map { it.durationMinutes })
    }

    @Test
    fun `allocates lanes across a chained overlap cluster`() {
        val result = DayTimelineLayout.calculate(
            intervals = listOf(
                DayTimelineInterval("a", LocalTime.of(9, 0), LocalTime.of(10, 0)),
                DayTimelineInterval("b", LocalTime.of(9, 30), LocalTime.of(10, 30)),
                DayTimelineInterval("c", LocalTime.of(10, 0), LocalTime.of(11, 0)),
            ),
            rangeStart = LocalTime.of(8, 0),
        )

        assertEquals(listOf(0, 1, 0), result.map { it.lane })
        assertEquals(listOf(2, 2, 2), result.map { it.laneCount })
    }

    @Test
    fun `boundary touching intervals share one lane`() {
        val result = DayTimelineLayout.calculate(
            intervals = listOf(
                DayTimelineInterval("a", LocalTime.of(9, 0), LocalTime.of(10, 0)),
                DayTimelineInterval("b", LocalTime.of(10, 0), LocalTime.of(11, 0)),
            ),
            rangeStart = LocalTime.of(8, 0),
        )

        assertEquals(listOf(0, 0), result.map { it.lane })
        assertEquals(listOf(1, 1), result.map { it.laneCount })
    }

    @Test
    fun `empty input returns empty layout`() {
        assertEquals(
            emptyList<DayTimelinePlacement>(),
            DayTimelineLayout.calculate(emptyList(), LocalTime.of(8, 0)),
        )
    }
}
