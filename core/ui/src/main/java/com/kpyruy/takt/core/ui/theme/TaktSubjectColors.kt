package com.kpyruy.takt.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.kpyruy.takt.core.ui.components.LocalCourseCodeKeys

@Composable
fun taktSubjectColor(subjectKey: String?, colorKey: String? = com.kpyruy.takt.core.ui.components.LocalCourseColorKeys.current[subjectKey]): Color {
    com.kpyruy.takt.core.model.CourseColor.fromKey(colorKey)?.let { return Color(it.argb) }
    val colors = LocalTaktSubjectColors.current
    if (colors.isEmpty()) return MaterialTheme.colorScheme.primary
    val code = LocalCourseCodeKeys.current[subjectKey] ?: subjectKey
    val fixedIndex = when (code?.substringBefore("_")) {
        "FYZI" -> 0
        "TPAR" -> 3
        "MATM1", "MATM2", "MATE" -> 2
        "ZAST", "ANJA", "ANJA3" -> 1
        "PROG" -> 4
        else -> null
    }
    if (fixedIndex != null) return colors[fixedIndex % colors.size]
    val raw = subjectKey.orEmpty().hashCode().toLong() and 0x7fffffffL
    return colors[(raw % colors.size).toInt()]
}
