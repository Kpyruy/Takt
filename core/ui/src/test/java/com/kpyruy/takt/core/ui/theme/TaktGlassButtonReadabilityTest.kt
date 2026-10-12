package com.kpyruy.takt.core.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.kpyruy.takt.core.model.ThemeFamily
import com.kpyruy.takt.core.ui.components.glassButtonColorStops
import org.junit.Assert.assertTrue
import org.junit.Test

class TaktGlassButtonReadabilityTest {
    @Test fun whiteActionLabelsMeetContrastAcrossAllPalettesAndGradientPositions() {
        ThemeFamily.entries.forEach { family ->
            val palette = paletteFor(family)
            listOf(palette.light to false, palette.dark to true).forEach { (scheme, dark) ->
                val base = if (dark) lerp(scheme.primary, scheme.primaryContainer, .72f) else scheme.primary
                val stops = glassButtonColorStops(base, enabled = true)
                (0..20).forEach { position ->
                    val fraction = position / 20f
                    val shade = if (fraction <= .5f) lerp(stops[0], stops[1], fraction * 2f)
                        else lerp(stops[1], stops[2], (fraction - .5f) * 2f)
                    val ratio = (Color.White.luminance() + .05f) / (shade.luminance() + .05f)
                    assertTrue("$family dark=$dark gradient=$fraction contrast=$ratio", ratio >= 4.5f)
                }
            }
        }
    }
}
