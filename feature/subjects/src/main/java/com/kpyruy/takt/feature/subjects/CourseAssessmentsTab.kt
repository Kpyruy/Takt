package com.kpyruy.takt.feature.subjects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.Course
import com.kpyruy.takt.core.model.AssessmentPhase
import com.kpyruy.takt.core.model.CourseGradingType
import com.kpyruy.takt.core.model.ExamEligibility
import com.kpyruy.takt.core.model.GradeItem
import com.kpyruy.takt.core.model.GradeItemType
import com.kpyruy.takt.core.model.GradeLetter
import com.kpyruy.takt.core.model.GradeProjection
import com.kpyruy.takt.core.model.GradeScale
import com.kpyruy.takt.core.model.GradeSummary
import com.kpyruy.takt.core.model.StudyTask
import com.kpyruy.takt.core.model.asScoredGradeItem
import com.kpyruy.takt.core.model.forPhase
import com.kpyruy.takt.core.ui.components.CompactSummaryItem
import com.kpyruy.takt.core.ui.components.CompactSummaryStrip
import com.kpyruy.takt.core.ui.components.SectionCard

@Composable
internal fun CourseAssessmentsTab(
    course: Course,
    phase: AssessmentPhase,
    gradeItems: List<GradeItem>,
    tasks: List<StudyTask>,
    gradeScale: GradeScale,
    eligibility: ExamEligibility,
    manualGrade: GradeLetter?,
    onAddGrade: () -> Unit,
    onEditGrade: (GradeItem) -> Unit,
    onEditTask: (StudyTask) -> Unit,
    onDeleteGrade: (String) -> Unit,
    onEditScale: () -> Unit,
    onManualGradeChange: (GradeLetter?) -> Unit,
) {
    var showLegend by remember { mutableStateOf(false) }
    val allGradedWork = gradeItems + tasks.mapNotNull { it.asScoredGradeItem() }
    val scoredTasks = tasks.filter { it.maxPoints != null }
    val projection = GradeProjection.calculate(allGradedWork, gradeScale)
    val summary = GradeSummary.calculate(allGradedWork, gradeScale, manualGrade)
    val displayType = course.gradingType.forPhase(phase)

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (displayType != CourseGradingType.PASS_FAIL) {
            Text(if (phase == AssessmentPhase.EXAM) "ЕКЗАМЕНАЦІЙНЕ ОЦІНЮВАННЯ" else "ОЦІНЮВАННЯ ПІД ЧАС НАВЧАННЯ",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
        when (displayType) {
            CourseGradingType.EXAM_LETTER -> {
                CompactSummaryStrip(
                    items = listOf(
                        CompactSummaryItem(
                            value = if (eligibility.requiredCount == 0) {
                                "—"
                            } else {
                                eligibility.completedCount.toString() + "/" + eligibility.requiredCount
                            },
                            label = "робіт для допуску",
                        ),
                        CompactSummaryItem(
                            value = projection.securedPoints.displayNumber(),
                            label = "балів гарантовано",
                        ),
                    )
                )

                SectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("До екзамену", style = MaterialTheme.typography.titleMedium)
                            Text(
                                projection.securedPoints.displayNumber() + " балів уже зафіксовано",
                                style = MaterialTheme.typography.titleLarge,
                            )
                            val examAdded = gradeItems.any { it.type == GradeItemType.EXAM }
                            Text(
                                text = if (examAdded) {
                                    "Максимально можлива оцінка зараз: " +
                                        (projection.maximumPossibleLetter?.name ?: "—")
                                } else {
                                    "Додай екзамен, щоб побачити максимальну можливу оцінку."
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { showLegend = true }) {
                            Icon(Icons.Default.Info, contentDescription = "Шкала оцінювання")
                        }
                    }
                }

                val upcoming = gradeItems
                    .filter { !it.completed }
                    .sortedBy { it.dueDate }
                    .take(3)
                if (upcoming.isNotEmpty()) {
                    SectionCard {
                        Text("Найближче", style = MaterialTheme.typography.titleMedium)
                        upcoming.forEach { item ->
                            Text(
                                item.title + " · до " + item.maxPoints.displayNumber() + " б.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            CourseGradingType.CONTINUOUS_LETTER -> SectionCard {
                Text("Поточний результат", style = MaterialTheme.typography.titleMedium)
                if (summary.maxPoints > 0.0) {
                    Text(
                        summary.earnedPoints.displayNumber() + " / " + summary.maxPoints.displayNumber() +
                            " · " + summary.percentage.displayNumber() + "%",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        "Оцінка: " + (summary.letter?.name ?: "—"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text("Ще немає результатів", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            CourseGradingType.PASS_FAIL -> SectionCard {
                Text("Без A–FX", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Цей предмет оцінюється як «зараховано / не зараховано».",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (course.gradingType != CourseGradingType.PASS_FAIL) {
            SectionCard {
                Text("Оцінювані роботи", style = MaterialTheme.typography.titleMedium)
                if (gradeItems.isEmpty() && scoredTasks.isEmpty()) {
                    Text(
                        "Поки немає тестів, робіт або екзамену.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    gradeItems.sortedWith(
                        compareBy<GradeItem> { it.completed }.thenBy { it.dueDate }
                    ).forEachIndexed { index, item ->
                        if (index > 0) HorizontalDivider()
                        GradeItemRow(
                            item = item,
                            onEdit = { onEditGrade(item) },
                            onDelete = { onDeleteGrade(item.id) },
                        )
                    }
                    if (scoredTasks.isNotEmpty()) {
                        if (gradeItems.isNotEmpty()) HorizontalDivider()
                        Text("Завдання з балами", style = MaterialTheme.typography.titleSmall)
                        scoredTasks.forEach { task ->
                            val maximum = task.maxPoints ?: return@forEach
                            Row(Modifier.fillMaxWidth().clickable { onEditTask(task) }.padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically) {
                                Text(task.title, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                Text("${task.earnedPoints?.displayNumber() ?: "—"} / ${maximum.displayNumber()}",
                                    style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(onClick = onAddGrade, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("Додати")
                    }
                    OutlinedButton(onClick = onEditScale, modifier = Modifier.weight(1f)) {
                        Text("Шкала")
                    }
                }
            }
        }

        if (displayType == CourseGradingType.CONTINUOUS_LETTER) {
            ManualGradeSection(manualGrade = manualGrade, onManualGradeChange = onManualGradeChange)
        }
    }

    if (showLegend) {
        AlertDialog(
            onDismissRequest = { showLegend = false },
            confirmButton = {
                TextButton(onClick = { showLegend = false }) { Text("Готово") }
            },
            title = { Text("Шкала предмета") },
            text = {
                Text(
                    gradeScale.bands.joinToString("\n") { band ->
                        band.grade.name + " · від " + band.minimumPercentage.displayNumber() + "%"
                    }
                )
            },
        )
    }
}

@Composable
internal fun ManualGradeSection(
    manualGrade: GradeLetter?,
    onManualGradeChange: (GradeLetter?) -> Unit,
) {
    SectionCard {
        Text("Підсумкова оцінка вручну", style = MaterialTheme.typography.titleMedium)
        Text(
            "Використовуй тільки коли підсумкова оцінка вже офіційно відома.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FilterChip(
            selected = manualGrade == null,
            onClick = { onManualGradeChange(null) },
            label = { Text("Авто") },
        )
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(GradeLetter.entries.size) { index ->
                val grade = GradeLetter.entries[index]
                FilterChip(
                    selected = manualGrade == grade,
                    onClick = { onManualGradeChange(grade) },
                    label = { Text(grade.name) },
                )
            }
        }
        if (manualGrade != null) {
            Text("Вручну: " + manualGrade.name, color = MaterialTheme.colorScheme.primary)
        }
    }
}
