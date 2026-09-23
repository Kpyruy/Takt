package com.kpyruy.takt.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.kpyruy.takt.core.model.ThemeFamily

internal data class TaktPalette(
    val light: ColorScheme,
    val dark: ColorScheme,
    val subjectColorsLight: List<Color>,
    val subjectColorsDark: List<Color>,
)

private fun palette(
    primary: Color,
    secondary: Color,
    tertiary: Color,
    darkPrimary: Color,
    darkSecondary: Color,
    darkTertiary: Color,
    subjectsLight: List<Color>,
    subjectsDark: List<Color>,
): TaktPalette = TaktPalette(
    light = lightColorScheme(
        primary = primary,
        onPrimary = Color.White,
        primaryContainer = if (primary == Color(0xFF345DD2)) Color(0xFFEAF0FF) else lerp(primary, Color.White, 0.9f),
        onPrimaryContainer = primary,
        secondary = secondary,
        onSecondary = Color.White,
        secondaryContainer = lerp(secondary, Color.White, 0.9f),
        onSecondaryContainer = secondary,
        tertiary = tertiary,
        onTertiary = Color.White,
        tertiaryContainer = lerp(tertiary, Color.White, 0.9f),
        onTertiaryContainer = tertiary,
        background = Color(0xFFF6F8FC),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFEDF2FF),
        onSurface = Color(0xFF1B283D),
        onSurfaceVariant = Color(0xFF68768B),
        onBackground = Color(0xFF1B283D),
        outline = Color(0xFF718096),
        outlineVariant = Color(0xFFE5EAF2),
        surfaceTint = Color.Transparent,
        surfaceContainer = Color.White,
        surfaceContainerLow = Color.White,
        surfaceContainerHigh = Color(0xFFEDF2FF),
        surfaceContainerHighest = Color(0xFFE5EAF2),
    ),
    dark = darkColorScheme(
        primary = darkPrimary,
        onPrimary = Color(0xFF172B56),
        primaryContainer = if (primary == Color(0xFF345DD2)) Color(0xFF2B3E62) else lerp(primary, Color(0xFF202C3C), 0.68f),
        onPrimaryContainer = darkPrimary,
        secondary = darkSecondary,
        onSecondary = Color(0xFF1B283D),
        secondaryContainer = lerp(secondary, Color(0xFF202C3C), 0.68f),
        onSecondaryContainer = darkSecondary,
        tertiary = darkTertiary,
        onTertiary = Color(0xFF1B283D),
        tertiaryContainer = lerp(tertiary, Color(0xFF202C3C), 0.68f),
        onTertiaryContainer = darkTertiary,
        background = Color(0xFF131C29),
        surface = Color(0xFF202C3C),
        surfaceVariant = Color(0xFF29374B),
        onSurface = Color(0xFFEDF3FF),
        onSurfaceVariant = Color(0xFFAFBBCD),
        onBackground = Color(0xFFEDF3FF),
        outline = Color(0xFF8594AA),
        outlineVariant = Color(0xFF364257),
        surfaceTint = Color.Transparent,
        surfaceContainer = Color(0xFF202C3C),
        surfaceContainerLow = Color(0xFF202C3C),
        surfaceContainerHigh = Color(0xFF29374B),
        surfaceContainerHighest = Color(0xFF364257),
    ),
    subjectColorsLight = subjectsLight,
    subjectColorsDark = subjectsDark,
)

