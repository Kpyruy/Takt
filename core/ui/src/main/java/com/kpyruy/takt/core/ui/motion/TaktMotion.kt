package com.kpyruy.takt.core.ui.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

object TaktMotion {
    const val FastMs = 140
    const val StandardMs = 220
    const val EmphasizedMs = 300

    val StandardEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

    fun <T> fast() = tween<T>(durationMillis = FastMs, easing = StandardEasing)
    fun <T> standard() = tween<T>(durationMillis = StandardMs, easing = StandardEasing)
    fun <T> emphasized() = tween<T>(durationMillis = EmphasizedMs, easing = StandardEasing)

    fun <T> menu() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
    )
}
