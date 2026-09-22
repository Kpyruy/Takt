package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.AppSettings
import com.kpyruy.takt.core.model.AppThemeMode
import com.kpyruy.takt.core.model.CardAppearance
import com.kpyruy.takt.core.model.CancellationDisplayStyle
import com.kpyruy.takt.core.model.ParityOverride
import com.kpyruy.takt.core.model.ThemeFamily
import com.kpyruy.takt.core.model.WeekLayout
import org.junit.Assert.assertEquals
import org.junit.Test

class StoredSettingsCodecTest {
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
        val settings = StoredSettingsCodec.decode(
            cancellationStyle = "MARKED",
            showHiddenLessons = true,
            parityOverride = "ODD",
            cardAppearance = "TONAL_FILLED",
            themeFamily = "WARM",
            themeMode = "DARK",
            weekLayout = "COMPACT_LIST",
        )

        assertEquals(CardAppearance.TONAL_FILLED, settings.cardAppearance)
        assertEquals(ThemeFamily.WARM, settings.themeFamily)
        assertEquals(AppThemeMode.DARK, settings.themeMode)
        assertEquals(WeekLayout.COMPACT_LIST, settings.weekLayout)
    }
}
