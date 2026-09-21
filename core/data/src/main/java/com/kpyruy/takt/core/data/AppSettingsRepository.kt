package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.AppSettings
import com.kpyruy.takt.core.model.CancellationDisplayStyle
import com.kpyruy.takt.core.model.ParityOverride
import kotlinx.coroutines.flow.Flow

interface AppSettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun setCancellationStyle(style: CancellationDisplayStyle)
    suspend fun setShowHiddenLessons(show: Boolean)
    suspend fun setParityOverride(override: ParityOverride)
}
