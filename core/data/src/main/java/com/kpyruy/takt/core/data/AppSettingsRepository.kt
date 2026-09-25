package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.AppSettings
import com.kpyruy.takt.core.model.AppThemeMode
import com.kpyruy.takt.core.model.CardAppearance
import com.kpyruy.takt.core.model.CancellationDisplayStyle
import com.kpyruy.takt.core.model.ParityOverride
import com.kpyruy.takt.core.model.ThemeFamily
import com.kpyruy.takt.core.model.WeekLayout
import com.kpyruy.takt.core.model.HomeWorkFilter
import kotlinx.coroutines.flow.Flow

interface AppSettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun setCancellationStyle(style: CancellationDisplayStyle)
    suspend fun setShowHiddenLessons(show: Boolean)
    suspend fun setParityOverride(override: ParityOverride)
    suspend fun setCardAppearance(appearance: CardAppearance)
    suspend fun setThemeFamily(themeFamily: ThemeFamily)
    suspend fun setThemeMode(themeMode: AppThemeMode)
    suspend fun setWeekLayout(layout: WeekLayout)
    suspend fun setHomeWorkFilter(filter: HomeWorkFilter)
}
