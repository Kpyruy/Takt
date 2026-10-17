package com.kpyruy.takt.core.ui.motion

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** Leading-edge throttle: immediate first switch, no queue and no extension from ignored taps. */
internal class NavigationTapGate(private val cooldownMs: Long, private val nowMs: () -> Long) {
    private var acceptedAt: Long? = null
    fun tryAcquire(): Boolean {
        val now = nowMs()
        if (acceptedAt?.let { now - it < cooldownMs } == true) return false
        acceptedAt = now
        return true
    }
}

/** Shared by a whole rail so alternating different buttons cannot bypass its cooldown. */
@Composable
fun rememberTabSwitchGuard(): (() -> Unit) -> Unit {
    val gate = remember { NavigationTapGate(TaktMotion.NavigationTapCooldownMs.toLong(), SystemClock::elapsedRealtime) }
    return remember(gate) { { action -> if (gate.tryAcquire()) action() } }
}
