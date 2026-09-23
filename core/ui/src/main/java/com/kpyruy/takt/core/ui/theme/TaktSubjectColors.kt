package com.kpyruy.takt.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun taktSubjectColor(subjectKey: String?): Color {
    val colors = LocalTaktSubjectColors.current
    if (colors.isEmpty()) return MaterialTheme.colorScheme.primary
    val fixedIndex = when (subjectKey?.substringBefore("_")) {
        "FYZI" -> 0
        "TPAR" -> 3
        "MATM1", "MATM2" -> 2
        "ZAST" -> 1
        else -> null
    }
    if (fixedIndex != null) return colors[fixedIndex % colors.size]
    val raw = subjectKey.orEmpty().hashCode().toLong() and 0x7fffffffL
    return colors[(raw % colors.size).toInt()]
}
