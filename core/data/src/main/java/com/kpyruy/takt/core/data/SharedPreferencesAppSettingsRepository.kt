package com.kpyruy.takt.core.data

import android.content.Context
import com.kpyruy.takt.core.model.AppSettings
import com.kpyruy.takt.core.model.AppLanguage
import com.kpyruy.takt.core.model.CourseNameLanguage
import com.kpyruy.takt.core.model.UisAutoSyncSettings
import com.kpyruy.takt.core.model.UisRefreshFrequency
import com.kpyruy.takt.core.model.AppThemeMode
import com.kpyruy.takt.core.model.CardAppearance
import com.kpyruy.takt.core.model.CancellationDisplayStyle
import com.kpyruy.takt.core.model.ParityOverride
import com.kpyruy.takt.core.model.ThemeFamily
import com.kpyruy.takt.core.model.WeekLayout
import com.kpyruy.takt.core.model.HomeWorkFilter
import com.kpyruy.takt.core.model.SemesterPeriod
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

    override suspend fun setLanguage(language: AppLanguage) {
        preferences.edit().putString(KEY_LANGUAGE, language.name).apply()
        state.value = state.value.copy(language = language)
    }

    override suspend fun setCourseNameLanguage(language: CourseNameLanguage) {
        preferences.edit().putString(KEY_COURSE_NAME_LANGUAGE, language.name).apply()
        state.value = state.value.copy(courseNameLanguage = language)
    }

    override suspend fun setUkrainianCourseNameFallback(language: CourseNameLanguage) {
        require(language != CourseNameLanguage.FOLLOW_APP)
        preferences.edit().putString(KEY_UKRAINIAN_NAME_FALLBACK, language.name).apply()
        state.value = state.value.copy(ukrainianCourseNameFallback = language)
    }

    override suspend fun setUisAutoSync(settings: UisAutoSyncSettings) {
        require(settings.timetable in setOf(UisRefreshFrequency.MANUAL, UisRefreshFrequency.TEACHING_START))
        preferences.edit()
            .putString(KEY_UIS_PROGRESS_FREQUENCY, settings.progress.name)
            .putString(KEY_UIS_SUBJECT_FREQUENCY, settings.subjects.name)
            .putString(KEY_UIS_PERIOD_FREQUENCY, settings.periods.name)
            .putString(KEY_UIS_TIMETABLE_FREQUENCY, settings.timetable.name)
            .putBoolean(KEY_UIS_APPLY_PROGRESS, settings.applyProgressAutomatically)
            .apply()
        state.value = state.value.copy(uisAutoSync = settings)
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

    override suspend fun setSemesterPeriods(periods: Map<Int, SemesterPeriod>) {
        require(periods.all { (semester, period) -> semester > 0 && period.hasValidDates() })
        val cleaned = periods.filterValues { !it.isEmpty }
        preferences.edit().putString(KEY_SEMESTER_PERIODS, SemesterPeriodsCodec.encode(cleaned)).apply()
        state.value = state.value.copy(semesterPeriods = cleaned)
    }

    override suspend fun setCurrentSemester(semester: Int?) {
        require(semester == null || semester > 0)
        preferences.edit().apply {
            if (semester == null) remove(KEY_CURRENT_SEMESTER) else putInt(KEY_CURRENT_SEMESTER, semester)
        }.apply()
        state.value = state.value.copy(currentSemester = semester)
    }

    override fun setUisCurrentSemester(semester: Int?) {
        require(semester == null || semester > 0)
        preferences.edit().apply {
            if (semester == null) remove(KEY_UIS_CURRENT_SEMESTER)
            else putInt(KEY_UIS_CURRENT_SEMESTER, semester)
        }.apply()
        state.value = state.value.copy(uisCurrentSemester = semester)
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
        semesterPeriods = preferences.getString(KEY_SEMESTER_PERIODS, null),
        currentSemester = preferences.getInt(KEY_CURRENT_SEMESTER, 0).toString(),
        uisCurrentSemester = preferences.getInt(KEY_UIS_CURRENT_SEMESTER, 0).toString(),
        language = preferences.getString(KEY_LANGUAGE, null),
        courseNameLanguage = preferences.getString(KEY_COURSE_NAME_LANGUAGE, null),
        ukrainianCourseNameFallback = preferences.getString(KEY_UKRAINIAN_NAME_FALLBACK, null),
        uisProgressFrequency = preferences.getString(KEY_UIS_PROGRESS_FREQUENCY, null),
        uisSubjectFrequency = preferences.getString(KEY_UIS_SUBJECT_FREQUENCY, null),
        uisPeriodFrequency = preferences.getString(KEY_UIS_PERIOD_FREQUENCY, null),
        uisTimetableFrequency = preferences.getString(KEY_UIS_TIMETABLE_FREQUENCY, null),
        uisApplyProgress = preferences.getBoolean(KEY_UIS_APPLY_PROGRESS, true),
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
        const val KEY_SEMESTER_PERIODS = "semester_periods"
        const val KEY_CURRENT_SEMESTER = "current_semester"
        const val KEY_UIS_CURRENT_SEMESTER = "uis_current_semester"
        const val KEY_LANGUAGE = "language"
        const val KEY_COURSE_NAME_LANGUAGE = "course_name_language"
        const val KEY_UKRAINIAN_NAME_FALLBACK = "ukrainian_course_name_fallback"
        const val KEY_UIS_PROGRESS_FREQUENCY = "uis_progress_frequency"
        const val KEY_UIS_SUBJECT_FREQUENCY = "uis_subject_frequency"
        const val KEY_UIS_PERIOD_FREQUENCY = "uis_period_frequency"
        const val KEY_UIS_TIMETABLE_FREQUENCY = "uis_timetable_frequency"
        const val KEY_UIS_APPLY_PROGRESS = "uis_apply_progress"
    }
}
