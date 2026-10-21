package com.kpyruy.takt.feature.subjects

import com.kpyruy.takt.core.model.uiMessage
import com.kpyruy.takt.core.model.UiKey
import com.kpyruy.takt.core.ui.components.TaktChoiceMenu
import com.kpyruy.takt.core.ui.components.TaktChoiceMenuItem
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.StudyTask
import com.kpyruy.takt.core.ui.components.GlassSurface
import com.kpyruy.takt.core.ui.components.SwipeCompletionBox
import com.kpyruy.takt.core.ui.i18n.t
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun StudyTaskRow(
    task: StudyTask,
    onCompletedChange: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    today: LocalDate = LocalDate.now(),
) {
    val overdue = !task.completed && task.dueDate?.isBefore(today) == true
    val belowThreshold = task.completed && task.requiredForExam && !task.meetsAdmissionRequirement
    var menuOpen by remember { mutableStateOf(false) }
    SwipeCompletionBox(completed = task.completed, onCompletedChange = onCompletedChange) {
        GlassSurface(Modifier.fillMaxWidth().animateContentSize().testTag("course-work-${task.id}")
            .clickable(onClick = onEdit), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Checkbox(checked = task.completed, onCheckedChange = onCompletedChange,
                        modifier = Modifier.semantics { contentDescription = t(uiMessage(UiKey.FORMAT_COMPLETED_VALUE, task.title)) })
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(task.title, style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold, maxLines = 2)
                        task.description?.takeIf { it.isNotBlank() }?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3)
                        }
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, t(UiKey.TEXT_TASK_ACTIONS)) }
                        TaktChoiceMenu(menuOpen, onDismissRequest = { menuOpen = false }) {
                            TaktChoiceMenuItem(text = { Text(t(UiKey.TEXT_EDIT)) }, onClick = { menuOpen = false; onEdit() })
                            TaktChoiceMenuItem(text = { Text(t(UiKey.TEXT_DELETE)) }, onClick = { menuOpen = false; onDelete() })
                        }
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    task.dueDate?.let {
                        AssessmentDetailChip(t(UiKey.TEXT_DUE) + it.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")), Icons.Outlined.CalendarMonth)
                    }
                    if (task.requiredForExam) AssessmentDetailChip(t(UiKey.TEXT_REQUIRED_FOR_EXAM_ELIGIBILITY_5E3B44BA), Icons.Outlined.Lock)
                    if (task.maxPoints != null) AssessmentDetailChip(t(UiKey.TEXT_COUNTS_TOWARD_THE_FINAL_GRADE), Icons.Outlined.Description)
                    task.minimumPointsForExam?.let {
                        AssessmentDetailChip(t(UiKey.TEXT_MINIMUM_3E72FBEC) + it.displayNumber() + t(UiKey.TEXT_PTS_77846466), Icons.Outlined.BarChart)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                val resultColor = if (overdue || belowThreshold) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Icon(if (overdue || belowThreshold) Icons.Outlined.ErrorOutline else if (task.completed) Icons.Outlined.CheckCircle else Icons.Outlined.Schedule,
                        null, Modifier.size(18.dp), tint = resultColor)
                    Text(when {
                        overdue -> overdueLabel(task.dueDate!!, today)
                        belowThreshold -> t(UiKey.TEXT_THRESHOLD_NOT_MET)
                        task.completed -> t(UiKey.TEXT_WORK_DONE)
                        else -> t(UiKey.TEXT_PENDING)
                    }, Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = resultColor)
                    task.maxPoints?.let { maximum ->
                        Text("${task.earnedPoints?.displayNumber() ?: "—"} / ${maximum.displayNumber()} ${t(UiKey.TEXT_PTS)}",
                            style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
