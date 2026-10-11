package com.kpyruy.takt.core.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

/** Preserve an accent's hue for small text while keeping its icon and background unchanged. */
fun readableAccentColor(accent: Color, background: Color): Color {
    // A small margin keeps the rendered, rounded RGB pixels above WCAG's 4.5:1 threshold.
    val minimum = 4.6f
    fun contrast(color: Color): Float {
        val a = color.luminance(); val b = background.luminance()
        return (maxOf(a, b) + .05f) / (minOf(a, b) + .05f)
    }
    if (contrast(accent) >= minimum) return accent
    val endpoint = if (contrast(Color.Black) >= contrast(Color.White)) Color.Black else Color.White
    var low = 0f
    var high = 1f
    repeat(12) {
        val middle = (low + high) / 2f
        if (contrast(lerp(accent, endpoint, middle)) >= minimum) high = middle else low = middle
    }
    return lerp(accent, endpoint, high)
}
