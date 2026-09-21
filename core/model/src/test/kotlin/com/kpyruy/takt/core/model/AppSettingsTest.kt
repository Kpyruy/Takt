package com.kpyruy.takt.core.model

import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class AppSettingsTest {
    private val oddDate = LocalDate.of(2026, 9, 21)

    @Test
    fun automaticParity_usesIsoCalendarWeek() {
        val settings = AppSettings()

        assertEquals(WeekParity.ODD, settings.effectiveParity(oddDate))
    }

    @Test
    fun manualParityOverride_winsOverCalendarWeek() {
        val settings = AppSettings(parityOverride = ParityOverride.EVEN)

        assertEquals(WeekParity.EVEN, settings.effectiveParity(oddDate))
    }

    @Test
    fun hiddenCancellationStyle_removesCancelledUnlessShowHiddenEnabled() {
        val cancelled = ResolvedScheduleEvent(
            id = "x",
            courseId = null,
            title = "Cancelled",
            date = oddDate,
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(9, 0),
            room = null,
            status = ScheduleEventStatus.CANCELLED,
        )
        val normal = cancelled.copy(id = "y", title = "Normal", status = ScheduleEventStatus.NORMAL)

        val hidden = AppSettings(
            cancellationStyle = CancellationDisplayStyle.HIDDEN,
            showHiddenLessons = false,
        )
        val revealed = hidden.copy(showHiddenLessons = true)

        assertEquals(listOf(normal), hidden.filterScheduleEvents(listOf(cancelled, normal)))
        assertEquals(listOf(cancelled, normal), revealed.filterScheduleEvents(listOf(cancelled, normal)))
    }

    @Test
    fun visualPreferencesHaveApprovedDefaults() {
        val settings = AppSettings()

        assertEquals(CardAppearance.ELEVATED, settings.cardAppearance)
        assertEquals(ThemeFamily.BLUE, settings.themeFamily)
        assertEquals(AppThemeMode.SYSTEM, settings.themeMode)
        assertEquals(WeekLayout.TIMETABLE, settings.weekLayout)
    }
}
