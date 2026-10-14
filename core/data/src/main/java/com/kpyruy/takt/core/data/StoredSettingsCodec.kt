package com.kpyruy.takt.core.data

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
import com.kpyruy.takt.core.model.CustomColorTheme
import com.kpyruy.takt.core.model.normalizeCustomThemes
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
        currentSemester: String? = null,
        uisCurrentSemester: String? = null,
        language: String? = null,
        courseNameLanguage: String? = null,
        ukrainianCourseNameFallback: String? = null,
        uisProgressFrequency: String? = null,
        uisSubjectFrequency: String? = null,
        uisPeriodFrequency: String? = null,
        uisTimetableFrequency: String? = null,
        uisApplyProgress: Boolean = true,
        customThemes: String? = null,
        selectedCustomThemeId: String? = null,
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
            currentSemester = currentSemester?.toIntOrNull()?.takeIf { it > 0 },
            uisCurrentSemester = uisCurrentSemester?.toIntOrNull()?.takeIf { it > 0 },
            language = enumValueOrNull<AppLanguage>(language) ?: default.language,
            courseNameLanguage = enumValueOrNull<CourseNameLanguage>(courseNameLanguage) ?: default.courseNameLanguage,
            ukrainianCourseNameFallback = enumValueOrNull<CourseNameLanguage>(ukrainianCourseNameFallback)
                ?.takeIf { it != CourseNameLanguage.FOLLOW_APP } ?: default.ukrainianCourseNameFallback,
            uisAutoSync = UisAutoSyncSettings(
                progress = enumValueOrNull<UisRefreshFrequency>(uisProgressFrequency) ?: UisRefreshFrequency.EVERY_ENTRY,
                subjects = enumValueOrNull<UisRefreshFrequency>(uisSubjectFrequency) ?: UisRefreshFrequency.EVERY_ENTRY,
                periods = enumValueOrNull<UisRefreshFrequency>(uisPeriodFrequency) ?: UisRefreshFrequency.MANUAL,
                timetable = enumValueOrNull<UisRefreshFrequency>(uisTimetableFrequency)
                    ?.takeIf { it == UisRefreshFrequency.MANUAL || it == UisRefreshFrequency.TEACHING_START }
                    ?: UisRefreshFrequency.TEACHING_START,
                applyProgressAutomatically = uisApplyProgress,
            ),
            customThemes = CustomThemesCodec.decode(customThemes),
            selectedCustomThemeId = selectedCustomThemeId,
        ).normalizedAppearance()
    }

    private inline fun <reified T : Enum<T>> enumValueOrNull(value: String?): T? =
        value?.let { stored -> enumValues<T>().firstOrNull { it.name == stored } }
}

internal object CustomThemesCodec {
    private val json = Json { ignoreUnknownKeys = true }
    fun encode(themes: List<CustomColorTheme>): String =
        json.encodeToString(normalizeCustomThemes(themes).map {
            BackupCustomColorTheme(it.id, it.name, it.primaryHex, it.secondaryHex, it.tertiaryHex)
        })

    fun decode(raw: String?): List<CustomColorTheme> = raw?.let {
        runCatching { json.decodeFromString<List<BackupCustomColorTheme>>(it).map { theme ->
            CustomColorTheme(theme.id, theme.name, theme.primaryHex, theme.secondaryHex, theme.tertiaryHex)
        }.let(::normalizeCustomThemes) }.getOrNull()
    } ?: emptyList()
}
