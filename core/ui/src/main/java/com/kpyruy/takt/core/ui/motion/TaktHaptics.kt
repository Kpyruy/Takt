package com.kpyruy.takt.core.ui.motion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

class TaktHaptics internal constructor(
    private val confirmAction: () -> Unit,
    private val tickAction: () -> Unit,
) {
    fun confirm() = confirmAction()
    fun tick() = tickAction()
}

@Composable
fun rememberTaktHaptics(): TaktHaptics {
    val feedback = LocalHapticFeedback.current
    return remember(feedback) {
        TaktHaptics(
            confirmAction = {
                feedback.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            tickAction = {
                feedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            },
        )
    }
}
