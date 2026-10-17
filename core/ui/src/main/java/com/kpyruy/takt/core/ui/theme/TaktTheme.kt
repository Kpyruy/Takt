package com.kpyruy.takt.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.kpyruy.takt.core.model.AppSettings
import com.kpyruy.takt.core.model.AppThemeMode
import com.kpyruy.takt.core.model.CardAppearance
import com.kpyruy.takt.core.model.ThemeFamily
import com.kpyruy.takt.core.model.CustomColorTheme
import com.kpyruy.takt.core.ui.i18n.TaktNativeLocale

val LocalTaktWallpaper = staticCompositionLocalOf<String?> { null }
val LocalTaktThemeFamily = staticCompositionLocalOf { ThemeFamily.BLUE }
val LocalTaktCustomTheme = staticCompositionLocalOf<CustomColorTheme?> { null }
val LocalTaktCardAppearance = staticCompositionLocalOf { CardAppearance.ELEVATED }
val LocalTaktSubjectColors = staticCompositionLocalOf<List<Color>> { emptyList() }

@Composable
fun TaktTheme(
    settings: AppSettings = AppSettings(),
    content: @Composable () -> Unit,
) {
    val darkTheme = usesDarkTheme(settings, isSystemInDarkTheme())
    val customTheme = settings.selectedCustomTheme
    val palette = remember(settings.themeFamily, customTheme) { paletteFor(settings) }

    CompositionLocalProvider(
        LocalContentColor provides if (darkTheme) palette.dark.onSurface else palette.light.onSurface,
        LocalTaktCardAppearance provides settings.cardAppearance,
        LocalTaktThemeFamily provides settings.themeFamily,
        LocalTaktCustomTheme provides customTheme,
        LocalTaktWallpaper provides settings.customWallpaper,
        LocalTaktSubjectColors provides if (darkTheme) palette.subjectColorsDark else palette.subjectColorsLight,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) palette.dark else palette.light,
            typography = TaktTypography,
            shapes = TaktShapes,
            content = { TaktNativeLocale {
                CompositionLocalProvider(androidx.compose.foundation.LocalIndication provides
                    com.kpyruy.takt.core.ui.components.TaktPressIndication) { content() }
            } },
        )
    }
}

/** Matches the surface shown by the loading screen before Compose draws its first frame. */
fun taktWindowBackgroundColor(settings: AppSettings, systemDark: Boolean): Int {
    val palette = paletteFor(settings)
    return (if (usesDarkTheme(settings, systemDark)) palette.dark else palette.light).surface.toArgb()
}

private fun usesDarkTheme(settings: AppSettings, systemDark: Boolean): Boolean = when (settings.themeMode) {
    AppThemeMode.SYSTEM -> systemDark
    AppThemeMode.LIGHT -> false
    AppThemeMode.DARK -> true
}
