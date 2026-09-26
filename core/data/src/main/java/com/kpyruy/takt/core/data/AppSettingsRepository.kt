package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.AppSettings
import com.kpyruy.takt.core.model.AppLanguage
import com.kpyruy.takt.core.model.AppThemeMode
import com.kpyruy.takt.core.model.CardAppearance
import com.kpyruy.takt.core.model.CancellationDisplayStyle
import com.kpyruy.takt.core.model.ParityOverride
import com.kpyruy.takt.core.model.ThemeFamily
import com.kpyruy.takt.core.model.WeekLayout
import com.kpyruy.takt.core.model.HomeWorkFilter
import com.kpyruy.takt.core.model.SemesterPeriod
import kotlinx.coroutines.flow.StateFlow

interface AppSettingsRepository {
    val settings: StateFlow<AppSettings>

    suspend fun setCancellationStyle(style: CancellationDisplayStyle)
    suspend fun setShowHiddenLessons(show: Boolean)
    suspend fun setParityOverride(override: ParityOverride)
    suspend fun setCardAppearance(appearance: CardAppearance)
    suspend fun setThemeFamily(themeFamily: ThemeFamily)
    suspend fun setThemeMode(themeMode: AppThemeMode)
    suspend fun setLanguage(language: AppLanguage)
    suspend fun setAppearance(themeMode: AppThemeMode, themeFamily: ThemeFamily, cardAppearance: CardAppearance)
    suspend fun setWeekLayout(layout: WeekLayout)
    suspend fun setHomeWorkFilter(filter: HomeWorkFilter)
    suspend fun setSemesterPeriods(periods: Map<Int, SemesterPeriod>)
    suspend fun setCurrentSemester(semester: Int?)
}
