package com.kpyruy.takt.feature.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle

/** Keep the course code attached to the title; long titles can still wrap naturally. */
@Composable
internal fun HomeWorkTitle(
    title: String,
    courseCode: String,
    completed: Boolean = false,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    val codeStyle = SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = MaterialTheme.typography.bodySmall.fontSize, fontWeight = FontWeight.Normal)
    Text(buildAnnotatedString {
        withStyle(SpanStyle(textDecoration = if (completed) TextDecoration.LineThrough else TextDecoration.None)) {
            append(title)
        }
        if (courseCode.isNotBlank()) withStyle(codeStyle) { append(" | "); append(courseCode) }
    }, style = MaterialTheme.typography.titleSmall, color = titleColor)
}
