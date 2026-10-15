package com.kpyruy.takt.core.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned

/** Installed only during the tour. Normal screens do no measuring or bookkeeping. */
class TaktTourTargets {
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
    DisposableEffect(targets, key, owner) { onDispose { targets.remove(key, owner) } }
    return onGloballyPositioned { targets.update(key, owner, it.boundsInWindow()) }
}
