package com.kpyruy.takt.feature.subjects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.data.GradeRepository
import com.kpyruy.takt.core.data.StudyContentRepository
import com.kpyruy.takt.core.data.StudyPlanRepository
import com.kpyruy.takt.core.model.Course
import com.kpyruy.takt.core.model.CourseGradingType
import com.kpyruy.takt.core.model.CourseStatus
import com.kpyruy.takt.core.model.ExamEligibilityCalculator
import com.kpyruy.takt.core.model.GradeProjection
import com.kpyruy.takt.core.model.GradeScale
import com.kpyruy.takt.core.model.GradeSummary
import com.kpyruy.takt.core.model.PassFailResult
import com.kpyruy.takt.core.ui.components.ScreenHeader

@Composable
fun SubjectsScreen(
    repository: StudyPlanRepository,
    gradeRepository: GradeRepository,
    studyContentRepository: StudyContentRepository,
    onCourseClick: (String) -> Unit,
) {
    val courses by repository.observeCourses().collectAsState(initial = emptyList())
    val current = remember(courses) { courses.filter { it.status == CourseStatus.ENROLLED } }
    val others = remember(courses) { courses.filterNot { it.status == CourseStatus.ENROLLED } }
    val groupedOthers = remember(others) { others.groupBy { it.semester }.toSortedMap() }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        ScreenHeader(
            title = "Предмети",
            subtitle = current.size.toString() + " активних · огляд семестру",
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (current.isNotEmpty()) {
                item { Text("Цей семестр", style = MaterialTheme.typography.titleLarge) }
                items(current, key = { "current-" + it.id }) { course ->
                    SubjectProgressCard(
                        course = course,
                        gradeRepository = gradeRepository,
                        studyContentRepository = studyContentRepository,
                        onClick = { onCourseClick(course.id) },
                    )
                }
            }

            groupedOthers.forEach { (semester, semesterCourses) ->
                item(key = "semester-title-" + semester) {
                    Text(
                        "Семестр " + semester,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                items(semesterCourses, key = { "other-" + it.id }) { course ->
                    SubjectProgressCard(
                        course = course,
                        gradeRepository = gradeRepository,
                        studyContentRepository = studyContentRepository,
                        onClick = { onCourseClick(course.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SubjectProgressCard(
    course: Course,
    gradeRepository: GradeRepository,
    studyContentRepository: StudyContentRepository,
    onClick: () -> Unit,
) {
    val gradeItems by gradeRepository.observeItems(course.id).collectAsState(initial = emptyList())
    val gradeScale by gradeRepository.observeScale(course.id).collectAsState(initial = GradeScale.default())
    val tasks by studyContentRepository.observeTasks(course.id).collectAsState(initial = emptyList())

    val progressLine = when (course.gradingType) {
        CourseGradingType.EXAM_LETTER -> {
            if (gradeItems.isEmpty()) {
                "Ще немає оцінюваних робіт"
            } else {
                val projection = GradeProjection.calculate(gradeItems, gradeScale)
                val ceiling = projection.maximumPossibleLetter?.name ?: "—"
                projection.securedPoints.compact() + " балів гарантовано · максимум " + ceiling
            }
        }
        CourseGradingType.CONTINUOUS_LETTER -> {
            if (gradeItems.isEmpty()) {
                "Ще немає результатів"
            } else {
                val summary = GradeSummary.calculate(gradeItems, gradeScale)
                summary.earnedPoints.compact() + " / " + summary.maxPoints.compact() +
                    " · " + summary.percentage.compact() + "% · " +
                    (summary.letter?.name ?: "—")
            }
        }
        CourseGradingType.PASS_FAIL -> when (course.passFailResult) {
            PassFailResult.PASSED -> "Зараховано"
            PassFailResult.FAILED -> "Не зараховано"
            null -> {
                val eligibility = ExamEligibilityCalculator.calculate(tasks, gradeItems)
                if (eligibility.requiredCount == 0) {
                    "Результату ще немає"
                } else {
                    "Обов'язкові роботи " + eligibility.completedCount + " / " + eligibility.requiredCount
                }
            }
        }
    }

    SubjectCard(course = course, progressLine = progressLine, onClick = onClick)
}

private fun Double.compact(): String =
    if (this % 1.0 == 0.0) toInt().toString() else "%.1f".format(this)
