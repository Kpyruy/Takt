package com.kpyruy.takt.feature.subjects

import com.kpyruy.takt.core.ui.i18n.t

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.ExamEligibility
import com.kpyruy.takt.core.model.CourseWork
import com.kpyruy.takt.core.model.GradeItem
import com.kpyruy.takt.core.model.StudyTask
import com.kpyruy.takt.core.ui.components.SectionCard
import com.kpyruy.takt.core.ui.components.GlassSurface
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
internal fun CourseTasksTab(
    tasks: List<StudyTask>,
    gradeItems: List<GradeItem>,
    eligibility: ExamEligibility,
    today: java.time.LocalDate,
    onCompletedChange: (StudyTask, Boolean) -> Unit,
    onEdit: (StudyTask) -> Unit,
    onDelete: (StudyTask) -> Unit,
    onAddTask: () -> Unit,
    onEditGrade: (GradeItem) -> Unit,
    onDeleteGrade: (GradeItem) -> Unit,
    onAddGrade: () -> Unit,
) {
    var addMenuOpen by remember { mutableStateOf(false) }
    val work = buildList {
        CourseWork.tasksNotRepresentedByAssessments(tasks, gradeItems)
            .forEach { add(CourseWorkEntry.Task(it)) }
        gradeItems.forEach { add(CourseWorkEntry.Graded(it)) }
    }.sortedWith(compareBy<CourseWorkEntry> { it.completed }
        .thenByDescending { it.requiredForExam }
        .thenBy { it.dueDate })

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AdmissionProgressCard(eligibility, compact = true)
        Text(t("Курсові задачі"), fontSize = 21.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp))
        if (work.isEmpty()) {
            SectionCard { Text(t("Поки немає задач."), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            work.forEach { entry ->
                when (entry) {
                    is CourseWorkEntry.Task -> {
                        StudyTaskRow(
                            task = entry.value,
                            today = today,
                            onCompletedChange = { completed -> onCompletedChange(entry.value, completed) },
                            onEdit = { onEdit(entry.value) },
                            onDelete = { onDelete(entry.value) },
                        )
                    }
                    is CourseWorkEntry.Graded -> GradeItemRow(
                        item = entry.value,
                        today = today,
                        onEdit = { onEditGrade(entry.value) },
                        onDelete = { onDeleteGrade(entry.value) },
                    )
                }
            }
        }
        Box(Modifier.fillMaxWidth()) {
            GlassSurface(Modifier.fillMaxWidth().clickable { addMenuOpen = true }, shape = RoundedCornerShape(22.dp)) {
                Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(t("Додати роботу"), color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium)
                }
            }
            DropdownMenu(expanded = addMenuOpen, onDismissRequest = { addMenuOpen = false }) {
                DropdownMenuItem(text = { Text(t("Завдання без балів")) }, onClick = {
                    addMenuOpen = false
                    onAddTask()
                })
                DropdownMenuItem(text = { Text(t("Оцінювана робота з балами")) }, onClick = {
                    addMenuOpen = false
                    onAddGrade()
                })
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
