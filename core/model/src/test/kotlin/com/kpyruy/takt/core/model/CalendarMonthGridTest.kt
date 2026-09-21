package com.kpyruy.takt.core.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarMonthGridTest {
    @Test
    fun september2026_gridStartsMondayAndContainsWholeMonth() {
        val days = CalendarMonthGrid.days(YearMonth.of(2026, 9))

        assertEquals(42, days.size)
        assertEquals(DayOfWeek.MONDAY, days.first().dayOfWeek)
        assertEquals(LocalDate.of(2026, 8, 31), days.first())
        assertEquals(LocalDate.of(2026, 10, 11), days.last())
        assertEquals(30, days.count { it.monthValue == 9 })
    }

    @Test
    fun monthGridAlwaysUsesSixWeeks() {
        val days = CalendarMonthGrid.days(YearMonth.of(2026, 2))

        assertEquals(42, days.size)
        assertEquals(DayOfWeek.MONDAY, days.first().dayOfWeek)
        assertEquals(DayOfWeek.SUNDAY, days.last().dayOfWeek)
    }
}
