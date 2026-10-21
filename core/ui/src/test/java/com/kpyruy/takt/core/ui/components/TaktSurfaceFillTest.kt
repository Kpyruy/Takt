package com.kpyruy.takt.core.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Test

class TaktSurfaceFillTest {
    @Test fun translucentActiveAccentIsCompositedOntoAnOpaqueMaterialBase() {
        val fill = opaqueSurfaceFill(Color(0xFF202C3B), Color(0xFFC2ABF2).copy(alpha = .12f))
        assertEquals("A non-glass card must hide the wallpaper", 1f, fill.alpha, .0001f)
        // 12% accent over the dark surface, not 12% accent over the wallpaper.
        assertEquals(0xFF343B51.toInt(), fill.toArgb())
    }

    @Test fun tonalFilledAccentsUseTheirTonalBase() {
        val fill = opaqueSurfaceFill(Color(0xFFE2E7FF), Color(0xFF3451A4).copy(alpha = .18f))
        assertEquals(1f, fill.alpha, .0001f)
        assertEquals(0xFFC3CCEF.toInt(), fill.toArgb())
    }

    @Test fun unspecifiedAndSolidTintsKeepTheMaterialColors() {
        val base = Color(0xFF202C3B)
        assertEquals(base, opaqueSurfaceFill(base, Color.Unspecified))
        val accent = Color(0xFF3451A4)
        assertEquals(accent, opaqueSurfaceFill(base, accent))
    }
}
