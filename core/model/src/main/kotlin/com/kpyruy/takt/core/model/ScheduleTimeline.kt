package com.kpyruy.takt.core.model

import java.time.Duration
import java.time.LocalTime

data class ScheduleGap(
    val startTime: LocalTime,
    val endTime: LocalTime,
) {
    val minutes: Long
        get() = Duration.between(startTime, endTime).toMinutes()
}

object ScheduleTimeline {
    fun nextEvent(
        events: List<ResolvedScheduleEvent>,
        now: LocalTime,
    ): ResolvedScheduleEvent? =
        events
            .sortedBy { it.startTime }
            .firstOrNull { it.endTime > now }

    fun gaps(events: List<ResolvedScheduleEvent>): List<ScheduleGap> {
        val ordered = events.sortedBy { it.startTime }
        return ordered.zipWithNext().mapNotNull { (current, next) ->
            if (next.startTime > current.endTime) {
                ScheduleGap(current.endTime, next.startTime)
            } else {
                null
            }
        }
    }
}
