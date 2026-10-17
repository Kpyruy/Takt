package com.kpyruy.takt.feature.calendar

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/** Paint against measured card bounds, including fixed timeline height and agenda padding. */
internal fun Modifier.calendarEventAccent(color: Color, startOffset: Dp = 0.dp): Modifier =
    drawBehind {
        val railWidth = 3.dp.toPx()
        val x = if (layoutDirection == LayoutDirection.Ltr) startOffset.toPx()
            else size.width - startOffset.toPx() - railWidth
        drawRect(color, topLeft = Offset(x, 0f), size = Size(railWidth, size.height))
    }
