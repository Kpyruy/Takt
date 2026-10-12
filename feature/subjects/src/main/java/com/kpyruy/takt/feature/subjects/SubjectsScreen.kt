package com.kpyruy.takt.feature.subjects

import com.kpyruy.takt.core.ui.i18n.t

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.data.GradeRepository
import com.kpyruy.takt.core.data.StudyContentRepository
import com.kpyruy.takt.core.data.StudyPlanRepository
import com.kpyruy.takt.core.model.Course
import com.kpyruy.takt.core.model.CourseGradingType
import com.kpyruy.takt.core.model.CourseStatus
import com.kpyruy.takt.core.model.ExamEligibilityCalculator
import com.kpyruy.takt.core.model.GradeItemType
import com.kpyruy.takt.core.model.PassFailResult
import com.kpyruy.takt.core.model.CourseWork
import com.kpyruy.takt.core.ui.components.ScreenHeader
import com.kpyruy.takt.core.ui.components.GlassSurface
import com.kpyruy.takt.core.ui.components.TaktTextField
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.text.font.FontWeight

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
    var filter by rememberSaveable { mutableIntStateOf(1) }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var filtersOpen by rememberSaveable { mutableStateOf(false) }
    var semesterOpen by remember { mutableStateOf(false) }
    var selectedSemester by rememberSaveable { mutableStateOf<Int?>(null) }
    // Null means an explicit choice of all semesters, distinct from the initial auto view.
    var allSemesters by rememberSaveable { mutableStateOf(false) }
    val semester = if (allSemesters) null else selectedSemester ?: current.map { it.semester }.distinct().singleOrNull()
    val visible = courses.filter { course ->
        (when (filter) { 0 -> course.status == CourseStatus.ENROLLED; 2 -> course.status == CourseStatus.FULFILLED; else -> true }) &&
            (semester == null || course.semester == semester) &&
            (course.title.contains(query, true) || course.code.contains(query, true))
    }
    Box(Modifier.fillMaxSize()) {
      Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(t("Предмети"), Modifier.weight(1f), fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold)
            TaktIconButton(Icons.Default.Search, t("Знайти предмет"), onClick = { searchOpen = !searchOpen })
            TaktIconButton(Icons.Default.Settings, t("Фільтри предметів"), onClick = { filtersOpen = !filtersOpen })
        }
        Box(Modifier.padding(top = 12.dp, bottom = 14.dp)) {
            GlassSurface(Modifier.clickable { semesterOpen = true }, shape = RoundedCornerShape(50)) {
                Row(Modifier.padding(horizontal = 17.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (semester != null) t("$semester семестр · ${if (semester % 2 == 1) "зимовий" else "літній"}") else t("Усі семестри"),
                        style = MaterialTheme.typography.titleMedium)
                    Icon(Icons.Default.ExpandMore, t("Обрати семестр"))
                }
            }
            DropdownMenu(expanded = semesterOpen, onDismissRequest = { semesterOpen = false }) {
                DropdownMenuItem(text = { Text(t("Усі семестри")) }, onClick = { selectedSemester = null; allSemesters = true; semesterOpen = false })
                courses.map { it.semester }.distinct().sorted().forEach { number ->
                    DropdownMenuItem(text = { Text(t("$number семестр")) }, onClick = { selectedSemester = number; allSemesters = false; semesterOpen = false })
                }
            }
        }
        if (searchOpen) {
            TaktTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(t("Знайти предмет")) }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true)
            Spacer(Modifier.height(12.dp))
        }
        if (filtersOpen) {
            TaktUnderlineTabs(labels = listOf(t("Активні · ${current.size}"), t("Усі"), t("Закриті")), selectedIndex = filter, onSelected = { filter = it })
            Spacer(Modifier.height(12.dp))
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp), contentPadding = PaddingValues(bottom = 100.dp)) {
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
      if (allowCourseCreation) TaktIconButton(Icons.Default.Add, t("Додати предмет"), onClick = onAddCourse,
          modifier = Modifier.align(Alignment.BottomEnd).padding(end = 22.dp, bottom = 20.dp))
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
    val tasks by remember(studyContentRepository, course.id) { studyContentRepository.observeTasks(course.id) }.collectAsStateWithLifecycle(initialValue = emptyList())
    val manualGrade by remember(gradeRepository, course.id) { gradeRepository.observeManualGrade(course.id) }.collectAsStateWithLifecycle(initialValue = null)

    val coursework = CourseWork.scoredItems(tasks, gradeItems)
        .filterNot { it.type == GradeItemType.EXAM }
    val earned = coursework.filter { it.completed }.sumOf { it.earnedPoints }
    val maximum = coursework.sumOf { it.maxPoints }
    val eligibility = ExamEligibilityCalculator.calculate(tasks, gradeItems)
    val next = (CourseWork.tasksNotRepresentedByAssessments(tasks, gradeItems)
        .filterNot { it.completed }.map { it.title to it.dueDate } +
        gradeItems.filter { !it.isSubmitted && it.type != GradeItemType.EXAM }.map { it.title to it.dueDate })
        .minWithOrNull(compareBy<Pair<String, java.time.LocalDate?>> { it.second ?: java.time.LocalDate.MAX }.thenBy { it.first })
    val progressLine = when {
        course.status == CourseStatus.FULFILLED -> ""
        !eligibility.eligible -> if (eligibility.requiredCount - eligibility.completedCount == 1) t("1 робота до допуску") else t("${eligibility.requiredCount - eligibility.completedCount} робіт до допуску")
        next != null -> next.first + (next.second?.let { " · " + it.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM")) } ?: "")
        course.gradingType == CourseGradingType.PASS_FAIL -> when (course.passFailResult) { PassFailResult.PASSED -> t("Зараховано"); PassFailResult.FAILED -> t("Не зараховано"); null -> "" }
        CourseWork.allSubmitted(tasks, gradeItems) -> t("Усі роботи здано")
        else -> ""
    }
    SubjectCard(course, progressLine, onClick, earned, maximum, grade = course.officialGrade ?: manualGrade)
}

private fun Double.compact(): String =
    if (this % 1.0 == 0.0) toInt().toString() else "%.1f".format(this)
