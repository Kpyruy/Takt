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
    val Atom = lineIcon("TaktAtom", "M22.00 12.00 L21.91 12.46 L21.66 12.91 L21.24 13.34 L20.66 13.75 L19.93 14.13 L19.07 14.47 L18.09 14.78 L17.00 15.03 L15.83 15.23 L14.59 15.38 L13.31 15.47 L12.00 15.50 L10.69 15.47 L9.41 15.38 L8.17 15.23 L7.00 15.03 L5.91 14.78 L4.93 14.47 L4.07 14.13 L3.34 13.75 L2.76 13.34 L2.34 12.91 L2.09 12.46 L2.00 12.00 L2.09 11.54 L2.34 11.09 L2.76 10.66 L3.34 10.25 L4.07 9.87 L4.93 9.53 L5.91 9.22 L7.00 8.97 L8.17 8.77 L9.41 8.62 L10.69 8.53 L12.00 8.50 L13.31 8.53 L14.59 8.62 L15.83 8.77 L17.00 8.97 L18.09 9.22 L19.07 9.53 L19.93 9.87 L20.66 10.25 L21.24 10.66 L21.66 11.09 L21.91 11.54 L22.00 12.00Z M17.00 20.66 L16.56 20.81 L16.05 20.82 L15.46 20.67 L14.81 20.38 L14.12 19.94 L13.39 19.36 L12.64 18.66 L11.88 17.85 L11.11 16.93 L10.37 15.93 L9.65 14.87 L8.97 13.75 L8.34 12.60 L7.78 11.45 L7.29 10.30 L6.88 9.19 L6.55 8.12 L6.32 7.11 L6.19 6.19 L6.15 5.38 L6.22 4.67 L6.39 4.09 L6.65 3.64 L7.00 3.34 L7.44 3.19 L7.95 3.18 L8.54 3.33 L9.19 3.62 L9.88 4.06 L10.61 4.64 L11.36 5.34 L12.12 6.15 L12.89 7.07 L13.63 8.07 L14.35 9.13 L15.03 10.25 L15.66 11.40 L16.22 12.55 L16.71 13.70 L17.12 14.81 L17.45 15.88 L17.68 16.89 L17.81 17.81 L17.85 18.62 L17.78 19.33 L17.61 19.91 L17.35 20.36 L17.00 20.66Z M7.00 20.66 L6.65 20.36 L6.39 19.91 L6.22 19.33 L6.15 18.62 L6.19 17.81 L6.32 16.89 L6.55 15.88 L6.88 14.81 L7.29 13.70 L7.78 12.55 L8.34 11.40 L8.97 10.25 L9.65 9.13 L10.37 8.07 L11.11 7.07 L11.87 6.15 L12.64 5.34 L13.39 4.64 L14.12 4.06 L14.81 3.62 L15.46 3.33 L16.05 3.18 L16.56 3.19 L17.00 3.34 L17.35 3.64 L17.61 4.09 L17.78 4.67 L17.85 5.37 L17.81 6.19 L17.68 7.11 L17.45 8.12 L17.12 9.19 L16.71 10.30 L16.22 11.45 L15.66 12.60 L15.03 13.75 L14.35 14.87 L13.63 15.93 L12.89 16.93 L12.12 17.85 L11.36 18.66 L10.61 19.36 L9.88 19.94 L9.19 20.38 L8.54 20.67 L7.95 20.82 L7.44 20.81 L7.00 20.66Z M13 12a1 1 0 1 0-2 0a1 1 0 1 0 2 0")
}
