package com.kpyruy.takt.feature.subjects

import com.kpyruy.takt.core.ui.i18n.t

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.sp
import com.kpyruy.takt.core.ui.components.TaktUnderlineTabs
import com.kpyruy.takt.core.ui.components.TaktIconButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.kpyruy.takt.core.model.GradeItemType
import com.kpyruy.takt.core.model.GradeProjection
import com.kpyruy.takt.core.model.GradeScale
import com.kpyruy.takt.core.model.GradeSummary
import com.kpyruy.takt.core.model.PassFailResult
import com.kpyruy.takt.core.model.CourseWork
import com.kpyruy.takt.core.ui.components.ScreenHeader

@Composable
fun SubjectsScreen(
    repository: StudyPlanRepository,
    gradeRepository: GradeRepository,
    studyContentRepository: StudyContentRepository,
    onCourseClick: (String) -> Unit,
    onAddCourse: () -> Unit,
    allowCourseCreation: Boolean = true,
) {
    val courses by remember(repository) { repository.observeCourses() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val current = remember(courses) { courses.filter { it.status == CourseStatus.ENROLLED } }
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableIntStateOf(0) }
    val visible = courses.filter { course ->
        (when (filter) { 0 -> course.status == CourseStatus.ENROLLED; 2 -> course.status == CourseStatus.FULFILLED; else -> true }) &&
            (course.title.contains(query, true) || course.code.contains(query, true))
    }
    Column(Modifier.fillMaxSize().background(subjectBackground()).padding(horizontal = 20.dp, vertical = 18.dp)) {
        ScreenHeader(title = t("Предмети"), subtitle = t("${current.map { it.semester }.distinct().singleOrNull()?.let { "$it семестр · " }.orEmpty()}${current.size} активні"),
            action = { if (allowCourseCreation) TextButton(onClick = onAddCourse) { Text(t("Додати")) } })
        OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
            placeholder = { Text(t("Знайти предмет"), fontSize = 12.sp, lineHeight = 16.sp) }, leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(20.dp)) },
            singleLine = true, shape = RoundedCornerShape(11.dp), textStyle = MaterialTheme.typography.bodySmall,
            colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = MaterialTheme.colorScheme.surface, focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant))
        TaktUnderlineTabs(labels = listOf(t("Активні · ${current.size}"), t("Усі"), t("Закриті")), selectedIndex = filter, onSelected = { filter = it })
        LazyColumn(contentPadding = PaddingValues(bottom = 80.dp)) {
            items(visible, key = { it.id }) { course ->
                SubjectProgressCard(course, gradeRepository, studyContentRepository) { onCourseClick(course.id) }
            }
            if (visible.isEmpty()) item {
                Column(Modifier.padding(vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SmallText(if (courses.isEmpty()) t("Тут будуть твої предмети") else t("Предметів не знайдено"))
                    if (courses.isEmpty() && allowCourseCreation) Button(onClick = onAddCourse) { Text(t("Додати перший предмет")) }
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
    val gradeItems by remember(gradeRepository, course.id) { gradeRepository.observeItems(course.id) }.collectAsStateWithLifecycle(initialValue = emptyList())
    val gradeScale by remember(gradeRepository, course.id) { gradeRepository.observeScale(course.id) }.collectAsStateWithLifecycle(initialValue = GradeScale.default())
    val tasks by remember(studyContentRepository, course.id) { studyContentRepository.observeTasks(course.id) }.collectAsStateWithLifecycle(initialValue = emptyList())

    val coursework = CourseWork.scoredItems(tasks, gradeItems)
        .filterNot { it.type == GradeItemType.EXAM }
    val earned = coursework.filter { it.completed }.sumOf { it.earnedPoints }
    val maximum = coursework.sumOf { it.maxPoints }
    val eligibility = ExamEligibilityCalculator.calculate(tasks, gradeItems)
    val next = (CourseWork.tasksNotRepresentedByAssessments(tasks, gradeItems)
        .filterNot { it.completed }.map { it.title to it.dueDate } +
        gradeItems.filter { !it.completed && it.type != GradeItemType.EXAM }.map { it.title to it.dueDate })
        .minWithOrNull(compareBy<Pair<String, java.time.LocalDate?>> { it.second ?: java.time.LocalDate.MAX }.thenBy { it.first })
    val progressLine = when {
        course.status == CourseStatus.FULFILLED -> t("Предмет закрито")
        !eligibility.eligible -> if (eligibility.requiredCount - eligibility.completedCount == 1) t("1 робота до допуску") else t("${eligibility.requiredCount - eligibility.completedCount} робіт до допуску")
        next != null -> next.first + (next.second?.let { " · " + it.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM")) } ?: "")
        course.gradingType == CourseGradingType.PASS_FAIL -> when (course.passFailResult) { PassFailResult.PASSED -> t("Зараховано"); PassFailResult.FAILED -> t("Не зараховано"); null -> "" }
        CourseWork.allSubmitted(tasks, gradeItems) -> t("Усі роботи здано")
        else -> ""
    }
    SubjectCard(course, progressLine, onClick, earned, maximum)
}

private fun Double.compact(): String =
    if (this % 1.0 == 0.0) toInt().toString() else "%.1f".format(this)
