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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
internal fun HomeAssessmentRow(item: GradeItem, courseCode: String, onClick: () -> Unit) {
    val subjectColor = taktSubjectColor(item.courseId)
    val icon = when (item.type) {
        GradeItemType.TEST, GradeItemType.MIDTERM -> Icons.Outlined.Quiz
        GradeItemType.EXAM -> Icons.Outlined.School
        else -> Icons.Outlined.Assignment
    }
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth().testTag("home-assessment-${item.id}")) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(icon, contentDescription = null, tint = subjectColor)
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleSmall)
                Text(buildList {
                    add(courseCode)
                    item.durationMinutes?.let { add(t("$it хв")) }
                }.joinToString(" · "), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (item.requiredForExam) {
                    val threshold = item.minimumPointsForExam?.let { t(" · мін. ${it.pointText()} б.") }.orEmpty()
                    Text(t("Для допуску$threshold"), style = MaterialTheme.typography.labelSmall,
                        color = if (item.completed && !item.meetsAdmissionRequirement) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    if (item.completed) "${item.earnedPoints.pointText()}/${item.maxPoints.pointText()}"
                        else t("до ${item.maxPoints.pointText()} б."),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                item.dueDate?.let { date ->
                    val today = LocalDate.now()
                    Text(when (date) {
                        today -> t("Сьогодні")
                        today.plusDays(1) -> t("Завтра")
                        else -> date.format(DateTimeFormatter.ofPattern("dd.MM"))
                    }, style = MaterialTheme.typography.labelSmall,
                        color = if (date <= today) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

internal fun Double.pointText(): String = if (this % 1.0 == 0.0) toInt().toString() else toString()
