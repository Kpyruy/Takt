package com.kpyruy.takt.core.ui.theme

import androidx.compose.ui.graphics.Color
import com.kpyruy.takt.core.model.ThemeFamily

fun taktThemePreviewColors(family: ThemeFamily): List<Color> =
    paletteFor(family).subjectColorsLight.take(4)
