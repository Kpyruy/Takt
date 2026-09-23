package com.kpyruy.takt.core.model

import java.time.Duration
import java.time.LocalTime

data class DayTimelineInterval(
    val id: String,
    val start: LocalTime,
    val end: LocalTime,
) {
    init {
        require(end > start) { "end must be after start" }
    }
}

data class DayTimelinePlacement(
    val id: String,
    val offsetMinutes: Long,
    val durationMinutes: Long,
    val lane: Int,
    val laneCount: Int,
)

object DayTimelineLayout {
    fun calculate(
        intervals: List<DayTimelineInterval>,
        rangeStart: LocalTime,
    ): List<DayTimelinePlacement> {
        val ordered = intervals.sortedWith(compareBy<DayTimelineInterval> { it.start }.thenBy { it.end })
        if (ordered.isEmpty()) return emptyList()

        val placements = mutableListOf<DayTimelinePlacement>()
        var clusterStart = 0
        var clusterEnd = ordered.first().end

        fun placeCluster(endExclusive: Int) {
            val laneEnds = mutableListOf<LocalTime>()
            val pending = mutableListOf<DayTimelinePlacement>()
            for (index in clusterStart until endExclusive) {
                val interval = ordered[index]
                val reusableLane = laneEnds.indexOfFirst { it <= interval.start }
                val lane = if (reusableLane >= 0) {
                    laneEnds[reusableLane] = interval.end
                    reusableLane
                } else {
                    laneEnds += interval.end
                    laneEnds.lastIndex
                }
                pending += DayTimelinePlacement(
                    id = interval.id,
                    offsetMinutes = Duration.between(rangeStart, interval.start).toMinutes(),
                    durationMinutes = Duration.between(interval.start, interval.end).toMinutes(),
                    lane = lane,
                    laneCount = 0,
                )
            }
            placements += pending.map { it.copy(laneCount = laneEnds.size) }
        }

        for (index in 1 until ordered.size) {
            val interval = ordered[index]
            if (interval.start >= clusterEnd) {
                placeCluster(index)
                clusterStart = index
                clusterEnd = interval.end
            } else if (interval.end > clusterEnd) {
                clusterEnd = interval.end
            }
        }
        placeCluster(ordered.size)

        return placements
    }
}
