package com.kpyruy.takt.core.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.lessonInteraction(onClick: () -> Unit, onLongClick: () -> Unit): Modifier =
    combinedClickable(onClickLabel = "Відкрити предмет", onLongClickLabel = "Дії з парою", onClick = onClick, onLongClick = onLongClick)
