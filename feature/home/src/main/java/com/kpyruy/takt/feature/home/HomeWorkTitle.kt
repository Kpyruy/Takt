package com.kpyruy.takt.feature.home

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow

/** Reserve space for the complete code before measuring the single-line title. */
@Composable
internal fun HomeWorkTitle(
    title: String,
    courseCode: String,
    completed: Boolean = false,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(Modifier.fillMaxWidth()) {
        Text(title, Modifier.weight(1f, fill = false).alignByBaseline(),
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp, lineHeight = 21.sp), color = titleColor,
            textDecoration = if (completed) TextDecoration.LineThrough else TextDecoration.None,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (courseCode.isNotBlank()) Text(" | $courseCode", Modifier.alignByBaseline(),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp, lineHeight = 20.sp), color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1, softWrap = false)
    }
}
