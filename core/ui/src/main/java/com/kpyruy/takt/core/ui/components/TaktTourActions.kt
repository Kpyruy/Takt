package com.kpyruy.takt.core.ui.components

import androidx.compose.runtime.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import androidx.compose.ui.semantics.SemanticsPropertyKey

/** Test observation without announcing internal state as accessibility text. */
object TaktTourSemantics {
    val CalendarState = SemanticsPropertyKey<String>("TaktTourCalendarState")
    val Playback = SemanticsPropertyKey<String>("TaktTourPlayback")
}

/** Commands are registered only by the tour's current, isolated example screens. */
class TaktTourActions {
    private data class Action(val owner: Any, val run: (Long) -> Unit)
    private val actions = mutableStateMapOf<String, Action>()
    internal fun register(key: String, owner: Any, run: (Long) -> Unit) { actions[key] = Action(owner, run) }
    internal fun remove(key: String, owner: Any) { if (actions[key]?.owner === owner) actions.remove(key) }
    suspend fun perform(key: String, argument: Long = 0) {
        withTimeout(4_000) { snapshotFlow { actions[key] }.first { it != null } }!!.run(argument)
    }
    fun performIfPresent(key: String, argument: Long = 0) { actions[key]?.run?.invoke(argument) }
}

val LocalTaktTourActions = staticCompositionLocalOf<TaktTourActions?> { null }

/** The demonstration calls the same callback used by the actual control. */
@Composable
fun BindTaktTourAction(key: String, action: (Long) -> Unit) {
    val actions = LocalTaktTourActions.current ?: return
    val latest = rememberUpdatedState(action)
    val owner = remember(actions, key) { Any() }
    DisposableEffect(actions, key, owner) {
        actions.register(key, owner) { latest.value(it) }
        onDispose { actions.remove(key, owner) }
    }
}
