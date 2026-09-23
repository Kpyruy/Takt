package com.kpyruy.takt.feature.subjects

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.kpyruy.takt.core.model.GradeItemType
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
    onAdd: () -> Unit,
) {
    val courses by repository.observeCourses().collectAsState(initial = emptyList())
    val current = remember(courses) { courses.filter { it.status == CourseStatus.ENROLLED } }
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableIntStateOf(0) }
    val visible = courses.filter { course ->
        (when (filter) { 0 -> course.status == CourseStatus.ENROLLED; 2 -> course.status == CourseStatus.FULFILLED; else -> true }) &&
            (course.title.contains(query, true) || course.code.contains(query, true))
    }
    Column(Modifier.fillMaxSize().background(subjectBackground()).padding(horizontal = 20.dp, vertical = 18.dp)) {
        ScreenHeader(title = "Предмети", subtitle = "${current.map { it.semester }.distinct().singleOrNull()?.let { "$it семестр · " }.orEmpty()}${current.size} активні",
            action = { TaktIconButton(Icons.Default.Add, "Додати", onAdd) })
        OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
            placeholder = { Text("Знайти предмет", fontSize = 12.sp, lineHeight = 16.sp) }, leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(20.dp)) },
            singleLine = true, shape = RoundedCornerShape(11.dp), textStyle = MaterialTheme.typography.bodySmall,
            colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = MaterialTheme.colorScheme.surface, focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant))
        TaktUnderlineTabs(labels = listOf("Активні · ${current.size}", "Усі", "Закриті"), selectedIndex = filter, onSelected = { filter = it })
        LazyColumn {
            items(visible, key = { it.id }) { course ->
                SubjectProgressCard(course, gradeRepository, studyContentRepository) { onCourseClick(course.id) }
            }
            if (visible.isEmpty()) item { SmallText("Предметів не знайдено", Modifier.padding(vertical = 24.dp)) }
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

    val coursework = gradeItems.filterNot { it.type == GradeItemType.EXAM }
    val earned = coursework.filter { it.completed }.sumOf { it.earnedPoints }
    val maximum = coursework.sumOf { it.maxPoints }
    val eligibility = ExamEligibilityCalculator.calculate(tasks, gradeItems)
    val next = tasks.filterNot { it.completed }.sortedBy { it.dueDate ?: java.time.LocalDate.MAX }.firstOrNull()
    val progressLine = when {
        course.status == CourseStatus.FULFILLED -> "Предмет закрито"
        !eligibility.eligible -> if (eligibility.requiredCount - eligibility.completedCount == 1) "1 робота до допуску" else "${eligibility.requiredCount - eligibility.completedCount} робіт до допуску"
        next != null -> next.title + (next.dueDate?.let { " · " + it.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM")) } ?: "")
        course.gradingType == CourseGradingType.PASS_FAIL -> when (course.passFailResult) { PassFailResult.PASSED -> "Зараховано"; PassFailResult.FAILED -> "Не зараховано"; null -> "Результату ще немає" }
        tasks.isNotEmpty() -> "Усі роботи здано"
        else -> course.code + " · " + course.credits + " кредитів"
    }
    SubjectCard(course, progressLine, onClick, earned, maximum)
}

private fun Double.compact(): String =
    if (this % 1.0 == 0.0) toInt().toString() else "%.1f".format(this)
