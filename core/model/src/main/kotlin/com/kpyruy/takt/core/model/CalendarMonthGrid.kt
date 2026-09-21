package com.kpyruy.takt.core.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

object CalendarMonthGrid {
    fun days(month: YearMonth): List<LocalDate> {
        val first = month.atDay(1)
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return (0L until 42L).map(first::plusDays)
    }
}
