package com.kpyruy.takt.core.ui.components

import com.kpyruy.takt.core.ui.i18n.t

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.lessonInteraction(onClick: () -> Unit, onLongClick: () -> Unit): Modifier =
    combinedClickable(onClickLabel = t("Відкрити предмет"), onLongClickLabel = t("Дії з парою"), onClick = onClick, onLongClick = onLongClick)
