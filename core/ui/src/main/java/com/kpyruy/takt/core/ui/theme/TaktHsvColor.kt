package com.kpyruy.takt.core.ui.theme

import com.kpyruy.takt.core.model.normalizeThemeHex
import java.util.Locale
import kotlin.math.roundToInt

/** Small scalar color math only; no bitmap or per-pixel work during a picker gesture. */
data class TaktHsvColor(val hue: Float, val saturation: Float, val value: Float) {
    fun toHex(): String {
        val h = ((hue % 360f) + 360f) % 360f / 60f
        val s = saturation.coerceIn(0f, 1f)
        val v = value.coerceIn(0f, 1f)
        val chroma = v * s
        val x = chroma * (1f - kotlin.math.abs(h % 2f - 1f))
        val (r, g, b) = when (h.toInt()) {
            0 -> Triple(chroma, x, 0f)
            1 -> Triple(x, chroma, 0f)
            2 -> Triple(0f, chroma, x)
            3 -> Triple(0f, x, chroma)
            4 -> Triple(x, 0f, chroma)
            else -> Triple(chroma, 0f, x)
        }
        val m = v - chroma
        return String.format(Locale.ROOT, "#%02X%02X%02X",
            ((r + m) * 255f).roundToInt(), ((g + m) * 255f).roundToInt(), ((b + m) * 255f).roundToInt())
    }

    companion object {
        fun fromHex(hex: String): TaktHsvColor? {
            val clean = normalizeThemeHex(hex) ?: return null
            val rgb = clean.drop(1).toInt(16)
            val r = (rgb shr 16 and 255) / 255f
            val g = (rgb shr 8 and 255) / 255f
            val b = (rgb and 255) / 255f
            val max = maxOf(r, g, b)
            val min = minOf(r, g, b)
            val delta = max - min
            val hue = when {
                delta == 0f -> 0f
                max == r -> 60f * (((g - b) / delta + 6f) % 6f)
                max == g -> 60f * ((b - r) / delta + 2f)
                else -> 60f * ((r - g) / delta + 4f)
            }
            return TaktHsvColor(hue, if (max == 0f) 0f else delta / max, max)
        }
    }
}
