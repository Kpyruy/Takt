package com.kpyruy.takt.feature.studyplan

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.kpyruy.takt.core.model.Course
import com.kpyruy.takt.core.ui.components.CourseIconPicker
import com.kpyruy.takt.core.ui.components.TaktUnderlineTabs
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.kpyruy.takt.core.data.StudyPlanRepository
import com.kpyruy.takt.core.model.CourseStatus
import com.kpyruy.takt.core.ui.components.ScreenHeader
import com.kpyruy.takt.core.ui.components.TaktIconButton

@Composable
fun StudyPlanScreen(
    repository: StudyPlanRepository,
    onCourseClick: (String) -> Unit,
) {
    val courses by remember(repository) { repository.observeCourses() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val earned = remember(courses) { courses.filter { it.status == CourseStatus.FULFILLED }.sumOf { it.credits } }
    val semesters = remember(courses) { courses.groupBy { it.semester }.toSortedMap() }
    val largeText = LocalDensity.current.fontScale > 1.2f
    val completedSemesters = remember(semesters) { semesters.values.count { semesterState(it) == SemesterState.COMPLETED } }
    var expandedSemesters by rememberSaveable { mutableStateOf(emptySet<Int>()) }
    var filter by rememberSaveable { mutableIntStateOf(0) }
    var editingCourseId by rememberSaveable { mutableStateOf<String?>(null) }
    var iconCourseId by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val activeCourses = remember(courses) { courses.filter { it.status == CourseStatus.ENROLLED } }
    val completedCourses = remember(courses) { courses.filter { it.status == CourseStatus.FULFILLED } }
    var menuExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).testTag("progress-screen"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 96.dp),
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
                SemesterSection(
                    semester = semester,
                    courses = semesterCourses,
                    expanded = semester in expandedSemesters,
                    onExpandedChange = { expanded -> expandedSemesters = if (expanded) expandedSemesters + semester else expandedSemesters - semester },
                    onCourseClick = onCourseClick,
                    onCourseStatus = { editingCourseId = it.id },
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
