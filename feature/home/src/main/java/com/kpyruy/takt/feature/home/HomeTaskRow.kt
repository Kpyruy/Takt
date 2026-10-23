package com.kpyruy.takt.feature.home

import com.kpyruy.takt.core.model.uiMessage
import com.kpyruy.takt.core.model.UiKey
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.StudyTask
import com.kpyruy.takt.core.ui.components.SwipeCompletionBox
import com.kpyruy.takt.core.ui.components.EntityIconTile
import com.kpyruy.takt.core.ui.components.WorkRequirementIcons
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HomeTaskRow(task: StudyTask, courseCode: String, onCompletedChange: (Boolean) -> Unit) {
    val today = LocalDate.now()
    val due = task.dueDate?.let { when (it) {
        today -> t(UiKey.TEXT_TODAY)
        today.plusDays(1) -> t(UiKey.TEXT_TOMORROW_456A73BB)
        else -> it.format(DateTimeFormatter.ofPattern("dd.MM"))
    } }
    val overdue = !task.completed && task.dueDate?.isBefore(today) == true
    SwipeCompletionBox(completed = task.completed, onCompletedChange = onCompletedChange) {
        Row(Modifier.fillMaxWidth().testTag("home-task-${task.id}").animateContentSize()
            .glassSurface(tint = if (overdue) MaterialTheme.colorScheme.errorContainer else androidx.compose.ui.graphics.Color.Unspecified)
            .padding(8.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            EntityIconTile(com.kpyruy.takt.core.ui.theme.taktSubjectColor(task.courseId), Modifier.size(40.dp)) { foreground ->
                IconToggleButton(checked = task.completed, onCheckedChange = onCompletedChange,
                    modifier = Modifier.size(40.dp)) {
                    Icon(if (task.completed) Icons.Outlined.CheckCircle else Icons.Outlined.Assignment,
                        task.title, tint = foreground)
                }
            }
            Column(Modifier.weight(1f)) {
                HomeWorkTitle(task.title, courseCode, completed = task.completed,
                    titleColor = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                WorkRequirementIcons(
                    requiredForExam = task.requiredForExam,
                    minimumPointsForExam = task.minimumPointsForExam,
                    admissionWarning = task.completed && !task.meetsAdmissionRequirement,
                    tagPrefix = "home-task-${task.id}",
                )
            }
            if (task.maxPoints != null || due != null) Column(horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)) {
                task.maxPoints?.let { maximum ->
                    Text(if (task.earnedPoints != null) t(uiMessage(UiKey.FORMAT_VALUE_VALUE_PTS, task.earnedPoints?.pointText(), maximum.pointText())) else t(uiMessage(UiKey.FORMAT_UP_TO_VALUE_PTS, maximum.pointText())),
                        Modifier.glassSurface(tint = MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                due?.let { Text(it, style = MaterialTheme.typography.labelSmall,
                    color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}
