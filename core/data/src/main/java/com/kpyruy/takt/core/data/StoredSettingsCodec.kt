package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.AppSettings
import com.kpyruy.takt.core.model.AppThemeMode
import com.kpyruy.takt.core.model.CardAppearance
import com.kpyruy.takt.core.model.CancellationDisplayStyle
import com.kpyruy.takt.core.model.ParityOverride
import com.kpyruy.takt.core.model.ThemeFamily
import com.kpyruy.takt.core.model.WeekLayout

object StoredSettingsCodec {
    fun decode(
        cancellationStyle: String?,
        showHiddenLessons: Boolean,
        parityOverride: String?,
        cardAppearance: String? = null,
        themeFamily: String? = null,
        themeMode: String? = null,
        weekLayout: String? = null,
        homeWorkFilter: String? = null,
        semesterPeriods: String? = null,
    ): AppSettings {
        val default = AppSettings()
        return AppSettings(
            cancellationStyle = enumValueOrNull<CancellationDisplayStyle>(cancellationStyle)
                ?: default.cancellationStyle,
            showHiddenLessons = showHiddenLessons,
            parityOverride = enumValueOrNull<ParityOverride>(parityOverride)
                ?: default.parityOverride,
            cardAppearance = enumValueOrNull<CardAppearance>(cardAppearance)
                ?: default.cardAppearance,
            themeFamily = enumValueOrNull<ThemeFamily>(themeFamily)
                ?: default.themeFamily,
            themeMode = enumValueOrNull<AppThemeMode>(themeMode)
                ?: default.themeMode,
            weekLayout = enumValueOrNull<WeekLayout>(weekLayout)
                ?: default.weekLayout,
            homeWorkFilter = HomeWorkFilterCodec.decode(homeWorkFilter),
            semesterPeriods = SemesterPeriodsCodec.decode(semesterPeriods),
        )
    }

    private inline fun <reified T : Enum<T>> enumValueOrNull(value: String?): T? =
        value?.let { stored -> enumValues<T>().firstOrNull { it.name == stored } }
}
