package com.kpyruy.takt.core.ui.components

import com.kpyruy.takt.core.ui.i18n.t

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.ui.motion.rememberTaktHaptics
import com.kpyruy.takt.core.ui.theme.LocalTaktCardAppearance
import com.kpyruy.takt.core.model.CardAppearance

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeCompletionBox(
    completed: Boolean,
    onCompletedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val currentCompleted by rememberUpdatedState(completed)
    val currentOnCompletedChange by rememberUpdatedState(onCompletedChange)
    val haptics = rememberTaktHaptics()
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) {
                currentOnCompletedChange(!currentCompleted)
                haptics.confirm()
                false
            } else {
                true
            }
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = false,
        backgroundContent = {
            if (dismissState.dismissDirection != SwipeToDismissBoxValue.Settled) Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (completed) "↺" else "✓")
                    Text(if (completed) t("Відновити") else t("Виконано"))
                }
            }
        },
        content = {
            val background = if (LocalTaktCardAppearance.current == CardAppearance.TONAL_FILLED)
                MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
            Box(Modifier.fillMaxWidth().background(background)) { content() }
        },
    )
}
