package com.kpyruy.takt.core.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleRulesTest {
    private val mondayOdd = LocalDate.of(2026, 9, 21)
    private val mondayEven = LocalDate.of(2026, 9, 28)

    @Test
    fun weeklyRule_occursOnMatchingWeekdayEveryWeek() {
        val rule = ScheduleRule(
            id = "stats",
            courseId = "ZAST_6B",
            title = "Základy štatistiky",
            dayOfWeek = DayOfWeek.MONDAY,
            startTime = LocalTime.of(15, 0),
            endTime = LocalTime.of(16, 50),
            recurrence = ScheduleRecurrence.WEEKLY,
        )

        assertTrue(rule.occursOn(mondayOdd))
        assertTrue(rule.occursOn(mondayEven))
        assertFalse(rule.occursOn(mondayOdd.plusDays(1)))
    }

    @Test
    fun oddWeekRule_onlyOccursInOddIsoWeeks() {
        val rule = ScheduleRule(
            id = "tpar",
            courseId = "TPAR_6B",
            title = "TPAR cvičenie",
            dayOfWeek = DayOfWeek.MONDAY,
            startTime = LocalTime.of(13, 0),
            endTime = LocalTime.of(14, 50),
            recurrence = ScheduleRecurrence.ODD_WEEKS,
        )

        assertTrue(rule.occursOn(mondayOdd))
        assertFalse(rule.occursOn(mondayEven))
    }

    @Test
    fun evenWeekRule_onlyOccursInEvenIsoWeeks() {
        val rule = ScheduleRule(
            id = "tpar-even",
            courseId = "TPAR_6B",
            title = "TPAR cvičenie",
            dayOfWeek = DayOfWeek.MONDAY,
            startTime = LocalTime.of(15, 0),
            endTime = LocalTime.of(16, 50),
            recurrence = ScheduleRecurrence.EVEN_WEEKS,
        )

        assertFalse(rule.occursOn(mondayOdd))
        assertTrue(rule.occursOn(mondayEven))
    }

    @Test
    fun manualParityOverride_changesParityDecisionWithoutChangingCalendarDate() {
        val rule = ScheduleRule(
            id = "override",
            courseId = "TPAR_6B",
            title = "TPAR",
            dayOfWeek = DayOfWeek.MONDAY,
            startTime = LocalTime.of(13, 0),
            endTime = LocalTime.of(14, 50),
            recurrence = ScheduleRecurrence.EVEN_WEEKS,
        )

        assertFalse(rule.occursOn(mondayOdd))
        assertTrue(rule.occursOn(mondayOdd, WeekParity.EVEN))
    }
}
