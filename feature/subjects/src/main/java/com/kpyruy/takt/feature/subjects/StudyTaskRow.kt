package com.kpyruy.takt.feature.subjects

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
                        modifier = Modifier.semantics { contentDescription = t("Виконано: ${task.title}") })
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(task.title, style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold, maxLines = 2)
                        task.description?.takeIf { it.isNotBlank() }?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3)
                        }
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, t("Дії завдання")) }
                        TaktChoiceMenu(menuOpen, onDismissRequest = { menuOpen = false }) {
                            TaktChoiceMenuItem(text = { Text(t("Редагувати")) }, onClick = { menuOpen = false; onEdit() })
                            TaktChoiceMenuItem(text = { Text(t("Видалити")) }, onClick = { menuOpen = false; onDelete() })
                        }
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    task.dueDate?.let {
                        AssessmentDetailChip(t("До ") + it.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")), Icons.Outlined.CalendarMonth)
                    }
                    if (task.requiredForExam) AssessmentDetailChip(t("потрібно для допуску"), Icons.Outlined.Lock)
                    if (task.maxPoints != null) AssessmentDetailChip(t("Входить у підсумок"), Icons.Outlined.Description)
                    task.minimumPointsForExam?.let {
                        AssessmentDetailChip(t("Мінімум ") + it.displayNumber() + t(" б."), Icons.Outlined.BarChart)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                val resultColor = if (overdue || belowThreshold) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Icon(if (overdue || belowThreshold) Icons.Outlined.ErrorOutline else if (task.completed) Icons.Outlined.CheckCircle else Icons.Outlined.Schedule,
                        null, Modifier.size(18.dp), tint = resultColor)
                    Text(when {
                        overdue -> overdueLabel(task.dueDate!!, today)
                        belowThreshold -> t("Поріг не виконано")
                        task.completed -> t("Виконано")
                        else -> t("Очікується")
                    }, Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = resultColor)
                    task.maxPoints?.let { maximum ->
                        Text("${task.earnedPoints?.displayNumber() ?: "—"} / ${maximum.displayNumber()} ${t("б.")}",
                            style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
