package com.kpyruy.takt.app

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/** Observes unused space only. Child clicks, scrolling and task swipes keep their gestures. */
@Composable
internal fun Modifier.rootMenuSwipe(route: String?, enabled: Boolean, onStep: (Int) -> Unit): Modifier {
    val callback = rememberUpdatedState(onStep)
    val density = LocalDensity.current
    val distance = with(density) { 64.dp.toPx() }
    val edge = with(density) { 24.dp.toPx() }
    return pointerInput(route, enabled, distance) {
        if (!enabled) return@pointerInput
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
            if (down.isConsumed || down.position.x < edge || down.position.x > size.width - edge) return@awaitEachGesture
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Final)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (change.isConsumed || event.changes.size != 1) break
                val delta = change.position - down.position
                if (abs(delta.y) > viewConfiguration.touchSlop && abs(delta.y) > abs(delta.x)) break
                if (!change.pressed) {
                    if (abs(delta.x) >= distance && abs(delta.x) > abs(delta.y) * 2) callback.value(if (delta.x > 0) 1 else -1)
                    break
                }
            }
        }
    }
}
