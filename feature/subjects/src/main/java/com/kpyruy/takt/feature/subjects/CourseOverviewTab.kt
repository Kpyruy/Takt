package com.kpyruy.takt.feature.subjects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.Course
import com.kpyruy.takt.core.model.CourseGradingType
import com.kpyruy.takt.core.model.CourseRequirementType
import com.kpyruy.takt.core.model.CourseStatus
import com.kpyruy.takt.core.model.ExamEligibility
import com.kpyruy.takt.core.model.GradeItem
import com.kpyruy.takt.core.model.GradeItemType
import com.kpyruy.takt.core.model.GradeProjection
import com.kpyruy.takt.core.model.GradeScale
import com.kpyruy.takt.core.model.GradeSummary
import com.kpyruy.takt.core.model.PassFailResult
import com.kpyruy.takt.core.model.StudyTask
import com.kpyruy.takt.core.ui.components.SectionCard
import com.kpyruy.takt.core.ui.components.StatusPill
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
internal fun CourseOverviewTab(
    course: Course,
    gradeItems: List<GradeItem>,
    gradeScale: GradeScale,
    tasks: List<StudyTask>,
    eligibility: ExamEligibility,
    onStatusChange: (CourseStatus) -> Unit,
    onGradingTypeChange: (CourseGradingType) -> Unit,
    onPassFailResultChange: (PassFailResult?) -> Unit,
) {
    val projection = GradeProjection.calculate(gradeItems, gradeScale)
    val summary = GradeSummary.calculate(gradeItems, gradeScale)
    val today = LocalDate.now()
    val upcomingTasks = tasks
        .filter { !it.completed && it.dueDate?.let { date -> !date.isBefore(today) } == true }
        .sortedBy { it.dueDate }
        .take(3)
    val upcomingAssessments = gradeItems
        .filter { !it.completed && it.dueDate?.let { date -> !date.isBefore(today) } == true }
        .sortedBy { it.dueDate }
        .take(3)

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Семестр " + course.semester, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(course.credits.toString() + " кредитів", style = MaterialTheme.typography.titleLarge)
                }
                StatusPill(text = course.status.label())
            }
            Text(
                when (course.gradingType) {
                    CourseGradingType.EXAM_LETTER -> {
                        val examAdded = gradeItems.any { it.type == GradeItemType.EXAM }
                        if (examAdded) {
                            val ceiling = projection.maximumPossibleLetter?.name ?: "—"
                            "Гарантовано " + projection.securedPoints.displayNumber() +
                                " балів · максимум " + ceiling
                        } else {
                            "Гарантовано " + projection.securedPoints.displayNumber() +
                                " балів · екзамен ще не додано"
                        }
                    }
                    CourseGradingType.CONTINUOUS_LETTER -> {
                        if (summary.maxPoints == 0.0) "Ще немає результатів"
                        else summary.earnedPoints.displayNumber() + " / " + summary.maxPoints.displayNumber() +
                            " · " + summary.percentage.displayNumber() + "%"
                    }
                    CourseGradingType.PASS_FAIL -> when (course.passFailResult) {
                        PassFailResult.PASSED -> "Зараховано"
                        PassFailResult.FAILED -> "Не зараховано"
                        null -> "Результату ще немає"
                    }
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (course.gradingType == CourseGradingType.EXAM_LETTER && eligibility.requiredCount > 0) {
                Text(
                    if (eligibility.eligible) "Допуск до екзамену: готово"
                    else "Для допуску: " + eligibility.completedCount + " / " + eligibility.requiredCount,
                    color = if (eligibility.eligible) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        SectionCard {
            Text("Статус предмета", style = MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(CourseStatus.entries) { status ->
                    FilterChip(
                        selected = course.status == status,
                        onClick = { onStatusChange(status) },
                        label = { Text(status.label()) },
                    )
                }
            }
        }

        SectionCard {
            Text("Тип оцінювання", style = MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(
                    listOf(
                        CourseGradingType.EXAM_LETTER,
                        CourseGradingType.CONTINUOUS_LETTER,
                        CourseGradingType.PASS_FAIL,
                    )
                ) { type ->
                    FilterChip(
                        selected = course.gradingType == type,
                        onClick = { onGradingTypeChange(type) },
                        label = { Text(type.label()) },
                    )
                }
            }
            if (course.gradingType == CourseGradingType.PASS_FAIL) {
                Text("Результат", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = course.passFailResult == null,
                            onClick = { onPassFailResultChange(null) },
                            label = { Text("Немає") },
                        )
                    }
                    item {
                        FilterChip(
                            selected = course.passFailResult == PassFailResult.PASSED,
                            onClick = { onPassFailResultChange(PassFailResult.PASSED) },
                            label = { Text("Зараховано") },
                        )
                    }
                    item {
                        FilterChip(
                            selected = course.passFailResult == PassFailResult.FAILED,
                            onClick = { onPassFailResultChange(PassFailResult.FAILED) },
                            label = { Text("Не зараховано") },
                        )
                    }
                }
            }
        }

        SectionCard {
            Text("Найближче", style = MaterialTheme.typography.titleMedium)
            if (upcomingTasks.isEmpty() && upcomingAssessments.isEmpty()) {
                Text("Немає найближчих дедлайнів.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                val formatter = DateTimeFormatter.ofPattern("dd.MM")
                upcomingAssessments.forEach { item ->
                    Text(
                        (item.dueDate?.format(formatter) ?: "—") + " · " +
                            item.title + " · до " + item.maxPoints.displayNumber() + " б."
                    )
                }
                upcomingTasks.forEach { task ->
                    Text(
                        (task.dueDate?.format(formatter) ?: "—") + " · " + task.title +
                            if (task.requiredForExam) " · для допуску" else ""
                    )
                }
            }
        }

        SectionCard {
            Text("Про предмет", style = MaterialTheme.typography.titleMedium)
            Text(
                when (course.requirementType) {
                    CourseRequirementType.COMPULSORY -> "Обов'язковий предмет"
                    CourseRequirementType.SEMI_COMPULSORY -> "Обов'язково-вибірковий предмет"
                    CourseRequirementType.ELECTIVE -> "Вибірковий предмет"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            course.syllabusUrl?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

internal fun CourseStatus.label(): String = when (this) {
    CourseStatus.FULFILLED -> "Закрито"
    CourseStatus.ENROLLED -> "Активний"
    CourseStatus.PLANNED -> "План"
    CourseStatus.NOT_ENROLLED -> "Не записаний"
    CourseStatus.NOT_NEEDED -> "Не потрібно"
}

internal fun CourseGradingType.label(): String = when (this) {
    CourseGradingType.EXAM_LETTER -> "Екзамен A–FX"
    CourseGradingType.CONTINUOUS_LETTER -> "Поточне A–FX"
    CourseGradingType.PASS_FAIL -> "Зараховано / ні"
}
