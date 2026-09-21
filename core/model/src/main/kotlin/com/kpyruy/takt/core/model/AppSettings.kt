package com.kpyruy.takt.core.model

import java.time.LocalDate
import java.time.temporal.WeekFields

enum class CancellationDisplayStyle {
    STRIKETHROUGH,
    HIDDEN,
    MARKED,
}

enum class ParityOverride {
    AUTO,
    EVEN,
    ODD,
}

data class AppSettings(
    val cancellationStyle: CancellationDisplayStyle = CancellationDisplayStyle.STRIKETHROUGH,
    val showHiddenLessons: Boolean = false,
    val parityOverride: ParityOverride = ParityOverride.AUTO,
) {
    fun effectiveParity(date: LocalDate): WeekParity = when (parityOverride) {
        ParityOverride.AUTO -> WeekParity.fromIsoWeek(
            date.get(WeekFields.ISO.weekOfWeekBasedYear())
        )
        ParityOverride.EVEN -> WeekParity.EVEN
        ParityOverride.ODD -> WeekParity.ODD
    }

    fun filterScheduleEvents(events: List<ResolvedScheduleEvent>): List<ResolvedScheduleEvent> {
        if (cancellationStyle != CancellationDisplayStyle.HIDDEN || showHiddenLessons) {
            return events
        }
        return events.filter { it.status != ScheduleEventStatus.CANCELLED }
    }
}
