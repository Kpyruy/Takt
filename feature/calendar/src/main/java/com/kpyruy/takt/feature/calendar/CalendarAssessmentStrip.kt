package com.kpyruy.takt.feature.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.GradeItem
import com.kpyruy.takt.core.model.GradeItemType
import com.kpyruy.takt.core.ui.theme.taktSubjectColor

@Composable
internal fun CalendarAssessmentStrip(item: GradeItem, courseTitle: String, onClick: () -> Unit) {
    val icon = when (item.type) {
        GradeItemType.TEST, GradeItemType.MIDTERM -> Icons.Outlined.Quiz
        GradeItemType.EXAM -> Icons.Outlined.School
        else -> Icons.Outlined.Assignment
    }
    val subjectColor = taktSubjectColor(item.courseId)
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().testTag("calendar-assessment-${item.id}"),
        shape = RoundedCornerShape(13.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            Icon(icon, contentDescription = null, tint = subjectColor)
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleSmall)
                Text("${item.type.label} · $courseTitle", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(if (item.completed) "${item.earnedPoints.pointLabel()}/${item.maxPoints.pointLabel()}" else "до ${item.maxPoints.pointLabel()} б.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun Double.pointLabel(): String = if (this % 1.0 == 0.0) toInt().toString() else toString()
