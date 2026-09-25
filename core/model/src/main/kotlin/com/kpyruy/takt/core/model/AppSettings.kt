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

enum class CardAppearance {
    ELEVATED,
    TONAL_FILLED,
}

enum class ThemeFamily {
    BLUE,
    GREEN,
    PURPLE,
    WARM,
    MONOCHROME,
}

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class WeekLayout {
    TIMETABLE,
    COMPACT_LIST,
}

data class AppSettings(
    val cancellationStyle: CancellationDisplayStyle = CancellationDisplayStyle.STRIKETHROUGH,
    val showHiddenLessons: Boolean = false,
    val parityOverride: ParityOverride = ParityOverride.AUTO,
    val cardAppearance: CardAppearance = CardAppearance.ELEVATED,
    val themeFamily: ThemeFamily = ThemeFamily.BLUE,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val weekLayout: WeekLayout = WeekLayout.TIMETABLE,
    val homeWorkFilter: HomeWorkFilter = HomeWorkFilter(),
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
