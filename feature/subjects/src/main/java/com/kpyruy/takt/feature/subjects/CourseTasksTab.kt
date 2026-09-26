package com.kpyruy.takt.feature.subjects

import com.kpyruy.takt.core.ui.i18n.t

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
import com.kpyruy.takt.core.model.CourseWork
import com.kpyruy.takt.core.model.GradeItem
import com.kpyruy.takt.core.model.StudyTask
import com.kpyruy.takt.core.ui.components.SectionCard

@Composable
internal fun CourseTasksTab(
    tasks: List<StudyTask>,
    gradeItems: List<GradeItem>,
    eligibility: ExamEligibility,
    onCompletedChange: (StudyTask, Boolean) -> Unit,
    onEdit: (StudyTask) -> Unit,
    onDelete: (StudyTask) -> Unit,
    onAddTask: () -> Unit,
    onEditGrade: (GradeItem) -> Unit,
    onDeleteGrade: (GradeItem) -> Unit,
    onAddGrade: () -> Unit,
) {
    val work = buildList {
        tasks.forEach { add(CourseWorkEntry.Task(it)) }
        CourseWork.actionable(gradeItems).forEach { add(CourseWorkEntry.Graded(it)) }
    }.sortedWith(compareBy<CourseWorkEntry> { it.completed }
        .thenByDescending { it.requiredForExam }
        .thenBy { it.dueDate })

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AdmissionProgressCard(eligibility)
        SectionCard {
            if (work.isEmpty()) {
                Text(t("Поки немає задач."), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                work.forEachIndexed { index, entry ->
                    if (index > 0) HorizontalDivider()
                    when (entry) {
                        is CourseWorkEntry.Task -> StudyTaskRow(
                            task = entry.value,
                            onCompletedChange = { completed -> onCompletedChange(entry.value, completed) },
                            onEdit = { onEdit(entry.value) },
                            onDelete = { onDelete(entry.value) },
                        )
                        is CourseWorkEntry.Graded -> CourseWorkGradeRow(
                            item = entry.value,
                            onEdit = { onEditGrade(entry.value) },
                            onDelete = { onDeleteGrade(entry.value) },
                        )
                    }
                }
            }
            OutlinedButton(onClick = onAddTask, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text(t("Додати завдання"))
            }
            OutlinedButton(onClick = onAddGrade, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text(t("Додати оцінювану роботу"))
            }
        }
    }
}

private sealed interface CourseWorkEntry {
    val completed: Boolean
    val requiredForExam: Boolean
    val dueDate: java.time.LocalDate?

    data class Task(val value: StudyTask) : CourseWorkEntry {
        override val completed get() = value.completed
        override val requiredForExam get() = value.requiredForExam
        override val dueDate get() = value.dueDate
    }

    data class Graded(val value: GradeItem) : CourseWorkEntry {
        override val completed get() = value.completed
        override val requiredForExam get() = value.requiredForExam
        override val dueDate get() = value.dueDate
    }
}
