package com.kpyruy.takt.core.ui.theme

import androidx.compose.ui.graphics.Color
import com.kpyruy.takt.core.model.ThemeFamily
import com.kpyruy.takt.core.model.AppSettings

fun taktThemePreviewColors(
    family: ThemeFamily,
    dark: Boolean,
): List<Color> {
    val palette = paletteFor(family)
    val colors = if (dark) palette.dark else palette.light
    return listOf(colors.primary, colors.secondary, colors.tertiary)
}

fun taktThemePreviewColors(settings: AppSettings, dark: Boolean): List<Color> {
    val palette = paletteFor(settings)
    val colors = if (dark) palette.dark else palette.light
    return listOf(colors.primary, colors.secondary, colors.tertiary)
}
