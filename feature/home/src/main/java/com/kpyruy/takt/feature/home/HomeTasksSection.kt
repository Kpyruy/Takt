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
            Text(t("Задачі"), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Text((overdue.size + upcoming.size).toString(), Modifier.testTag("home-tasks-count"),
                style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { showFilters = true }, modifier = Modifier.testTag("home-tasks-filter")) {
                Icon(Icons.Outlined.Tune, t("Фільтри задач"))
            }
        }
        Text(periodSummary(filter), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        if (overdue.isEmpty() && upcoming.isEmpty()) {
            Text(t("За цим фільтром задач немає"), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            if (overdue.isNotEmpty()) {
                Text(t("Прострочені · ${overdue.size}"),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("home-overdue-heading"))
            }
            (overdue + upcoming).forEach { entry ->
                when (entry) {
                    is HomeWorkEntry.Task -> HomeTaskRow(entry.value,
                        courseCodes[entry.courseId] ?: courseTitles[entry.courseId] ?: entry.courseId) {
                        onTaskCompleted(entry.value, it)
                    }
                    is HomeWorkEntry.Graded -> HomeAssessmentRow(entry.value,
                        courseCodes[entry.courseId] ?: entry.courseId) {
                        onOpenAssessment(entry.value)
                    }
                }
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
    ModalBottomSheet(onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(t("Фільтри задач"), Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
                IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, t("Закрити фільтри")) }
            }
            SectionCard {
                Text(t("Період"), style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        Triple(HomeWorkPeriod.SEVEN_DAYS, t("7 днів"), Icons.Outlined.Today),
                        Triple(HomeWorkPeriod.FOURTEEN_DAYS, t("14 днів"), Icons.Outlined.DateRange),
                        Triple(HomeWorkPeriod.ALL, t("Усі"), Icons.Outlined.Event),
                        Triple(HomeWorkPeriod.CUSTOM, t("Свій період"), Icons.Outlined.EditCalendar),
                    ).forEach { (period, label, icon) ->
                        FilterChip(selected = filter.period == period,
                            onClick = { onChange(filter.copy(period = period)) },
                            label = { Text(label) }, leadingIcon = { Icon(icon, null, Modifier.size(18.dp)) },
                            shape = RoundedCornerShape(12.dp))
                    }
                }
                if (filter.period == HomeWorkPeriod.CUSTOM) {
                    TaktDatePickerField(t("Від"), filter.fromDate,
                        { onChange(filter.copy(fromDate = it)) }, Modifier.fillMaxWidth())
                    TaktDatePickerField(t("До"), filter.toDate,
                        { onChange(filter.copy(toDate = it)) }, Modifier.fillMaxWidth())
                }
            }
            SectionCard {
                Text(t("Предмет"), style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = filter.courseId == null,
                        onClick = { onChange(filter.copy(courseId = null)) },
                        label = { Text(t("Усі предмети")) },
                        leadingIcon = { Icon(Icons.Outlined.School, null, Modifier.size(18.dp)) },
                        shape = RoundedCornerShape(12.dp))
                    courses.forEach { course ->
                        FilterChip(selected = filter.courseId == course.id,
                            onClick = { onChange(filter.copy(courseId = course.id)) },
                            label = { Text(course.code) },
                            leadingIcon = { Icon(Icons.Outlined.School, null, Modifier.size(18.dp)) },
                            shape = RoundedCornerShape(12.dp))
                    }
                }
            }
            SectionCard {
                Text(t("Тип задачі"), style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = filter.types.isEmpty(),
                        onClick = { onChange(filter.copy(types = emptySet())) },
                        label = { Text(t("Усі типи")) },
                        leadingIcon = { Icon(Icons.Outlined.CheckCircle, null, Modifier.size(18.dp)) },
                        shape = RoundedCornerShape(12.dp))
                    val types: List<Pair<GradeItemType?, String>> =
                        listOf(null to t("Завдання")) + GradeItemType.entries
                            .filterNot { it == GradeItemType.EXAM || it == GradeItemType.MIDTERM }.map { it to t(it.label) }
                    types.forEach { (type, label) ->
                        val icon = when (type) {
                            null -> Icons.Outlined.TaskAlt
                            GradeItemType.TEST, GradeItemType.MIDTERM -> Icons.Outlined.Quiz
                            GradeItemType.LAB -> Icons.Outlined.Science
                            else -> Icons.Outlined.Assignment
                        }
                        val selected = type in filter.types ||
                            (type == GradeItemType.TEST && GradeItemType.MIDTERM in filter.types)
                        FilterChip(selected = selected,
                            onClick = {
                                onChange(filter.copy(types = if (selected) filter.types - type - GradeItemType.MIDTERM
                                    else filter.types + type))
                            }, label = { Text(label) },
                            leadingIcon = { Icon(icon, null, Modifier.size(18.dp)) },
                            shape = RoundedCornerShape(12.dp))
                    }
                }
            }
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
            if (filter != HomeWorkFilter()) {
                OutlinedButton(onClick = { onChange(HomeWorkFilter()) }, Modifier.fillMaxWidth()) {
                    Text(t("Скинути фільтри задач"))
                }
            }
            Button(onClick = onDismiss, Modifier.fillMaxWidth()) { Text(t("Готово")) }
        }
    }
}
