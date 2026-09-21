package com.kpyruy.takt.core.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.WeekFields

enum class ScheduleRecurrence {
    WEEKLY,
    EVEN_WEEKS,
    ODD_WEEKS,
}

data class ScheduleRule(
    val id: String,
    val courseId: String?,
    val title: String,
    val dayOfWeek: DayOfWeek,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val recurrence: ScheduleRecurrence,
    val room: String? = null,
) {
    init {
        require(endTime > startTime) { "endTime must be after startTime" }
    }

    fun occursOn(date: LocalDate, parityOverride: WeekParity? = null): Boolean {
        if (date.dayOfWeek != dayOfWeek) return false

        val parity = parityOverride ?: WeekParity.fromIsoWeek(
            date.get(WeekFields.ISO.weekOfWeekBasedYear())
        )

        return when (recurrence) {
            ScheduleRecurrence.WEEKLY -> true
            ScheduleRecurrence.EVEN_WEEKS -> parity == WeekParity.EVEN
            ScheduleRecurrence.ODD_WEEKS -> parity == WeekParity.ODD
        }
    }
}
