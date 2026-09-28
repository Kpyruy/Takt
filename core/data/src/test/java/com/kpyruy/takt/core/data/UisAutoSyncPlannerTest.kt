package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.UisAutoSyncSettings
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class UisAutoSyncPlannerTest {
    @Test fun progressAndSubjectsAreDueOnEveryEntryAndTimetableOncePerTeachingPeriod() {
        val start = LocalDate.of(2026, 9, 21)
        val today = LocalDate.of(2026, 9, 28)
        val settings = UisAutoSyncSettings()
        assertEquals(setOf(UisSyncSection.PROGRESS, UisSyncSection.SUBJECTS, UisSyncSection.TIMETABLE),
            UisAutoSyncPlanner.due(today, settings, emptyMap(), start, LocalDate.of(2026, 12, 12)))
        assertEquals(setOf(UisSyncSection.PROGRESS, UisSyncSection.SUBJECTS), UisAutoSyncPlanner.due(today, settings,
            mapOf(UisSyncSection.PROGRESS to today, UisSyncSection.TIMETABLE to start),
            start, LocalDate.of(2026, 12, 12)))
        assertEquals(setOf(UisSyncSection.PROGRESS, UisSyncSection.SUBJECTS), UisAutoSyncPlanner.due(today.plusWeeks(1), settings,
            mapOf(UisSyncSection.PROGRESS to today, UisSyncSection.TIMETABLE to start),
            start, LocalDate.of(2026, 12, 12)))
    }

    @Test fun checksNextTimetableOnceAfterPreviousExamsFinish() {
        val start = LocalDate.of(2026, 9, 21)
        val examEnd = LocalDate.of(2027, 2, 13)
        val checked = mapOf(UisSyncSection.TIMETABLE to start,
            UisSyncSection.PROGRESS to LocalDate.of(2027, 2, 14))
        assertEquals(setOf(UisSyncSection.PROGRESS, UisSyncSection.SUBJECTS, UisSyncSection.TIMETABLE, UisSyncSection.PERIODS),
            UisAutoSyncPlanner.due(LocalDate.of(2027, 2, 14), UisAutoSyncSettings(), checked,
                start, LocalDate.of(2026, 12, 12), examEnd))
        assertEquals(setOf(UisSyncSection.PROGRESS, UisSyncSection.SUBJECTS),
            UisAutoSyncPlanner.due(LocalDate.of(2027, 2, 14), UisAutoSyncSettings(),
                checked + (UisSyncSection.TIMETABLE to LocalDate.of(2027, 2, 14)),
                start, LocalDate.of(2026, 12, 12), examEnd))
    }
}
