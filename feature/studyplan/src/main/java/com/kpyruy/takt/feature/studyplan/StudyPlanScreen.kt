package com.kpyruy.takt.feature.studyplan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.kpyruy.takt.core.model.Course
import com.kpyruy.takt.core.ui.components.CourseIconPicker
import com.kpyruy.takt.core.ui.components.TaktUnderlineTabs
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kpyruy.takt.core.data.StudyContentRepository
import com.kpyruy.takt.core.data.GradeRepository
import com.kpyruy.takt.core.model.ExamEligibilityCalculator
import com.kpyruy.takt.core.data.StudyPlanRepository
import com.kpyruy.takt.core.model.CourseStatus
import com.kpyruy.takt.core.ui.components.ScreenHeader
import com.kpyruy.takt.core.ui.components.TaktIconButton

@Composable
fun StudyPlanScreen(
    repository: StudyPlanRepository,
    studyContentRepository: StudyContentRepository,
    gradeRepository: GradeRepository,
    onCourseClick: (String) -> Unit,
    onTaskClick: (String) -> Unit = onCourseClick,
    onAssessmentClick: (String) -> Unit = onCourseClick,
) {
    val courses by repository.observeCourses().collectAsState(initial = emptyList())
    val tasks by studyContentRepository.observeAllTasks().collectAsState(initial = emptyList())
    val grades by gradeRepository.observeRecentItems(Int.MAX_VALUE).collectAsState(initial = emptyList())
    val earned = courses.filter { it.status == CourseStatus.FULFILLED }.sumOf { it.credits }
    val semesters = courses.groupBy { it.semester }.toSortedMap()
    val largeText = LocalDensity.current.fontScale > 1.2f
    val completedSemesters = semesters.values.count { semesterState(it) == SemesterState.COMPLETED }
    var expandedSemesters by rememberSaveable { mutableStateOf(emptySet<Int>()) }
    var filter by rememberSaveable { mutableIntStateOf(0) }
    var editingCourseId by rememberSaveable { mutableStateOf<String?>(null) }
    var iconCourseId by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val activeCourses = courses.filter { it.status == CourseStatus.ENROLLED }
    val completedCourses = courses.filter { it.status == CourseStatus.FULFILLED }
    var menuExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).testTag("progress-screen"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
    ) {
        item {
            ScreenHeader(
                title = "Твій шлях",
                subtitle = "Бакалаврат · B-PIAR",
                action = {
                    Box {
                        TaktIconButton(
                            icon = Icons.Default.MoreHoriz,
                            contentDescription = "Керування навчальним планом",
                            onClick = { menuExpanded = true },
                        )
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(text = { Text("Розгорнути весь план") }, onClick = {
                                expandedSemesters = semesters.keys.toSet()
                                menuExpanded = false
                            })
                            DropdownMenuItem(text = { Text("Згорнути всі семестри") }, onClick = {
                                expandedSemesters = emptySet()
                                menuExpanded = false
                            })
                        }
                    }
                },
            )
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.Bottom) {
                    Text(earned.toString(), style = MaterialTheme.typography.headlineLarge.copy(fontSize = 40.sp, lineHeight = 52.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1.9).sp))
                    Text(" / 180 кредитів", modifier = Modifier.padding(bottom = 5.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!largeText) Text(
                    "$completedSemesters із ${semesters.size} завершено",
                    Modifier.background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(7.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (largeText) Text("$completedSemesters із ${semesters.size} семестрів завершено", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            LinearProgressIndicator(
                progress = { (earned / 180f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(5.dp),
                gapSize = 0.dp,
                drawStopIndicator = {},
                trackColor = MaterialTheme.colorScheme.outlineVariant,
            )
            Spacer(Modifier.height(16.dp))
            TaktUnderlineTabs(listOf("План", "Активні · ${activeCourses.size}", "Здані · ${completedCourses.size}"), filter, { filter = it })
            Text(if (filter == 0) "Розгорни семестр · натисни статус, щоб змінити" else "Предмети з усіх семестрів · натисни статус, щоб змінити",
                Modifier.padding(top = 10.dp, bottom = 18.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (filter != 0) {
            val filtered = if (filter == 1) activeCourses else completedCourses
            items(filtered, key = { it.id }) { course ->
                ProgressCourseRow(course, onOpen = { onCourseClick(course.id) }, onStatus = { editingCourseId = course.id })
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            if (filtered.isEmpty()) item { Text(if (filter == 1) "Активних предметів ще немає. Обери їх у плані." else "Зданих предметів ще немає.", style = MaterialTheme.typography.bodySmall) }
        }
        if (filter == 0) semesters.forEach { (semester, semesterCourses) ->
            item(key = "semester-$semester") {
                val activeCourseIds = semesterCourses.filter { it.status == CourseStatus.ENROLLED }.map { it.id }.toSet()
                val pendingTask = tasks.filter { it.courseId in activeCourseIds && it.requiredForExam && !it.completed }
                    .sortedWith(compareBy<com.kpyruy.takt.core.model.StudyTask> { it.dueDate == null }.thenBy { it.dueDate }.thenBy { it.title }).firstOrNull()
                val pendingAssessment = grades.filter { it.courseId in activeCourseIds && it.requiredForExam && !it.completed }.sortedBy { it.dueDate ?: java.time.LocalDate.MAX }.firstOrNull()
                val taskComesFirst = pendingTask != null && (pendingAssessment == null ||
                    (pendingTask.dueDate ?: java.time.LocalDate.MAX) <= (pendingAssessment.dueDate ?: java.time.LocalDate.MAX))
                val nextTask = pendingTask.takeIf { taskComesFirst }
                val nextAssessment = pendingAssessment.takeUnless { taskComesFirst }
                val nextCourseId = nextTask?.courseId ?: nextAssessment?.courseId
                SemesterSection(
                    semester = semester,
                    courses = semesterCourses,
                    expanded = semester in expandedSemesters,
                    onExpandedChange = { expanded -> expandedSemesters = if (expanded) expandedSemesters + semester else expandedSemesters - semester },
                    onCourseClick = onCourseClick,
                    onCourseStatus = { editingCourseId = it.id },
                    nextAction = {
                        nextCourseId?.let { courseId ->
                            val course = semesterCourses.first { it.id == courseId }
                            val eligibility = ExamEligibilityCalculator.calculate(tasks.filter { it.courseId == courseId }, grades.filter { it.courseId == courseId })
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(top = 10.dp).clickable { if (nextTask != null) onTaskClick(course.id) else onAssessmentClick(course.id) },
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.background,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            ) {
                                Column(Modifier.padding(13.dp)) {
                                    Text("Найближчий крок", style = MaterialTheme.typography.titleMedium)
                                    Text("Допуск з ${course.title}", Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Row(Modifier.fillMaxWidth().padding(top = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("${eligibility.completedCount} із ${eligibility.requiredCount} робіт виконано", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Відкрити завдання: ${nextTask?.title ?: nextAssessment?.title.orEmpty()}", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    },
                )
            }
        }
        item {
            Text(if (earned >= 180) "Ціль у 180 кредитів досягнуто." else "До цілі — ще ${180 - earned} кредитів.", Modifier.padding(top = 2.dp, bottom = 8.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    courses.firstOrNull { it.id == editingCourseId }?.let { course ->
        CourseStatusSheet(course, onStatus = { status ->
            scope.launch { repository.updateStatus(course.id, status) }
            editingCourseId = null
        }, onIcon = { iconCourseId = course.id; editingCourseId = null }, onDismiss = { editingCourseId = null })
    }
    courses.firstOrNull { it.id == iconCourseId }?.let { course ->
        CourseIconPicker(course.iconKey, onSelect = { key ->
            scope.launch { repository.setIcon(course.id, key) }
            iconCourseId = null
        }, onDismiss = { iconCourseId = null })
    }

}
