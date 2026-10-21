package com.kpyruy.takt.feature.home

import com.kpyruy.takt.core.ui.i18n.t

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.kpyruy.takt.core.ui.components.TaktSurface as Surface
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.GradeItem
import com.kpyruy.takt.core.model.GradeItemType
import com.kpyruy.takt.core.ui.theme.taktSubjectColor
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
internal fun HomeAssessmentRow(item: GradeItem, courseCode: String, onClick: () -> Unit) {
    val subjectColor = taktSubjectColor(item.courseId)
    val icon = when (item.type) {
        GradeItemType.TEST, GradeItemType.MIDTERM -> Icons.Outlined.Quiz
        GradeItemType.EXAM -> Icons.Outlined.School
        GradeItemType.LAB -> Icons.Outlined.Science
        else -> Icons.Outlined.Assignment
    }
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth().testTag("home-assessment-${item.id}")) {
        Row(
            Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val iconShape = RoundedCornerShape(13.dp)
            Box(Modifier.size(40.dp).clip(iconShape)
                .background(subjectColor.copy(alpha = .16f), iconShape)
                .border(.75.dp, subjectColor.copy(alpha = .28f), iconShape), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = subjectColor, modifier = Modifier.testTag("home-assessment-glyph-${item.id}"))
            }
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleSmall)
                Text(buildList {
                    add(courseCode)
                    item.durationMinutes?.let { add(t("${it} min")) }
                }.joinToString(" · "), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (item.isAwaitingResult) Text(t("Awaiting grade"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!item.contributesToGrade) Text(t("Excluded from the final grade"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (item.requiredForExam) {
                    val threshold = item.minimumPointsForExam?.let { t(" · min. ${it.pointText()} pts") }.orEmpty()
                    Text(t("Required for exam${threshold}"), style = MaterialTheme.typography.labelSmall,
                        color = if (item.completed && !item.meetsAdmissionRequirement) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    if (item.completed) "${item.earnedPoints.pointText()}/${item.maxPoints.pointText()}"
                        else t("up to ${item.maxPoints.pointText()} pts"),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                item.dueDate?.let { date ->
                    val today = LocalDate.now()
                    Text(when (date) {
                        today -> t("Today")
                        today.plusDays(1) -> t("Tomorrow")
                        else -> date.format(DateTimeFormatter.ofPattern("dd.MM"))
                    }, style = MaterialTheme.typography.labelSmall,
                        color = if (!item.isSubmitted && date <= today) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

internal fun Double.pointText(): String = if (this % 1.0 == 0.0) toInt().toString() else toString()
