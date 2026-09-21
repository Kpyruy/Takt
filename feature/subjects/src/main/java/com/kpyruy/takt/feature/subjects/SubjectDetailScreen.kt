package com.kpyruy.takt.feature.subjects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.data.GradeRepository
import com.kpyruy.takt.core.data.StudyPlanRepository
import com.kpyruy.takt.core.model.CourseRequirementType
import com.kpyruy.takt.core.model.CourseStatus
import com.kpyruy.takt.core.model.GradeScale
import com.kpyruy.takt.core.model.GradeSummary
import com.kpyruy.takt.core.ui.components.ScreenHeader
import com.kpyruy.takt.core.ui.components.SectionCard
import com.kpyruy.takt.core.ui.components.StatusPill
import kotlinx.coroutines.launch

@Composable
fun SubjectDetailScreen(
    repository: StudyPlanRepository,
    gradeRepository: GradeRepository,
    courseId: String,
    onBack: () -> Unit,
) {
    val course by repository.observeCourse(courseId).collectAsState(initial = null)
    val gradeItems by gradeRepository.observeItems(courseId).collectAsState(initial = emptyList())
    val gradeScale by gradeRepository.observeScale(courseId).collectAsState(initial = GradeScale.default())
    val summary = remember(gradeItems, gradeScale) {
        GradeSummary.calculate(gradeItems, gradeScale)
    }
    val scope = rememberCoroutineScope()
    var showAddGrade by remember { mutableStateOf(false) }
    var showScaleEditor by remember { mutableStateOf(false) }

    val item = course
    if (item == null) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            ScreenHeader(
                title = "Предмет",
                action = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
            Text("Предмет не знайдено", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenHeader(
            title = item.title,
            subtitle = item.code,
            action = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                }
            },
        )

        SectionCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Семестр ${item.semester}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${item.credits} кредитів", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                StatusPill(
                    text = when (item.status) {
                        CourseStatus.FULFILLED -> "Закрито"
                        CourseStatus.ENROLLED -> "Активний"
                        CourseStatus.PLANNED -> "Заплановано"
                        CourseStatus.NOT_ENROLLED -> "Не записаний"
                        CourseStatus.NOT_NEEDED -> "Не потрібно"
                    }
                )
            }
        }

        SectionCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Оцінювання", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (summary.maxPoints > 0.0) {
                        Text(
                            "${summary.earnedPoints.compact()} / ${summary.maxPoints.compact()} балів",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "${summary.percentage.compact()}%",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text("Ще немає результатів", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                summary.letter?.let { StatusPill(it.name) }
            }

            if (gradeItems.isNotEmpty()) {
                gradeItems.forEachIndexed { index, gradeItem ->
                    if (index == 0) {
                        HorizontalDivider(modifier = Modifier.padding(top = 12.dp))
                    } else {
                        HorizontalDivider()
                    }
                    GradeItemRow(
                        item = gradeItem,
                        onDelete = {
                            scope.launch { gradeRepository.deleteItem(gradeItem.id) }
                        },
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = { showAddGrade = true },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("Додати")
                }
                OutlinedButton(
                    onClick = { showScaleEditor = true },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Шкала")
                }
            }
        }

        SectionCard {
            Text("Домашки та нотатки", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Немає активних завдань або нотаток.",
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionCard {
            Text("Про предмет", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = when (item.requirementType) {
                    CourseRequirementType.COMPULSORY -> "Обов'язковий предмет"
                    CourseRequirementType.SEMI_COMPULSORY -> "Обов'язково-вибірковий предмет"
                    CourseRequirementType.ELECTIVE -> "Вибірковий предмет"
                },
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                gradeScale.bands.joinToString(" · ") { band ->
                    "${band.grade} ≥ ${band.minimumPercentage.compact()}%"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showAddGrade) {
        AddGradeItemSheet(
            courseId = courseId,
            onDismiss = { showAddGrade = false },
            onSave = { gradeItem ->
                scope.launch {
                    gradeRepository.upsertItem(gradeItem)
                    showAddGrade = false
                }
            },
        )
    }

    if (showScaleEditor) {
        EditGradeScaleSheet(
            initialScale = gradeScale,
            onDismiss = { showScaleEditor = false },
            onSave = { scale ->
                scope.launch {
                    gradeRepository.setScale(courseId, scale)
                    showScaleEditor = false
                }
            },
        )
    }
}

private fun Double.compact(): String =
    if (this % 1.0 == 0.0) toInt().toString() else "%.1f".format(this)
