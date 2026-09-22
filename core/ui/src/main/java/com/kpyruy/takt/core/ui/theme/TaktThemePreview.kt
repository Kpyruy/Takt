package com.kpyruy.takt.core.ui.theme

import androidx.compose.ui.graphics.Color
import com.kpyruy.takt.core.model.ThemeFamily

fun taktThemePreviewColors(
    family: ThemeFamily,
    dark: Boolean,
): List<Color> {
    val palette = paletteFor(family)
    return if (dark) palette.subjectColorsDark.take(4) else palette.subjectColorsLight.take(4)
}
