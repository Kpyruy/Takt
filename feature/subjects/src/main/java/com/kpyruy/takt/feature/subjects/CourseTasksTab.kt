package com.kpyruy.takt.feature.subjects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.ExamEligibility
import com.kpyruy.takt.core.model.StudyTask
import com.kpyruy.takt.core.ui.components.SectionCard

@Composable
internal fun CourseTasksTab(
    tasks: List<StudyTask>,
    eligibility: ExamEligibility,
    onCompletedChange: (StudyTask, Boolean) -> Unit,
    onEdit: (StudyTask) -> Unit,
    onDelete: (StudyTask) -> Unit,
    onAddTask: () -> Unit,
) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionCard {
            Text("Завдання", style = MaterialTheme.typography.titleMedium)
            Text(
                when {
                    eligibility.requiredCount == 0 -> "Немає окремих вимог для допуску."
                    eligibility.eligible -> "Допуск: готово · " +
                        eligibility.completedCount + " / " + eligibility.requiredCount
                    else -> "Для допуску: " +
                        eligibility.completedCount + " / " + eligibility.requiredCount
                },
                color = if (eligibility.eligible) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SectionCard {
            if (tasks.isEmpty()) {
                Text("Поки немає завдань.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                tasks.sortedWith(
                    compareBy<StudyTask> { it.completed }
                        .thenByDescending { it.requiredForExam }
                        .thenBy { it.dueDate }
                ).forEachIndexed { index, task ->
                    if (index > 0) HorizontalDivider()
                    StudyTaskRow(
                        task = task,
                        onCompletedChange = { completed -> onCompletedChange(task, completed) },
                        onEdit = { onEdit(task) },
                        onDelete = { onDelete(task) },
                    )
                }
            }
            OutlinedButton(onClick = onAddTask, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Додати завдання")
            }
        }
    }
}
