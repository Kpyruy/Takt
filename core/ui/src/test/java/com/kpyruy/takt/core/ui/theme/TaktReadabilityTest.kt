package com.kpyruy.takt.core.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import com.kpyruy.takt.core.model.ThemeFamily
import org.junit.Assert.assertTrue
import org.junit.Test

class TaktReadabilityTest {
    @Test fun smallSecondaryTextIsReadableOnLightPageAndCards() {
        val scheme = paletteFor(ThemeFamily.BLUE).light
        for (background in listOf(scheme.background, scheme.surface, scheme.surfaceContainerHigh)) {
            assertTrue("Small secondary text needs 4.5:1 contrast", contrast(scheme.onSurfaceVariant, background) >= 4.5f)
        }
    }
    @Test fun warmButtonsAndContainerLabelsMeetSmallTextContrast() {
        val scheme = paletteFor(ThemeFamily.WARM).light
        assertTrue("White on warm buttons needs 4.5:1", contrast(scheme.onPrimary, scheme.primary) >= 4.5f)
        assertTrue("Warm container labels need 4.5:1", contrast(scheme.onPrimaryContainer, scheme.primaryContainer) >= 4.5f)
    }
    @Test fun subjectLabelColorsMeetContrastWithoutChangingLegibleAccents() {
        for (family in ThemeFamily.entries) {
            val palette = paletteFor(family)
            for ((scheme, accents) in listOf(palette.light to palette.subjectColorsLight, palette.dark to palette.subjectColorsDark)) {
                for (accent in accents) {
                    for (background in listOf(scheme.surfaceContainerHigh,
                        accent.copy(alpha = .16f).compositeOver(scheme.surfaceContainerHigh))) {
                        val readable = readableAccentColor(accent, background)
                        assertTrue("$family subject label must remain legible after pixel rounding",
                            contrast(Color(readable.toArgb()), background) >= 4.5f)
                        if (contrast(accent, background) >= 4.6f) org.junit.Assert.assertEquals(accent, readable)
                    }
                }
            }
        }
    }
    private fun contrast(a: Color, b: Color): Float {
        val x = a.luminance(); val y = b.luminance()
        return (maxOf(x,y) + .05f) / (minOf(x,y) + .05f)
    }
}
