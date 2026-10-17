package com.kpyruy.takt.core.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.geometry.Size
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** Installed only during the tour. Normal screens do no measuring or bookkeeping. */
class TaktTourTargets {
    data class Tap(val target: String, val sequence: Int)
    var tap by mutableStateOf<Tap?>(null)
        private set
    fun showTap(target: String) { tap = Tap(target, (tap?.sequence ?: 0) + 1) }
    private data class Target(val owner: Any, val bounds: Rect)
    private val targets = mutableStateMapOf<String, Target>()
    fun bounds(key: String): Rect? = targets[key]?.bounds
    internal fun update(key: String, owner: Any, bounds: Rect) {
        val next = Target(owner, bounds)
        if (targets[key] != next) targets[key] = next
    }
    internal fun remove(key: String, owner: Any) {
        if (targets[key]?.owner === owner) targets.remove(key)
    }
}

val LocalTaktTourTargets = staticCompositionLocalOf<TaktTourTargets?> { null }

@Composable
fun Modifier.tourTarget(key: String): Modifier {
    val targets = LocalTaktTourTargets.current ?: return this
    val owner = remember(targets, key) { Any() }
    val tap = targets.tap?.takeIf { it.target == key }
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(tap) {
        if (tap != null) { pulse.snapTo(0f); pulse.animateTo(1f, tween(340)) }
    }
    DisposableEffect(targets, key, owner) { onDispose { targets.remove(key, owner) } }
    return onGloballyPositioned {
        // Popup menus live in a separate window; screen coordinates keep their anchors aligned.
        targets.update(key, owner, Rect(it.positionOnScreen(), Size(it.size.width.toFloat(), it.size.height.toFloat())))
    }.drawWithContent {
        drawContent()
        val value = pulse.value
        if (value < 1f) {
            val radius = (10.dp.toPx() + value * 15.dp.toPx()).coerceAtMost(size.minDimension * .48f)
            drawCircle(Color.White.copy(alpha = .35f * (1f - value)), radius)
            drawCircle(Color.White.copy(alpha = 1f - value), radius, style = Stroke(2.5.dp.toPx()))
            drawCircle(Color.White.copy(alpha = .9f * (1f - value)), 4.dp.toPx())
        }
    }
}
