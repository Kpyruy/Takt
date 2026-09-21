package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.AppSettings
import com.kpyruy.takt.core.model.CancellationDisplayStyle
import com.kpyruy.takt.core.model.ParityOverride
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
}
