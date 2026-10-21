package com.kpyruy.takt.feature.home

import com.kpyruy.takt.core.model.uiMessage
import com.kpyruy.takt.core.model.UiKey
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
import com.kpyruy.takt.core.ui.components.EntityIconTile
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
            EntityIconTile(subjectColor, Modifier.size(40.dp)) { foreground ->
                Icon(icon, contentDescription = null, tint = foreground, modifier = Modifier.testTag("home-assessment-glyph-${item.id}"))
            }
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleSmall)
                Text(buildList {
                    add(courseCode)
                    item.durationMinutes?.let { add(t(uiMessage(UiKey.FORMAT_VALUE_MIN, it))) }
                }.joinToString(" · "), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (item.isAwaitingResult) Text(t(UiKey.TEXT_AWAITING_GRADE), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!item.contributesToGrade) Text(t(UiKey.TEXT_EXCLUDED_FROM_THE_FINAL_GRADE), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (item.requiredForExam) {
                    val threshold = item.minimumPointsForExam?.let { t(uiMessage(UiKey.FORMAT_MIN_VALUE_PTS, it.pointText())) }.orEmpty()
                    Text(t(uiMessage(UiKey.FORMAT_REQUIRED_FOR_EXAMVALUE_BC3F4853, threshold)), style = MaterialTheme.typography.labelSmall,
                        color = if (item.completed && !item.meetsAdmissionRequirement) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    if (item.completed) "${item.earnedPoints.pointText()}/${item.maxPoints.pointText()}"
                        else t(uiMessage(UiKey.FORMAT_UP_TO_VALUE_PTS, item.maxPoints.pointText())),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                item.dueDate?.let { date ->
                    val today = LocalDate.now()
                    Text(when (date) {
                        today -> t(UiKey.TEXT_TODAY)
                        today.plusDays(1) -> t(UiKey.TEXT_TOMORROW_456A73BB)
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
