package com.kpyruy.takt.feature.studyplan

import com.kpyruy.takt.core.ui.components.TaktChoiceMenu
import com.kpyruy.takt.core.ui.components.TaktChoiceMenuItem
import com.kpyruy.takt.core.ui.i18n.t

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.kpyruy.takt.core.ui.components.CourseIconPicker
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.ExpandMore
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
import com.kpyruy.takt.core.data.StudyPlanRepository
import com.kpyruy.takt.core.data.GradeRepository
import com.kpyruy.takt.core.data.UniversityAccountRepository
import com.kpyruy.takt.core.data.AppSettingsRepository
import com.kpyruy.takt.core.model.AppSettings
import com.kpyruy.takt.core.model.CourseStatus
import com.kpyruy.takt.core.ui.components.ScreenHeader
import com.kpyruy.takt.core.ui.components.TaktIconButton
import com.kpyruy.takt.core.ui.components.GlassSurface
import com.kpyruy.takt.core.ui.components.TaktSegmentedTabs
import androidx.compose.material.icons.filled.School
import androidx.compose.foundation.clickable

@Composable
fun StudyPlanScreen(
    repository: StudyPlanRepository,
    gradeRepository: GradeRepository,
    settingsRepository: AppSettingsRepository,
    universityAccountRepository: UniversityAccountRepository,
    onCourseClick: (String) -> Unit,
    onAddCourse: () -> Unit,
) {
    val courses by remember(repository) { repository.observeCourses() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val settings by settingsRepository.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    val earned = remember(courses) { courses.filter { it.status == CourseStatus.FULFILLED }.sumOf { it.credits } }
    val plannedCredits = remember(courses) { courses.filter { it.status == CourseStatus.FULFILLED ||
        it.status == CourseStatus.ENROLLED || it.status == CourseStatus.PLANNED }.sumOf { it.credits } }
    val uisProgress by universityAccountRepository.importProgress.collectAsStateWithLifecycle()
    val uisAccount by universityAccountRepository.hasAccount.collectAsStateWithLifecycle()
    val officialCredits = uisProgress.earnedCredits != null && uisProgress.requiredCredits != null
    val displayedEarned = if (officialCredits) uisProgress.earnedCredits!! else earned
    val displayedTotal = if (officialCredits) uisProgress.requiredCredits!! else plannedCredits
    val semesters = remember(courses) { courses.groupBy { it.semester }.toSortedMap() }
    val currentSemester = settings.effectiveCurrentSemester(courses)
    val largeText = LocalDensity.current.fontScale > 1.2f
    val completedSemesters = remember(semesters, currentSemester) {
        semesters.count { (semester, items) -> semesterState(items, semester == currentSemester) == SemesterState.COMPLETED }
    }
    var expandedSemesters by rememberSaveable(currentSemester) { mutableStateOf(currentSemester?.let(::setOf) ?: emptySet()) }
    var filter by rememberSaveable { mutableIntStateOf(0) }
    var editingCourseId by rememberSaveable { mutableStateOf<String?>(null) }
    var iconCourseId by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val activeCourses = remember(courses) { courses.filter { it.status == CourseStatus.ENROLLED } }
    val completedCourses = remember(courses) { courses.filter { it.status == CourseStatus.FULFILLED } }
    var menuExpanded by remember { mutableStateOf(false) }
    var selectingSemester by remember { mutableStateOf(false) }
    var searchOpen by rememberSaveable { mutableStateOf(false) }

    if (courses.isEmpty()) {
        Column(Modifier.fillMaxSize()
            .padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ScreenHeader(title = t("Твій шлях"))
            Text(t(if (uisAccount) "Предмети завантажуються з UIS." else "Додай перший предмет, щоб бачити свій прогрес."),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!uisAccount) Button(onClick = onAddCourse) { Text(t("Додати предмет")) }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("progress-screen"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 96.dp),
    ) {
        item {
            ScreenHeader(
                title = t("Твій шлях"),
                subtitle = t("Навчальний план"),
                action = {
                    Box {
                        TaktIconButton(
                            icon = Icons.Default.MoreHoriz,
                            contentDescription = t("Керування навчальним планом"),
                            onClick = { menuExpanded = true },
                            modifier = Modifier.testTag("progress-menu"),
                        )
                        TaktChoiceMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            TaktChoiceMenuItem(
                                text = { Text(t("Знайти предмет")) },
                                modifier = Modifier.testTag("progress-search-open"),
                                onClick = { menuExpanded = false; searchOpen = true },
                            )
                            TaktChoiceMenuItem(text = { Text(t("Розгорнути весь план")) }, onClick = {
                                expandedSemesters = semesters.keys.toSet()
                                menuExpanded = false
                            })
                            TaktChoiceMenuItem(text = { Text(t("Згорнути всі семестри")) }, onClick = {
                                expandedSemesters = emptySet()
                                menuExpanded = false
                            })
                        }
                    }
                },
            )
            Spacer(Modifier.height(12.dp))
            GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(23.dp)) {
                BoxWithConstraints(Modifier.fillMaxWidth().padding(16.dp)) {
                    val stackSemesterControl = largeText || maxWidth < 288.dp
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Row(Modifier.weight(1f), verticalAlignment = Alignment.Bottom) {
                                Text(displayedEarned.toString(), style = MaterialTheme.typography.headlineLarge.copy(fontSize = 40.sp, lineHeight = 52.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1.9).sp))
                                Text(t(" / $displayedTotal кредитів"), modifier = Modifier.padding(bottom = 5.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (!stackSemesterControl) CurrentSemesterControl(currentSemester) { selectingSemester = true }
                        }
                        if (stackSemesterControl) CurrentSemesterControl(currentSemester) { selectingSemester = true }
                        LinearProgressIndicator(
                            progress = { if (displayedTotal > 0) (displayedEarned / displayedTotal.toFloat()).coerceIn(0f, 1f) else 0f },
                            modifier = Modifier.fillMaxWidth().height(5.dp),
                            gapSize = 0.dp,
                            drawStopIndicator = {},
                            trackColor = MaterialTheme.colorScheme.outlineVariant,
                        )
                        Text(t("$completedSemesters із ${semesters.size} семестрів завершено"),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            TaktSegmentedTabs(listOf(t("План"), t("Активні"), t("Здані")), filter, { filter = it },
                railCornerRadius = 12.dp)
            Spacer(Modifier.height(12.dp))
        }
        if (filter != 0) {
            val filtered = if (filter == 1) activeCourses else completedCourses
            items(filtered, key = { it.id }) { course ->
                GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                    Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp)) {
                        ProgressCourseRow(course, gradeRepository, onOpen = { onCourseClick(course.id) }, onStatus = { editingCourseId = course.id })
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            if (filtered.isEmpty()) item { Text(if (filter == 1) t("Активних предметів ще немає. Обери їх у плані.") else t("Зданих предметів ще немає."), style = MaterialTheme.typography.bodySmall) }
        }
        if (filter == 0) semesters.forEach { (semester, semesterCourses) ->
            item(key = "semester-$semester") {
                SemesterSection(
                    semester = semester,
                    courses = semesterCourses,
                    gradeRepository = gradeRepository,
                    isCurrent = semester == currentSemester,
                    expanded = semester in expandedSemesters,
                    onExpandedChange = { expanded -> expandedSemesters = if (expanded) expandedSemesters + semester else expandedSemesters - semester },
                    onCourseClick = onCourseClick,
                    onCourseStatus = { editingCourseId = it.id },
                )
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
    if (searchOpen) CourseSearchSheet(
        courses = courses,
        onCourseClick = { courseId -> searchOpen = false; onCourseClick(courseId) },
        onDismiss = { searchOpen = false },
    )
    courses.firstOrNull { it.id == editingCourseId }?.let { course ->
        CourseStatusSheet(course, uisManaged = uisAccount, onStatus = { status ->
            if (!uisAccount) scope.launch { repository.updateStatus(course.id, status) }
            editingCourseId = null
        }, onIcon = { iconCourseId = course.id; editingCourseId = null }, onDismiss = { editingCourseId = null })
    }
    courses.firstOrNull { it.id == iconCourseId }?.let { course ->
        CourseIconPicker(course.iconKey, onSelect = { key ->
            scope.launch { repository.setIcon(course.id, key) }
            iconCourseId = null
        }, onDismiss = { iconCourseId = null })
    }
    if (selectingSemester) CurrentSemesterSheet(
        semesters = semesters.mapValues { (_, items) -> items.count { it.status == CourseStatus.ENROLLED } },
        currentSemester = currentSemester,
        manualSemester = settings.currentSemester,
        onSelect = { selected -> scope.launch {
            settingsRepository.setCurrentSemester(selected)
            selectingSemester = false
        } },
        onDismiss = { selectingSemester = false },
    )

}

@Composable
private fun CurrentSemesterControl(semester: Int?, onClick: () -> Unit) {
    GlassSurface(Modifier.testTag("current-semester-selector").clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp), tint = MaterialTheme.colorScheme.primaryContainer) {
        Row(Modifier.heightIn(min = 48.dp).padding(horizontal = 9.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Icon(Icons.Default.School, null, Modifier.size(17.dp), tint = MaterialTheme.colorScheme.primary)
            Text(if (semester != null) t("$semester семестр") else t("Обрати семестр"), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary)
            Icon(Icons.Default.ExpandMore, t("Обрати семестр"), Modifier.size(17.dp), tint = MaterialTheme.colorScheme.primary)
        }
    }
}
