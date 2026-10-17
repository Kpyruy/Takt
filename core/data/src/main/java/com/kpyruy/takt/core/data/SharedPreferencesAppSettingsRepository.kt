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
    private val preferencesName: String = PREFS_NAME,
) : AppSettingsRepository, BackupSettingsParticipant {
    private val app = context.applicationContext
    private val writer = Any()
    private var restoring = false
    private var recoveryBlocked = false
    private var pendingUis = false
    private var pendingUisValue: Int? = null
    override val restoreJournalDirectory get() = java.io.File(app.filesDir, "backup-restore")
    override val restoreNamespace get() = preferencesName
    private val preferences = context.applicationContext.getSharedPreferences(
        preferencesName,
        Context.MODE_PRIVATE,
    )

    private val state = MutableStateFlow(read())
    override val settings = state.asStateFlow()
    private suspend fun change(edit: android.content.SharedPreferences.Editor.() -> Unit,
        transform: (AppSettings) -> AppSettings) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        synchronized(writer) {
            check(!restoring && !recoveryBlocked) { "Backup restore is in progress" }
            val updated = transform(state.value)
            check(preferences.edit().apply(edit).commit()) { "Could not save settings" }
            state.value = updated
        }
    }

    override suspend fun setReminders(reminders: com.kpyruy.takt.core.model.ReminderSettings) = updateReminders { reminders }
    override suspend fun updateReminders(transform: (com.kpyruy.takt.core.model.ReminderSettings) -> com.kpyruy.takt.core.model.ReminderSettings) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { synchronized(writer) {
            check(!restoring && !recoveryBlocked) { "Backup restore is in progress" }
            val clean = transform(state.value.reminders).normalized()
            check(preferences.edit().putString(KEY_REMINDERS, ReminderSettingsCodec.encode(clean)).commit())
            state.value = state.value.copy(reminders = clean)
        } }
    }
    override suspend fun setCancellationStyle(style: CancellationDisplayStyle) =
        change({ putString(KEY_CANCELLATION_STYLE, style.name) }) { it.copy(cancellationStyle = style) }
    override suspend fun setShowHiddenLessons(show: Boolean) =
        change({ putBoolean(KEY_SHOW_HIDDEN, show) }) { it.copy(showHiddenLessons = show) }
    override suspend fun setParityOverride(override: ParityOverride) =
        change({ putString(KEY_PARITY_OVERRIDE, override.name) }) { it.copy(parityOverride = override) }
    override suspend fun setCardAppearance(appearance: CardAppearance) =
        change({ putString(KEY_CARD_APPEARANCE, appearance.name) }) { it.copy(cardAppearance = appearance) }
    override suspend fun setThemeFamily(themeFamily: ThemeFamily) =
        change({ putString(KEY_THEME_FAMILY, themeFamily.name); remove(KEY_SELECTED_CUSTOM_THEME) }) {
            it.copy(themeFamily = themeFamily, selectedCustomThemeId = null)
        }
    override suspend fun setThemeMode(themeMode: AppThemeMode) =
        change({ putString(KEY_THEME_MODE, themeMode.name) }) { it.copy(themeMode = themeMode) }
    override suspend fun setLanguage(language: AppLanguage) =
        change({ putString(KEY_LANGUAGE, language.name) }) { it.copy(language = language) }
    override suspend fun setCourseNameLanguage(language: CourseNameLanguage) =
        change({ putString(KEY_COURSE_NAME_LANGUAGE, language.name) }) { it.copy(courseNameLanguage = language) }
    override suspend fun setUkrainianCourseNameFallback(language: CourseNameLanguage) {
        require(language != CourseNameLanguage.FOLLOW_APP)
        change({ putString(KEY_UKRAINIAN_NAME_FALLBACK, language.name) }) { it.copy(ukrainianCourseNameFallback = language) }
    }
    override suspend fun setUisAutoSync(settings: UisAutoSyncSettings) {
        require(settings.timetable in setOf(UisRefreshFrequency.MANUAL, UisRefreshFrequency.TEACHING_START))
        change({
            putString(KEY_UIS_PROGRESS_FREQUENCY, settings.progress.name)
            putString(KEY_UIS_SUBJECT_FREQUENCY, settings.subjects.name)
            putString(KEY_UIS_PERIOD_FREQUENCY, settings.periods.name)
            putString(KEY_UIS_TIMETABLE_FREQUENCY, settings.timetable.name)
            putBoolean(KEY_UIS_APPLY_PROGRESS, settings.applyProgressAutomatically)
        }) { it.copy(uisAutoSync = settings) }
    }
    override suspend fun setAppearance(themeMode: AppThemeMode, themeFamily: ThemeFamily, cardAppearance: CardAppearance) =
        setAppearance(state.value.copy(themeMode = themeMode, themeFamily = themeFamily,
            cardAppearance = cardAppearance, selectedCustomThemeId = null))
    override suspend fun setAppearance(appearance: AppSettings) {
        val clean = appearance.normalizedAppearance()
        change({
            putString(KEY_THEME_MODE, clean.themeMode.name); putString(KEY_THEME_FAMILY, clean.themeFamily.name)
            putString(KEY_CARD_APPEARANCE, clean.cardAppearance.name)
            putString(KEY_CUSTOM_THEMES, CustomThemesCodec.encode(clean.customThemes))
            putString(KEY_SELECTED_CUSTOM_THEME, clean.selectedCustomThemeId); putString(KEY_CUSTOM_WALLPAPER, clean.customWallpaper)
        }) { it.copy(themeMode = clean.themeMode, themeFamily = clean.themeFamily, cardAppearance = clean.cardAppearance,
            customThemes = clean.customThemes, selectedCustomThemeId = clean.selectedCustomThemeId, customWallpaper = clean.customWallpaper) }
    }
    override suspend fun setWeekLayout(layout: WeekLayout) =
        change({ putString(KEY_WEEK_LAYOUT, layout.name) }) { it.copy(weekLayout = layout) }
    override suspend fun setHomeWorkFilter(filter: HomeWorkFilter) =
        change({ putString(KEY_HOME_WORK_FILTER, HomeWorkFilterCodec.encode(filter)) }) { it.copy(homeWorkFilter = filter) }
    override suspend fun setSemesterPeriods(periods: Map<Int, SemesterPeriod>) {
        require(periods.all { (semester, period) -> semester > 0 && period.hasValidDates() })
        val clean = periods.filterValues { !it.isEmpty }
        change({ putString(KEY_SEMESTER_PERIODS, SemesterPeriodsCodec.encode(clean)) }) { it.copy(semesterPeriods = clean) }
    }
    override suspend fun setCurrentSemester(semester: Int?) {
        require(semester == null || semester > 0)
        change({ if (semester == null) remove(KEY_CURRENT_SEMESTER) else putInt(KEY_CURRENT_SEMESTER, semester) }) {
            it.copy(currentSemester = semester)
        }
    }
    override fun setUisCurrentSemester(semester: Int?) = synchronized(writer) {
        require(semester == null || semester > 0)
        if (restoring || recoveryBlocked) { pendingUis = true; pendingUisValue = semester; return@synchronized }
        preferences.edit().apply {
            if (semester == null) remove(KEY_UIS_CURRENT_SEMESTER) else putInt(KEY_UIS_CURRENT_SEMESTER, semester)
        }.apply()
        state.value = state.value.copy(uisCurrentSemester = semester)
    }

    override fun beginBackupRestore(): BackupSettingsLease = synchronized(writer) {
        check(!restoring) { "Backup restore is already in progress" }
        restoring = true
        object : BackupSettingsLease {
            override val before = state.value
            override fun persist(settings: AppSettings) = synchronized(writer) { writeAll(settings) }
            override fun publishAndRelease() = synchronized(writer) {
                var loaded = read()
                if (pendingUis) {
                    val queued = pendingUisValue
                    check(preferences.edit().apply {
                        if (queued == null) remove(KEY_UIS_CURRENT_SEMESTER) else putInt(KEY_UIS_CURRENT_SEMESTER, queued)
                    }.commit())
                    loaded = loaded.copy(uisCurrentSemester = queued)
                    pendingUis = false
                }
                state.value = loaded
                restoring = false
                recoveryBlocked = false
            }
            override fun releaseWithoutPublishing() = synchronized(writer) { restoring = false; recoveryBlocked = true }
        }
    }

    /** One durable preference commit; notification/UI collectors are published only after Room's decision. */
    private fun writeAll(value: AppSettings) {
        val clean = value.normalizedAppearance()
        check(preferences.edit().apply {
            putString(KEY_CANCELLATION_STYLE, clean.cancellationStyle.name); putBoolean(KEY_SHOW_HIDDEN, clean.showHiddenLessons)
            putString(KEY_PARITY_OVERRIDE, clean.parityOverride.name); putString(KEY_CARD_APPEARANCE, clean.cardAppearance.name)
            putString(KEY_THEME_FAMILY, clean.themeFamily.name); putString(KEY_THEME_MODE, clean.themeMode.name)
            putString(KEY_CUSTOM_THEMES, CustomThemesCodec.encode(clean.customThemes)); putString(KEY_SELECTED_CUSTOM_THEME, clean.selectedCustomThemeId)
            putString(KEY_CUSTOM_WALLPAPER, clean.customWallpaper); putString(KEY_WEEK_LAYOUT, clean.weekLayout.name)
            putString(KEY_HOME_WORK_FILTER, HomeWorkFilterCodec.encode(clean.homeWorkFilter))
            putString(KEY_SEMESTER_PERIODS, SemesterPeriodsCodec.encode(clean.semesterPeriods))
            if (clean.currentSemester == null) remove(KEY_CURRENT_SEMESTER) else putInt(KEY_CURRENT_SEMESTER, clean.currentSemester!!)
            if (clean.uisCurrentSemester == null) remove(KEY_UIS_CURRENT_SEMESTER) else putInt(KEY_UIS_CURRENT_SEMESTER, clean.uisCurrentSemester!!)
            putString(KEY_LANGUAGE, clean.language.name); putString(KEY_COURSE_NAME_LANGUAGE, clean.courseNameLanguage.name)
            putString(KEY_UKRAINIAN_NAME_FALLBACK, clean.ukrainianCourseNameFallback.name)
            putString(KEY_UIS_PROGRESS_FREQUENCY, clean.uisAutoSync.progress.name)
            putString(KEY_UIS_SUBJECT_FREQUENCY, clean.uisAutoSync.subjects.name)
            putString(KEY_UIS_PERIOD_FREQUENCY, clean.uisAutoSync.periods.name)
            putString(KEY_UIS_TIMETABLE_FREQUENCY, clean.uisAutoSync.timetable.name)
            putBoolean(KEY_UIS_APPLY_PROGRESS, clean.uisAutoSync.applyProgressAutomatically)
            putString(KEY_REMINDERS, ReminderSettingsCodec.encode(clean.reminders))
        }.commit()) { "Could not persist restored settings" }
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
        customThemes = preferences.getString(KEY_CUSTOM_THEMES, null),
        selectedCustomThemeId = preferences.getString(KEY_SELECTED_CUSTOM_THEME, null),
        reminders = preferences.getString(KEY_REMINDERS, null),
        customWallpaper = preferences.getString(KEY_CUSTOM_WALLPAPER, null),
    )

    private companion object {
        const val PREFS_NAME = "takt_settings"
        const val KEY_CUSTOM_WALLPAPER = "custom_wallpaper"
        const val KEY_REMINDERS = "local_reminders_v1"
        const val KEY_CANCELLATION_STYLE = "cancellation_style"
        const val KEY_SHOW_HIDDEN = "show_hidden_lessons"
        const val KEY_PARITY_OVERRIDE = "parity_override"
        const val KEY_CARD_APPEARANCE = "card_appearance"
        const val KEY_THEME_FAMILY = "theme_family"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_CUSTOM_THEMES = "custom_color_themes"
        const val KEY_SELECTED_CUSTOM_THEME = "selected_custom_theme_id"
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
