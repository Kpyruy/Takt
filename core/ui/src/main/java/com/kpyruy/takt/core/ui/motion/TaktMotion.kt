package com.kpyruy.takt.core.ui.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween

object TaktMotion {
    const val FastMs = 147
    const val StandardMs = 260
    const val EmphasizedMs = 340

    val StandardEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

    fun <T> fast() = tween<T>(durationMillis = FastMs, easing = StandardEasing)
    fun <T> standard() = tween<T>(durationMillis = StandardMs, easing = StandardEasing)
    fun <T> emphasized() = tween<T>(durationMillis = EmphasizedMs, easing = StandardEasing)

    const val NavigationTapCooldownMs = 120
    const val NavigationMs = 330
    const val ContentMs = 202
    // Smooth acceleration and a gentle finish; evaluated against the display's frame clock.
    // No fixed60Hz keyframes:120/144/240Hz receive their own intermediate positions.
    val NavigationEasing = CubicBezierEasing(0.30f, 0.0f, 0.25f, 1.0f)

    fun <T> menu() = tween<T>(durationMillis = NavigationMs, easing = NavigationEasing)
    fun <T> content() = tween<T>(durationMillis = ContentMs, easing = NavigationEasing)
}
