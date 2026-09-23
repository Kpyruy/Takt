package com.kpyruy.takt.core.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** The same lightweight line icons used in the approved UI studies. */
object TaktIcons {
    private fun lineIcon(name: String, data: String) = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        .addPath(PathParser().parsePathString(data).toNodes(), stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.65f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        .build()
    val Home = lineIcon("TaktHome", "M3 10 12 3l9 7v10a1 1 0 0 1-1 1h-5v-7H9v7H4a1 1 0 0 1-1-1Z")
    val Calendar = lineIcon("TaktCalendar", "M5 5h14a2 2 0 0 1 2 2v13H3V7a2 2 0 0 1 2-2ZM7 3v4m10-4v4M3 10h18M7 14h2m4 0h2m2 3h-2m-8 0h2")
    val Book = lineIcon("TaktBook", "M12 5C8 2 4 3 3 4v16c3-2 6-1 9 1m0-16c4-3 8-2 9-1v16c-3-2-6-1-9 1Zm0 0v16")
    val Progress = lineIcon("TaktProgress", "M4 21V11m8 10V3m8 18v-7")
}
