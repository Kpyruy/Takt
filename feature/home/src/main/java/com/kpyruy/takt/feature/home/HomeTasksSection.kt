package com.kpyruy.takt.feature.home

import com.kpyruy.takt.core.ui.i18n.t

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.HomeWork
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.*
import com.kpyruy.takt.core.ui.components.TaktDatePickerField
import com.kpyruy.takt.core.ui.components.SectionCard
import com.kpyruy.takt.core.ui.components.TaktFullSheet
import com.kpyruy.takt.core.ui.components.CourseIcons
import java.time.LocalDate

private val homeWorkFilterSaver = Saver<HomeWorkFilter, List<String>>(
    save = { filter -> listOf(
        filter.period.name,
        filter.courseId.orEmpty(),
        filter.types.joinToString(",") { it?.name ?: "TASK" },
        filter.includeCompleted.toString(),
        filter.includeUndated.toString(),
        filter.fromDate?.toString().orEmpty(),
        filter.toDate?.toString().orEmpty(),
        filter.courseIds.joinToString(","),
    ) },
    restore = { values -> HomeWorkFilter(
        period = runCatching { HomeWorkPeriod.valueOf(values[0]) }
            .getOrDefault(HomeWorkPeriod.FOURTEEN_DAYS),
        courseId = values[1].ifEmpty { null },
        types = values[2].split(',').filter { it.isNotEmpty() }
            .mapTo(mutableSetOf()) { if (it == "TASK") null else GradeItemType.valueOf(it) },
        includeCompleted = values[3].toBoolean(),
        includeUndated = values[4].toBoolean(),
        fromDate = values[5].ifEmpty { null }?.let(LocalDate::parse),
        toDate = values[6].ifEmpty { null }?.let(LocalDate::parse),
        courseIds = values.getOrNull(7).orEmpty().split(",").filter(String::isNotBlank).toSet(),
    ) },
)

@Composable
internal fun HomeTasksSection(
    tasks: List<StudyTask>,
    assessments: List<GradeItem>,
    courses: List<Course>,
    today: LocalDate,
    savedFilter: HomeWorkFilter,
    onFilterChange: (HomeWorkFilter) -> Unit,
    onTaskCompleted: (StudyTask, Boolean) -> Unit,
    onOpenAssessment: (GradeItem) -> Unit,
) {
    var filter by rememberSaveable(stateSaver = homeWorkFilterSaver) {
        mutableStateOf(savedFilter)
    }
    LaunchedEffect(savedFilter) {
        if (filter != savedFilter) filter = savedFilter
    }
    var showFilters by remember { mutableStateOf(false) }
    val entries = remember(tasks, assessments, today, filter) {
        HomeWorkPlanner.visible(tasks, assessments, today, filter)
    }
    val overdue = remember(tasks, assessments, today, filter) {
        HomeWorkPlanner.overdue(tasks, assessments, today, filter)
    }
    val upcoming = remember(entries, overdue) { entries.filterNot { it in overdue } }
    val courseTitles = remember(courses) { courses.associate { it.id to it.title } }
    val courseCodes = remember(courses) { courses.associate { it.id to it.code } }
    val filterCourses = remember(courses, tasks, assessments) {
        val ids = tasks.mapTo(mutableSetOf()) { it.courseId }
        CourseWork.actionable(assessments).forEach { ids.add(it.courseId) }
        courses.filter { it.id in ids }
    }

    Column(Modifier.fillMaxWidth().testTag("home-tasks-section")) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(t("Найближчі роботи"), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
            Text((overdue.size + upcoming.size).toString(), Modifier.testTag("home-tasks-count"),
                style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { showFilters = true }, modifier = Modifier.testTag("home-tasks-filter")) {
                Icon(Icons.Outlined.Tune, t("Фільтри задач"))
            }
        }
        if (filter.period != HomeWorkPeriod.FOURTEEN_DAYS || filter.selectedCourseIds.isNotEmpty()) Text(periodSummary(filter), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        if (overdue.isEmpty() && upcoming.isEmpty()) {
            Text(t("За цим фільтром задач немає"), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            val renderEntry: @Composable (HomeWorkEntry) -> Unit = { entry ->
                when (entry) {
                    is HomeWorkEntry.Task -> HomeTaskRow(entry.value, courseTitles[entry.courseId] ?: entry.courseId) {
                        onTaskCompleted(entry.value, it)
                    }
                    is HomeWorkEntry.Graded -> HomeAssessmentRow(entry.value, courseTitles[entry.courseId] ?: entry.courseId) {
                        onOpenAssessment(entry.value)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            upcoming.forEach { renderEntry(it) }
            if (overdue.isNotEmpty()) {
                Text(t("Прострочені · ${overdue.size}"), style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("home-overdue-heading").padding(vertical = 6.dp))
                overdue.forEach { renderEntry(it) }
            }
        }
    }

    if (showFilters) {
        HomeTaskFilterSheet(filter, filterCourses, onChange = { next ->
            filter = next
            onFilterChange(next)
        }, onDismiss = { showFilters = false })
    }
}

private fun periodSummary(filter: HomeWorkFilter): String = when (filter.period) {
    HomeWorkPeriod.SEVEN_DAYS -> t("Найближчі 7 днів")
    HomeWorkPeriod.FOURTEEN_DAYS -> t("Найближчі 14 днів")
    HomeWorkPeriod.ALL -> t("Усі дати")
    HomeWorkPeriod.CUSTOM -> t("Обраний період")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun HomeTaskFilterSheet(
    filter: HomeWorkFilter,
    courses: List<Course>,
    onChange: (HomeWorkFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    TaktFullSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(t("Фільтри задач"), Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
                IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, t("Закрити фільтри")) }
            }
            HomeFilterChoices(filter, courses, onChange)
            SectionCard {
                Text(t("Показувати"), style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth().clickable { onChange(filter.copy(includeUndated = !filter.includeUndated)) },
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Event, null, Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary)
                    Text(t("Показувати задачі без дати"), Modifier.weight(1f).padding(start = 12.dp))
                    Checkbox(filter.includeUndated, onCheckedChange = null)
                }
                HorizontalDivider()
                Row(Modifier.fillMaxWidth().clickable { onChange(filter.copy(includeCompleted = !filter.includeCompleted)) },
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CheckCircle, null, Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary)
                    Text(t("Показувати виконані"), Modifier.weight(1f).padding(start = 12.dp))
                    Checkbox(filter.includeCompleted, onCheckedChange = null)
                }
            }
            Button(onClick = onDismiss, Modifier.fillMaxWidth().testTag("home-filter-done")) { Text(t("Готово")) }
        }
    }
}

/** Match the work itself; only legacy MIDTERM intentionally shares the test symbol. */
internal fun homeFilterTaskIcon(type: GradeItemType?): ImageVector = when (type) {
    null -> Icons.Outlined.TaskAlt
    GradeItemType.TEST, GradeItemType.MIDTERM -> Icons.Outlined.Quiz
    GradeItemType.LAB -> Icons.Outlined.Science
    GradeItemType.SEMINAR -> Icons.Outlined.RecordVoiceOver
    GradeItemType.HOMEWORK -> Icons.Outlined.HomeWork
    GradeItemType.PROJECT -> Icons.Outlined.AccountTree
    GradeItemType.ORAL -> Icons.Outlined.Mic
    GradeItemType.EXAM -> Icons.Outlined.School
    GradeItemType.OTHER -> Icons.Outlined.MoreHoriz
}
