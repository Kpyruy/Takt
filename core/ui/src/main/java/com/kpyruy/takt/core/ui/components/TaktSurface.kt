package com.kpyruy.takt.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** A glass pane for existing Surface call sites; interaction and content color remain explicit. */
@Composable
fun TaktSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    color: Color = Color.Unspecified,
    contentColor: Color = LocalContentColor.current,
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    border: BorderStroke? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val tint = if (color == scheme.surface || color == scheme.background) Color.Unspecified else color
    var pane = modifier.glassSurface(shape, tint)
    if (border != null) pane = pane.border(border, shape)
    if (onClick != null) pane = pane.clickable(enabled = enabled, onClick = onClick)
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Box(pane, propagateMinConstraints = true) { content() }
    }
}
