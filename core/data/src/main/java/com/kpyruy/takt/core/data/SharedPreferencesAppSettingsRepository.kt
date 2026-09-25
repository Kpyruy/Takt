package com.kpyruy.takt.core.data

import android.content.Context
import com.kpyruy.takt.core.model.AppSettings
import com.kpyruy.takt.core.model.AppThemeMode
import com.kpyruy.takt.core.model.CardAppearance
import com.kpyruy.takt.core.model.CancellationDisplayStyle
import com.kpyruy.takt.core.model.ParityOverride
import com.kpyruy.takt.core.model.ThemeFamily
import com.kpyruy.takt.core.model.WeekLayout
import com.kpyruy.takt.core.model.HomeWorkFilter
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

    override suspend fun setCardAppearance(appearance: CardAppearance) {
        preferences.edit().putString(KEY_CARD_APPEARANCE, appearance.name).apply()
        state.value = state.value.copy(cardAppearance = appearance)
    }

    override suspend fun setThemeFamily(themeFamily: ThemeFamily) {
        preferences.edit().putString(KEY_THEME_FAMILY, themeFamily.name).apply()
        state.value = state.value.copy(themeFamily = themeFamily)
    }

    override suspend fun setThemeMode(themeMode: AppThemeMode) {
        preferences.edit().putString(KEY_THEME_MODE, themeMode.name).apply()
        state.value = state.value.copy(themeMode = themeMode)
    }

    override suspend fun setAppearance(
        themeMode: AppThemeMode,
        themeFamily: ThemeFamily,
        cardAppearance: CardAppearance,
    ) {
        preferences.edit()
            .putString(KEY_THEME_MODE, themeMode.name)
            .putString(KEY_THEME_FAMILY, themeFamily.name)
            .putString(KEY_CARD_APPEARANCE, cardAppearance.name)
            .apply()
        state.value = state.value.copy(
            themeMode = themeMode,
            themeFamily = themeFamily,
            cardAppearance = cardAppearance,
        )
    }

    override suspend fun setWeekLayout(layout: WeekLayout) {
        preferences.edit().putString(KEY_WEEK_LAYOUT, layout.name).apply()
        state.value = state.value.copy(weekLayout = layout)
    }

    override suspend fun setHomeWorkFilter(filter: HomeWorkFilter) {
        preferences.edit().putString(KEY_HOME_WORK_FILTER, HomeWorkFilterCodec.encode(filter)).apply()
        state.value = state.value.copy(homeWorkFilter = filter)
    }

    private fun read(): AppSettings = StoredSettingsCodec.decode(
        cancellationStyle = preferences.getString(KEY_CANCELLATION_STYLE, null),
        showHiddenLessons = preferences.getBoolean(KEY_SHOW_HIDDEN, false),
        parityOverride = preferences.getString(KEY_PARITY_OVERRIDE, null),
        cardAppearance = preferences.getString(KEY_CARD_APPEARANCE, null),
        themeFamily = preferences.getString(KEY_THEME_FAMILY, null),
        themeMode = preferences.getString(KEY_THEME_MODE, null),
        weekLayout = preferences.getString(KEY_WEEK_LAYOUT, null),
        homeWorkFilter = preferences.getString(KEY_HOME_WORK_FILTER, null),
    )

    private companion object {
        const val PREFS_NAME = "takt_settings"
        const val KEY_CANCELLATION_STYLE = "cancellation_style"
        const val KEY_SHOW_HIDDEN = "show_hidden_lessons"
        const val KEY_PARITY_OVERRIDE = "parity_override"
        const val KEY_CARD_APPEARANCE = "card_appearance"
        const val KEY_THEME_FAMILY = "theme_family"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_WEEK_LAYOUT = "week_layout"
        const val KEY_HOME_WORK_FILTER = "home_work_filter"
    }
}
