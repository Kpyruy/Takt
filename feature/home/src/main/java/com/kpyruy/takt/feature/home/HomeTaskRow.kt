package com.kpyruy.takt.feature.home

import com.kpyruy.takt.core.ui.i18n.t

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import com.kpyruy.takt.core.ui.components.glassSurface
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.StudyTask
import com.kpyruy.takt.core.ui.components.SwipeCompletionBox
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HomeTaskRow(task: StudyTask, courseTitle: String, onCompletedChange: (Boolean) -> Unit) {
    val today = LocalDate.now()
    val due = task.dueDate?.let { when (it) {
        today -> t("Today")
        today.plusDays(1) -> t("Tomorrow")
        else -> it.format(DateTimeFormatter.ofPattern("dd.MM"))
    } }
    val overdue = !task.completed && task.dueDate?.isBefore(today) == true
    SwipeCompletionBox(completed = task.completed, onCompletedChange = onCompletedChange) {
        Row(Modifier.fillMaxWidth().testTag("home-task-${task.id}").animateContentSize()
            .glassSurface(tint = if (overdue) MaterialTheme.colorScheme.errorContainer else androidx.compose.ui.graphics.Color.Unspecified)
            .padding(8.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconToggleButton(checked = task.completed, onCheckedChange = onCompletedChange,
                modifier = Modifier.size(40.dp).glassSurface(tint = MaterialTheme.colorScheme.primaryContainer)) {
                Icon(if (task.completed) Icons.Outlined.CheckCircle else Icons.Outlined.Assignment,
                    task.title, tint = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f)) {
                Text(task.title, style = MaterialTheme.typography.titleSmall,
                    color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (task.completed) TextDecoration.LineThrough else null)
                Text(listOfNotNull(courseTitle, due).joinToString(" · "), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (task.requiredForExam) Text(t("Required for exam") + task.minimumPointsForExam?.let { t(" · min. ${it.pointText()} pts") }.orEmpty(),
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (task.requiredForExam && task.completed && !task.meetsAdmissionRequirement)
                    Text(t("Threshold for admission not met"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
            task.maxPoints?.let { maximum ->
                Text(if (task.earnedPoints != null) t("${task.earnedPoints?.pointText()} / ${maximum.pointText()} pts") else t("up to ${maximum.pointText()} pts"),
                    Modifier.glassSurface(tint = MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
