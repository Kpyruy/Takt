package com.kpyruy.takt.feature.subjects

import com.kpyruy.takt.core.ui.i18n.t

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.kpyruy.takt.core.data.AppSettingsRepository
import com.kpyruy.takt.core.model.AppSettings
import androidx.compose.ui.platform.testTag
import androidx.compose.material.icons.outlined.Settings
import kotlinx.coroutines.flow.flowOf
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
import androidx.compose.material.icons.filled.Add

@Composable
fun SubjectsScreen(
    repository: StudyPlanRepository,
    gradeRepository: GradeRepository,
    studyContentRepository: StudyContentRepository,
    onCourseClick: (String) -> Unit,
    onAddCourse: () -> Unit,
    allowCourseCreation: Boolean = true,
    settingsRepository: AppSettingsRepository? = null,
    onOpenSettings: () -> Unit = {},
    showAddButton: Boolean = true,
) {
    val courses by remember(repository) { repository.observeCourses() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val settings by remember(settingsRepository) { settingsRepository?.settings ?: flowOf(AppSettings()) }
        .collectAsStateWithLifecycle(initialValue = AppSettings())
    val currentSemester = settings.effectiveCurrentSemester(courses)
    val visible = remember(courses, settings, currentSemester) {
        courses.filter { course ->
            course.status == CourseStatus.ENROLLED &&
                (currentSemester == null || settings.academicSemester(course, currentSemester) == currentSemester)
        }
    }
    Box(Modifier.fillMaxSize().testTag("subjects-screen")) {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp).padding(top = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            com.kpyruy.takt.core.ui.components.ScreenHeader(
                title = t("Предмети"), modifier = Modifier.testTag("subjects-header"),
                action = {
                    TaktIconButton(Icons.Outlined.Settings, t("Налаштування"), onClick = onOpenSettings,
                        modifier = Modifier.testTag("subjects-settings"))
                },
            )
            LazyColumn(Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(9.dp), contentPadding = PaddingValues(bottom = 120.dp)) {
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
        if (allowCourseCreation && showAddButton) TaktIconButton(Icons.Default.Add, t("Додати предмет"), onClick = onAddCourse,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 22.dp, bottom = 100.dp))
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
