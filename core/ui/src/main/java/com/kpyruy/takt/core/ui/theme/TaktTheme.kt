package com.kpyruy.takt.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.kpyruy.takt.core.model.AppSettings
import com.kpyruy.takt.core.model.AppThemeMode
import com.kpyruy.takt.core.model.CardAppearance

val LocalTaktCardAppearance = staticCompositionLocalOf { CardAppearance.ELEVATED }
val LocalTaktSubjectColors = staticCompositionLocalOf<List<Color>> { emptyList() }

@Composable
fun TaktTheme(
    settings: AppSettings = AppSettings(),
    content: @Composable () -> Unit,
) {
    val darkTheme = usesDarkTheme(settings, isSystemInDarkTheme())
    val palette = paletteFor(settings.themeFamily)

    CompositionLocalProvider(
        LocalTaktCardAppearance provides settings.cardAppearance,
        LocalTaktSubjectColors provides if (darkTheme) palette.subjectColorsDark else palette.subjectColorsLight,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) palette.dark else palette.light,
            typography = TaktTypography,
            shapes = TaktShapes,
            content = content,
        )
    }
}

/** Matches the surface shown by the loading screen before Compose draws its first frame. */
fun taktWindowBackgroundColor(settings: AppSettings, systemDark: Boolean): Int {
    val palette = paletteFor(settings.themeFamily)
    return (if (usesDarkTheme(settings, systemDark)) palette.dark else palette.light).surface.toArgb()
}

private fun usesDarkTheme(settings: AppSettings, systemDark: Boolean): Boolean = when (settings.themeMode) {
    AppThemeMode.SYSTEM -> systemDark
    AppThemeMode.LIGHT -> false
    AppThemeMode.DARK -> true
}
