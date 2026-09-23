package com.kpyruy.takt.feature.subjects

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kpyruy.takt.core.model.Course
import com.kpyruy.takt.core.ui.theme.taktSubjectColor

@Composable internal fun subjectBackground(paper: Boolean = false): Color =
    if (MaterialTheme.colorScheme.surface.luminance() < .5f) MaterialTheme.colorScheme.background
    else if (paper) Color.White else Color(0xFFF6F8FC)

@Composable internal fun SubjectPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable internal fun SubjectMonogram(course: Course) {
    com.kpyruy.takt.core.ui.components.CourseAvatar(course)
}

@Composable internal fun SmallText(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, fontSize = 12.sp, lineHeight = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
