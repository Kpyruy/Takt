package com.kpyruy.takt.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Tune
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
        period = HomeWorkPeriod.valueOf(values[0]),
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
    classDays: List<LocalDate>,
    onTaskCompleted: (StudyTask, Boolean) -> Unit,
    onOpenAssessment: (GradeItem) -> Unit,
) {
    var filter by rememberSaveable(stateSaver = homeWorkFilterSaver) { mutableStateOf(HomeWorkFilter()) }
    var showFilters by remember { mutableStateOf(false) }
    val entries = remember(tasks, assessments, today, classDays, filter) {
        HomeWorkPlanner.visible(tasks, assessments, today, classDays, filter)
    }
    val courseTitles = remember(courses) { courses.associate { it.id to it.title } }
    val courseCodes = remember(courses) { courses.associate { it.id to it.code } }
    val filterCourses = remember(courses, tasks, assessments) {
        val ids = tasks.mapTo(mutableSetOf()) { it.courseId }
        CourseWork.actionable(assessments).forEach { ids.add(it.courseId) }
        courses.filter { it.id in ids }
    }

    Column(Modifier.fillMaxWidth().testTag("home-tasks-section")) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Задачі", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Text(entries.size.toString(), Modifier.testTag("home-tasks-count"),
                style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { showFilters = true }, modifier = Modifier.testTag("home-tasks-filter")) {
                Icon(Icons.Outlined.Tune, "Фільтри задач")
            }
        }
        Text(periodSummary(filter), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        if (entries.isEmpty()) {
            Text("За цим фільтром задач немає", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            entries.forEach { entry ->
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
        HomeTaskFilterSheet(filter, filterCourses, onChange = { filter = it }, onDismiss = { showFilters = false })
    }
}

private fun periodSummary(filter: HomeWorkFilter): String = when (filter.period) {
    HomeWorkPeriod.SEVEN_CLASS_DAYS -> "Сьогодні та 7 наступних днів із парами"
    HomeWorkPeriod.FOURTEEN_DAYS -> "Наступні 14 днів"
    HomeWorkPeriod.ALL -> "Усі дати"
    HomeWorkPeriod.CUSTOM -> "Обраний період"
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
                Text("Фільтри задач", Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
                IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, "Закрити фільтри") }
            }
            Column {
                Text("ПЕРІОД", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        HomeWorkPeriod.SEVEN_CLASS_DAYS to "7 днів із парами",
                        HomeWorkPeriod.FOURTEEN_DAYS to "14 днів",
                        HomeWorkPeriod.ALL to "Усі",
                        HomeWorkPeriod.CUSTOM to "Свій період",
                    ).forEach { (period, label) ->
                        FilterChip(selected = filter.period == period,
                            onClick = { onChange(filter.copy(period = period)) },
                            label = { Text(label) }, shape = RoundedCornerShape(12.dp))
                    }
                }
                if (filter.period == HomeWorkPeriod.CUSTOM) {
                    TaktDatePickerField("Від", filter.fromDate,
                        { onChange(filter.copy(fromDate = it)) }, Modifier.fillMaxWidth())
                    TaktDatePickerField("До", filter.toDate,
                        { onChange(filter.copy(toDate = it)) }, Modifier.fillMaxWidth())
                }
            }
            Column {
                Text("ПРЕДМЕТ", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = filter.courseId == null,
                        onClick = { onChange(filter.copy(courseId = null)) },
                        label = { Text("Усі предмети") }, shape = RoundedCornerShape(12.dp))
                    courses.forEach { course ->
                        FilterChip(selected = filter.courseId == course.id,
                            onClick = { onChange(filter.copy(courseId = course.id)) },
                            label = { Text(course.code) }, shape = RoundedCornerShape(12.dp))
                    }
                }
            }
            Column {
                Text("ТИП", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = filter.types.isEmpty(),
                        onClick = { onChange(filter.copy(types = emptySet())) },
                        label = { Text("Усі типи") }, shape = RoundedCornerShape(12.dp))
                    val types: List<Pair<GradeItemType?, String>> =
                        listOf(null to "Завдання") + GradeItemType.entries
                            .filterNot { it == GradeItemType.EXAM }.map { it to it.label }
                    types.forEach { (type, label) ->
                        FilterChip(selected = type in filter.types,
                            onClick = {
                                onChange(filter.copy(types = if (type in filter.types) filter.types - type
                                    else filter.types + type))
                            }, label = { Text(label) }, shape = RoundedCornerShape(12.dp))
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(filter.includeUndated, onCheckedChange = { onChange(filter.copy(includeUndated = it)) })
                Text("Показувати задачі без дати")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(filter.includeCompleted, onCheckedChange = { onChange(filter.copy(includeCompleted = it)) })
                Text("Показувати виконані")
            }
            Button(onClick = onDismiss, Modifier.fillMaxWidth()) { Text("Готово") }
        }
    }
}
