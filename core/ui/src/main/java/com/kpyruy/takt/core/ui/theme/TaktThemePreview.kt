package com.kpyruy.takt.core.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.kpyruy.takt.core.model.ThemeFamily
import com.kpyruy.takt.core.model.AppSettings
import com.kpyruy.takt.core.model.CustomColorTheme
import java.util.Locale

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

/** Only an editor draft materializes derived roles; stored legacy themes keep nullable defaults. */
fun taktEditableCustomTheme(theme: CustomColorTheme): CustomColorTheme {
    if (theme.secondaryHex != null && theme.tertiaryHex != null) return theme
    val colors = taktThemePreviewColors(AppSettings(customThemes = listOf(theme), selectedCustomThemeId = theme.id), false)
    fun Color.hex() = String.format(Locale.ROOT, "#%06X", toArgb() and 0xFFFFFF)
    return theme.copy(secondaryHex = theme.secondaryHex ?: colors[1].hex(),
        tertiaryHex = theme.tertiaryHex ?: colors[2].hex())
}
