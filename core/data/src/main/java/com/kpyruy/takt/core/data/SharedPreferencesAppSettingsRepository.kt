package com.kpyruy.takt.core.data

import android.content.Context
import com.kpyruy.takt.core.model.AppSettings
import com.kpyruy.takt.core.model.CancellationDisplayStyle
import com.kpyruy.takt.core.model.ParityOverride
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SharedPreferencesAppSettingsRepository(
    context: Context,
) : AppSettingsRepository {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE,
    )

    private val state = MutableStateFlow(read())
    override val settings = state.asStateFlow()

    override suspend fun setCancellationStyle(style: CancellationDisplayStyle) {
        preferences.edit().putString(KEY_CANCELLATION_STYLE, style.name).apply()
        state.value = state.value.copy(cancellationStyle = style)
    }

    override suspend fun setShowHiddenLessons(show: Boolean) {
        preferences.edit().putBoolean(KEY_SHOW_HIDDEN, show).apply()
        state.value = state.value.copy(showHiddenLessons = show)
    }

    override suspend fun setParityOverride(override: ParityOverride) {
        preferences.edit().putString(KEY_PARITY_OVERRIDE, override.name).apply()
        state.value = state.value.copy(parityOverride = override)
    }

    private fun read(): AppSettings = StoredSettingsCodec.decode(
        cancellationStyle = preferences.getString(KEY_CANCELLATION_STYLE, null),
        showHiddenLessons = preferences.getBoolean(KEY_SHOW_HIDDEN, false),
        parityOverride = preferences.getString(KEY_PARITY_OVERRIDE, null),
    )

    private companion object {
        const val PREFS_NAME = "takt_settings"
        const val KEY_CANCELLATION_STYLE = "cancellation_style"
        const val KEY_SHOW_HIDDEN = "show_hidden_lessons"
        const val KEY_PARITY_OVERRIDE = "parity_override"
    }
}
