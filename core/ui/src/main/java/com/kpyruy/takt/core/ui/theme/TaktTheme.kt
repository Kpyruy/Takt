package com.kpyruy.takt.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8DA2FF),
    secondary = Color(0xFF94D3C5),
    background = Color(0xFF0B1220),
    surface = Color(0xFF111A2B),
    surfaceVariant = Color(0xFF182338),
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFF1F5F9),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF3659C9),
    secondary = Color(0xFF27695D),
    background = Color(0xFFF7F8FC),
    surface = Color(0xFFFFFFFF),
)

@Composable
fun TaktTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
