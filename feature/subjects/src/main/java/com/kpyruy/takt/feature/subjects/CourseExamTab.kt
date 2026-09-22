package com.kpyruy.takt.feature.subjects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.ExamEligibility
import com.kpyruy.takt.core.model.ExamInfo
import com.kpyruy.takt.core.model.ExamMaterial
import com.kpyruy.takt.core.model.GradeItem
import com.kpyruy.takt.core.model.GradeItemType
import com.kpyruy.takt.core.model.GradeLetter
import com.kpyruy.takt.core.model.GradeProjection
import com.kpyruy.takt.core.model.GradeScale
import com.kpyruy.takt.core.model.StudyTask
import com.kpyruy.takt.core.ui.components.SectionCard
import com.kpyruy.takt.core.ui.components.TaktSegmentedTabs
import java.time.format.DateTimeFormatter

@Composable
internal fun CourseExamTab(
    courseId: String,
    gradeItems: List<GradeItem>,
    gradeScale: GradeScale,
    tasks: List<StudyTask>,
    eligibility: ExamEligibility,
    examInfo: ExamInfo?,
    materials: List<ExamMaterial>,
    manualGrade: GradeLetter?,
    onSaveExamInfo: (ExamInfo) -> Unit,
    onAddMaterial: (ExamMaterial) -> Unit,
    onDeleteMaterial: (ExamMaterial) -> Unit,
    onManualGradeChange: (GradeLetter?) -> Unit,
) {
    var mode by rememberSaveable { mutableIntStateOf(0) }
    var showEditor by remember { mutableStateOf(false) }
    val projection = remember(gradeItems, gradeScale) {
        GradeProjection.calculate(gradeItems, gradeScale)
    }

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TaktSegmentedTabs(
            labels = listOf("Підготовка", "Екзамен"),
            selectedIndex = mode,
            onSelected = { mode = it },
        )

        if (mode == 0) {
            SectionCard {
                Text("Готовність", style = MaterialTheme.typography.titleMedium)
                Text(
                    when {
                        eligibility.requiredCount == 0 -> "Окремих вимог для допуску не позначено."
                        eligibility.eligible -> "Допуск: готово · " +
                            eligibility.completedCount + " / " + eligibility.requiredCount
                        else -> "Для допуску: " +
                            eligibility.completedCount + " / " + eligibility.requiredCount
                    }
                )
                Text(
                    "Гарантовано: " + projection.securedPoints.displayNumber() + " б.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Максимально можлива оцінка: " +
                        (projection.maximumPossibleLetter?.name ?: "—"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val requiredTasks = tasks.filter { it.requiredForExam }
            val requiredGrades = gradeItems.filter { it.requiredForExam }
            SectionCard {
                Text("Що треба закрити", style = MaterialTheme.typography.titleMedium)
                if (requiredTasks.isEmpty() && requiredGrades.isEmpty()) {
                    Text("Обов'язкових робіт не позначено.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    requiredTasks.forEach { Text((if (it.completed) "✓ " else "○ ") + it.title) }
                    requiredGrades.forEach { Text((if (it.completed) "✓ " else "○ ") + it.title) }
                }
            }
        } else {
            ExamProgressCards(gradeItems = gradeItems, projection = projection)

            if (gradeItems.any { it.type == GradeItemType.EXAM }) {
                SectionCard {
                    Text("Скільки треба на екзамені", style = MaterialTheme.typography.titleMedium)
                    listOf(GradeLetter.A, GradeLetter.B, GradeLetter.C, GradeLetter.D, GradeLetter.E)
                        .forEach { grade ->
                            val needed = projection.examPointsNeeded[grade]
                            Text(
                                grade.name + " · " +
                                    if (needed == null) "Недосяжно" else needed.displayNumber() + " б."
                            )
                        }
                }
            }

            SectionCard {
                Text("Дані екзамену", style = MaterialTheme.typography.titleMedium)
                if (examInfo == null) {
                    Text(
                        "Дата, аудиторія та спроба ще не вказані.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        examInfo.date?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                            ?: "Дата ще не відома"
                    )
                    examInfo.startTime?.let { start ->
                        Text(start.toString() + (examInfo.endTime?.let { end -> "–" + end } ?: ""))
                    }
                    examInfo.room?.let { Text("Аудиторія · " + it) }
                    Text("Спроба · " + examInfo.attemptNumber + "/" + examInfo.maxAttempts)
                    if (examInfo.notes.isNotBlank()) {
                        Text(examInfo.notes, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Button(onClick = { showEditor = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Text(if (examInfo == null) "Додати дані" else "Редагувати")
                }
            }

            ExamMaterialsSection(
                courseId = courseId,
                materials = materials,
                onAdd = onAddMaterial,
                onDelete = onDeleteMaterial,
            )

            ManualGradeSection(
                manualGrade = manualGrade,
                onManualGradeChange = onManualGradeChange,
            )
        }
    }

    if (showEditor) {
        EditExamInfoSheet(
            courseId = courseId,
            initial = examInfo,
            gradeItems = gradeItems,
            onDismiss = { showEditor = false },
            onSave = {
                onSaveExamInfo(it)
                showEditor = false
            },
        )
    }
}
