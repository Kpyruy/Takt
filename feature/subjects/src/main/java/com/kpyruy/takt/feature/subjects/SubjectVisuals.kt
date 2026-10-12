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
import com.kpyruy.takt.core.ui.components.GlassSurface

@Composable internal fun subjectBackground(paper: Boolean = false): Color =
    Color.Transparent

@Composable internal fun SubjectPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    GlassSurface(modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable internal fun SubjectMonogram(course: Course) {
    com.kpyruy.takt.core.ui.components.CourseAvatar(course)
}

@Composable internal fun SmallText(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, fontSize = 13.sp, lineHeight = 19.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
