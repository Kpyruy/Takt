package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.UisAutoSyncSettings
import com.kpyruy.takt.core.model.UisRefreshFrequency
import java.time.LocalDate

enum class UisSyncSection { PROGRESS, SUBJECTS, PERIODS, TIMETABLE }

object UisAutoSyncPlanner {
    fun due(
        today: LocalDate,
        settings: UisAutoSyncSettings,
        lastChecked: Map<UisSyncSection, LocalDate>,
        teachingStart: LocalDate?,
        teachingEnd: LocalDate?,
        examEnd: LocalDate? = null,
    ): Set<UisSyncSection> = buildSet {
        val frequencies = mapOf(
            UisSyncSection.PROGRESS to settings.progress,
            UisSyncSection.SUBJECTS to settings.subjects,
            UisSyncSection.PERIODS to settings.periods,
            UisSyncSection.TIMETABLE to settings.timetable,
        )
        frequencies.forEach { (section, frequency) ->
            val last = lastChecked[section]
            val shouldCheck = when (frequency) {
                UisRefreshFrequency.MANUAL -> false
                UisRefreshFrequency.EVERY_ENTRY -> true
                UisRefreshFrequency.DAILY -> last == null || last.plusDays(1) <= today
                UisRefreshFrequency.WEEKLY -> last == null || last.plusWeeks(1) <= today
                UisRefreshFrequency.MONTHLY -> last == null || last.plusMonths(1) <= today
                UisRefreshFrequency.TEACHING_START -> teachingStart != null && teachingStart <= today &&
                    (teachingEnd == null || today <= teachingEnd) && (last == null || last < teachingStart)
            }
            if (shouldCheck) add(section)
        }
        if (settings.timetable == UisRefreshFrequency.TEACHING_START && examEnd != null &&
            today > examEnd && (lastChecked[UisSyncSection.TIMETABLE] == null ||
                lastChecked.getValue(UisSyncSection.TIMETABLE) < examEnd)) {
            add(UisSyncSection.TIMETABLE)
            add(UisSyncSection.PERIODS)
        }
    }
}
