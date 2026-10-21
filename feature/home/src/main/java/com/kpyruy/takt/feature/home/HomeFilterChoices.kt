package com.kpyruy.takt.feature.home

import com.kpyruy.takt.core.model.UiKey
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.*
import com.kpyruy.takt.core.ui.components.*
import com.kpyruy.takt.core.ui.i18n.t

@Composable
internal fun HomeFilterChoices(filter: HomeWorkFilter, courses: List<Course>, onChange: (HomeWorkFilter) -> Unit) {
    val periods = listOf(HomeWorkPeriod.SEVEN_DAYS to t(UiKey.TEXT_7_DAYS), HomeWorkPeriod.FOURTEEN_DAYS to t(UiKey.TEXT_14_DAYS),
        HomeWorkPeriod.ALL to t(UiKey.TEXT_ALL), HomeWorkPeriod.CUSTOM to t(UiKey.TEXT_CUSTOM_PERIOD))
    FilterPopupField(t(UiKey.TEXT_PERIOD_6E795D4D), periods.first { it.first == filter.period }.second, Icons.Outlined.DateRange, "home-filter-period") { close ->
        periods.forEach { (value, label) ->
            TaktChoiceMenuItem(text = { Text(label) }, onClick = { onChange(filter.copy(period = value)); close() },
                trailingIcon = { if (value == filter.period) Icon(Icons.Outlined.Check, null) })
        }
    }
    if (filter.period == HomeWorkPeriod.CUSTOM) {
        TaktDatePickerField(t(UiKey.TEXT_FROM_21819769), filter.fromDate, { onChange(filter.copy(fromDate = it)) }, Modifier.fillMaxWidth())
        TaktDatePickerField(t(UiKey.TEXT_TO_F4B06EF6), filter.toDate, { onChange(filter.copy(toDate = it)) }, Modifier.fillMaxWidth())
    }
    val ids = filter.selectedCourseIds
    val courseSummary = if (ids.isEmpty()) t(UiKey.TEXT_ALL_SUBJECTS) else courses.filter { it.id in ids }.joinToString(", ") { it.code }.ifBlank { t(UiKey.TEXT_SELECTED_COURSES) }
    FilterPopupField(t(UiKey.TEXT_SUBJECT_68971283), courseSummary, Icons.Outlined.School, "home-filter-courses") { _ ->
        FilterCheckRow(t(UiKey.TEXT_ALL_SUBJECTS), ids.isEmpty(), "home-filter-course-all", Icons.Outlined.School) {
            onChange(filter.copy(courseId = null, courseIds = emptySet()))
        }
        courses.forEach { course ->
            FilterCheckRow(course.code + " · " + course.title, course.id in ids, "home-filter-course-${course.id}",
                CourseIcons.find(course.iconKey)?.vector ?: Icons.Outlined.School) {
                onChange(filter.copy(courseId = null, courseIds = if (course.id in ids) ids - course.id else ids + course.id))
            }
        }
    }
    val types = listOf<GradeItemType?>(null) + GradeItemType.entries.filterNot { it in setOf(GradeItemType.EXAM, GradeItemType.MIDTERM) }
    val selected = filter.types.map { if (it == GradeItemType.MIDTERM) GradeItemType.TEST else it }.toSet()
    fun label(type: GradeItemType?) = if (type == null) t(UiKey.TEXT_ASSIGNMENT) else t(type.label)
    FilterPopupField(t(UiKey.TEXT_TASK_TYPE), if (selected.isEmpty()) t(UiKey.TEXT_ALL_TYPES) else selected.joinToString(", ") { label(it) },
        Icons.Outlined.TaskAlt, "home-filter-types") { _ ->
        FilterCheckRow(t(UiKey.TEXT_ALL_TYPES), selected.isEmpty(), "home-filter-type-all", Icons.Outlined.CheckCircle) { onChange(filter.copy(types = emptySet())) }
        types.forEach { type ->
            FilterCheckRow(label(type), type in selected, "home-filter-type-${type?.name ?: "TASK"}", homeFilterTaskIcon(type)) {
                onChange(filter.copy(types = if (type in selected) selected - type else selected + type))
            }
        }
    }
}

@Composable
private fun FilterPopupField(label: String, value: String, icon: ImageVector, tag: String,
    choices: @Composable ColumnScope.(() -> Unit) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        Box {
            GlassSurface(Modifier.fillMaxWidth().testTag(tag).clickable { open = true }) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                    Text(value, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Icon(Icons.Outlined.ExpandMore, null)
                }
            }
            TaktChoiceMenu(expanded = open, onDismissRequest = { open = false }) { choices { open = false } }
        }
    }
}

@Composable
private fun FilterCheckRow(label: String, checked: Boolean, tag: String, icon: ImageVector, onClick: () -> Unit) {
    TaktChoiceMenuItem(text = { Text(label) }, onClick = onClick,
        modifier = Modifier.testTag(tag).semantics { role = Role.Checkbox; toggleableState = if (checked) ToggleableState.On else ToggleableState.Off },
        leadingIcon = { Icon(icon, null, Modifier.size(21.dp)) },
        trailingIcon = { Checkbox(checked, onCheckedChange = null) })
}
