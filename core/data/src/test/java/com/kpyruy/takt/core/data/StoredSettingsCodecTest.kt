package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.AppSettings
import com.kpyruy.takt.core.model.AppLanguage
import com.kpyruy.takt.core.model.AppThemeMode
import com.kpyruy.takt.core.model.CardAppearance
import com.kpyruy.takt.core.model.CancellationDisplayStyle
import com.kpyruy.takt.core.model.ParityOverride
import com.kpyruy.takt.core.model.ThemeFamily
import com.kpyruy.takt.core.model.WeekLayout
import com.kpyruy.takt.core.model.HomeWorkPeriod
import com.kpyruy.takt.core.model.HomeWorkFilter
import org.junit.Assert.assertEquals
import org.junit.Test

class StoredSettingsCodecTest {
    @Test
    fun invalidCurrentSemesterFallsBackToAutomaticSelection() {
        assertEquals(null, StoredSettingsCodec.decode(null, false, null, currentSemester = "0").currentSemester)
        assertEquals(null, StoredSettingsCodec.decode(null, false, null, currentSemester = "oops").currentSemester)
    }
    @Test
    fun validStoredValues_areDecoded() {
        assertEquals(
            AppSettings(
                cancellationStyle = CancellationDisplayStyle.HIDDEN,
                showHiddenLessons = true,
                parityOverride = ParityOverride.EVEN,
            ),
            StoredSettingsCodec.decode(
                cancellationStyle = "HIDDEN",
                showHiddenLessons = true,
                parityOverride = "EVEN",
            ),
        )
    }

    @Test
    fun unknownStoredValues_fallBackToDefaults() {
        assertEquals(
            AppSettings(),
            StoredSettingsCodec.decode(
                cancellationStyle = "old-value",
                showHiddenLessons = false,
                parityOverride = "invalid",
            ),
        )
    }

    @Test
    fun decodeRestoresVisualPreferences() {
        val periods = mapOf(3 to com.kpyruy.takt.core.model.SemesterPeriod(
            studyStart = java.time.LocalDate.of(2026, 9, 1),
            studyEnd = java.time.LocalDate.of(2026, 12, 18),
        ))
        val settings = StoredSettingsCodec.decode(
            cancellationStyle = "MARKED",
            showHiddenLessons = true,
            parityOverride = "ODD",
            cardAppearance = "TONAL_FILLED",
            themeFamily = "WARM",
            themeMode = "DARK",
            weekLayout = "COMPACT_LIST",
            homeWorkFilter = HomeWorkFilterCodec.encode(HomeWorkFilter(period = HomeWorkPeriod.SEVEN_DAYS)),
            semesterPeriods = SemesterPeriodsCodec.encode(periods),
            currentSemester = "5",
            language = "SLOVAK",
        )

        assertEquals(CardAppearance.TONAL_FILLED, settings.cardAppearance)
        assertEquals(ThemeFamily.WARM, settings.themeFamily)
        assertEquals(AppThemeMode.DARK, settings.themeMode)
        assertEquals(WeekLayout.COMPACT_LIST, settings.weekLayout)
        assertEquals(HomeWorkPeriod.SEVEN_DAYS, settings.homeWorkFilter.period)
        assertEquals(periods, settings.semesterPeriods)
        assertEquals(5, settings.currentSemester)
        assertEquals(AppLanguage.SLOVAK, settings.language)
    }
}
