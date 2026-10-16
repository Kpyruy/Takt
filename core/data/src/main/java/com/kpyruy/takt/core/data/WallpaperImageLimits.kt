package com.kpyruy.takt.core.data

import kotlin.math.roundToInt

internal object WallpaperImageLimits {
    const val MAX_BYTES = 24L * 1024 * 1024
    const val MAX_EDGE = 2048
    fun outputSize(width: Int, height: Int): Pair<Int, Int> {
        require(width > 0 && height > 0 && width <= 32768 && height <= 32768 && width.toLong() * height <= 64_000_000)
        val scale = minOf(1.0, MAX_EDGE.toDouble() / maxOf(width, height))
        return maxOf(1, (width * scale).roundToInt()) to maxOf(1, (height * scale).roundToInt())
    }
}