internal fun paletteFor(family: ThemeFamily): TaktPalette = when (family) {
    ThemeFamily.BLUE -> palette(
        primary = Color(0xFF345DD2),
        secondary = Color(0xFF27695D),
        tertiary = Color(0xFF7454B8),
        darkPrimary = Color(0xFFAAC1FF),
        darkSecondary = Color(0xFF94D3C5),
        darkTertiary = Color(0xFFC7A9FF),
        subjectsLight = listOf(
            Color(0xFF677ACF), Color(0xFF619E8C), Color(0xFF8874B3),
            Color(0xFFC58B4C), Color(0xFFBE7096), Color(0xFF619E8C),
        ),
        subjectsDark = listOf(
            Color(0xFF7AB6FF), Color(0xFF61D6C6), Color(0xFFB69CFF),
            Color(0xFFFFC761), Color(0xFFFF92C5), Color(0xFF69D5A3),
        ),
    )
    ThemeFamily.GREEN -> palette(
        primary = Color(0xFF147D64),
        secondary = Color(0xFF356B9A),
        tertiary = Color(0xFF9A6717),
        darkPrimary = Color(0xFF70D6B6),
        darkSecondary = Color(0xFF8DC7F5),
        darkTertiary = Color(0xFFE7B85B),
        subjectsLight = listOf(
            Color(0xFF16A085), Color(0xFF3B82F6), Color(0xFF84A937),
            Color(0xFFF59E0B), Color(0xFF9B6BD3), Color(0xFFE05D6F),
        ),
        subjectsDark = listOf(
            Color(0xFF67D5BE), Color(0xFF7AB6FF), Color(0xFFB7D66E),
            Color(0xFFFFC761), Color(0xFFC5A0EC), Color(0xFFF194A1),
        ),
    )
    ThemeFamily.PURPLE -> palette(
        primary = Color(0xFF7048C8),
        secondary = Color(0xFF3A7198),
        tertiary = Color(0xFF9D5F7A),
        darkPrimary = Color(0xFFB49AF2),
        darkSecondary = Color(0xFF8CC5EC),
        darkTertiary = Color(0xFFE0A2BF),
        subjectsLight = listOf(
            Color(0xFF7C5CE0), Color(0xFF3B82F6), Color(0xFF00A68A),
            Color(0xFFE08035), Color(0xFFD9568A), Color(0xFF9A70B8),
        ),
        subjectsDark = listOf(
            Color(0xFFB69CFF), Color(0xFF7AB6FF), Color(0xFF65D4BD),
            Color(0xFFFFB66F), Color(0xFFF08CB2), Color(0xFFC8A0DF),
        ),
    )
    ThemeFamily.WARM -> palette(
        primary = Color(0xFFC65A2E),
        secondary = Color(0xFF9B6A19),
        tertiary = Color(0xFF8B4D72),
        darkPrimary = Color(0xFFF4A17E),
        darkSecondary = Color(0xFFE7C06B),
        darkTertiary = Color(0xFFDCA0C5),
        subjectsLight = listOf(
            Color(0xFFE36D3D), Color(0xFFD99A20), Color(0xFFB65D86),
            Color(0xFF6E8F3D), Color(0xFF4A7FB1), Color(0xFF9A6BC0),
        ),
        subjectsDark = listOf(
            Color(0xFFFFA17B), Color(0xFFFFC965), Color(0xFFE49BBD),
            Color(0xFFA9C97A), Color(0xFF86B7E5), Color(0xFFC7A1E2),
        ),
    )
    ThemeFamily.MONOCHROME -> palette(
        primary = Color(0xFF303846),
        secondary = Color(0xFF5C6573),
        tertiary = Color(0xFF7B8491),
        darkPrimary = Color(0xFFD5DBE5),
        darkSecondary = Color(0xFFADB7C5),
        darkTertiary = Color(0xFF8F9AAA),
        subjectsLight = listOf(
            Color(0xFF2F6B8A), Color(0xFF547A62), Color(0xFF7B6B9B),
            Color(0xFF9A734A), Color(0xFF855E72), Color(0xFF586B80),
        ),
        subjectsDark = listOf(
            Color(0xFF75A9C2), Color(0xFF83AB8E), Color(0xFFA79BC1),
            Color(0xFFC2A078), Color(0xFFB88FA4), Color(0xFF8EA1B4),
        ),
    )
}
