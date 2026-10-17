package com.kpyruy.takt.core.model

import java.time.Duration

data class TimetableLaneEvent(val event: ResolvedScheduleEvent, val lane: Int, val laneCount: Int)

/** Connected overlap groups share lanes; independent later lessons retain their full column. */
object TimetableLayout {
    fun lanes(events: List<ResolvedScheduleEvent>): List<TimetableLaneEvent> {
        val placements = DayTimelineLayout.calculate(events.mapIndexed { index, event ->
            DayTimelineInterval(index.toString(), event.startTime, event.endTime)
        }, java.time.LocalTime.MIN)
        return placements.map { placement ->
            TimetableLaneEvent(events[placement.id.toInt()], placement.lane, placement.laneCount)
        }
    }

    fun durationMinutes(event: ResolvedScheduleEvent): Long = Duration.between(event.startTime, event.endTime).toMinutes()

    /** Expand the temporal scale for short lessons instead of enlarging cards over neighbouring times. */
    fun hourHeight(events: List<ResolvedScheduleEvent>, fontScale: Float): Float {
        val scale = fontScale.coerceAtLeast(1f)
        val shortest = events.minOfOrNull { durationMinutes(it).coerceAtLeast(1) } ?: 60
        return maxOf(96f * scale, 164f * scale * 60f / shortest)
    }
}
