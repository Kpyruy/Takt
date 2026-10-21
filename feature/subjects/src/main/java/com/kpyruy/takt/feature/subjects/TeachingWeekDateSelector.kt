package com.kpyruy.takt.feature.subjects

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kpyruy.takt.core.data.AppSettingsRepository
import com.kpyruy.takt.core.data.ScheduleRepository
import com.kpyruy.takt.core.model.*
import com.kpyruy.takt.core.ui.components.TaktChoiceMenu
import com.kpyruy.takt.core.ui.components.TaktChoiceMenuItem
import com.kpyruy.takt.core.ui.components.TaktSurface
import com.kpyruy.takt.core.ui.components.LessonTypeIcon
import com.kpyruy.takt.core.ui.components.taktPressFeedback
import com.kpyruy.takt.core.ui.i18n.TaktI18n
import com.kpyruy.takt.core.ui.i18n.t
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.flowOf

@Composable
internal fun rememberTeachingWeekOptions(
    repository: ScheduleRepository?,
    settingsRepository: AppSettingsRepository?,
    courseId: String,
    academicSemester: Int?,
): TeachingWeekOptions {
    val rules by remember(repository) { repository?.observeRules() ?: flowOf(emptyList<ScheduleRule>()) }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val exceptions by remember(repository) { repository?.observeExceptions() ?: flowOf(emptyList<ScheduleException>()) }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val settings by remember(settingsRepository) { settingsRepository?.settings ?: flowOf(AppSettings()) }
        .collectAsStateWithLifecycle(initialValue = AppSettings())
    return remember(rules, exceptions, settings, courseId, academicSemester) {
        TeachingWeekSchedule.resolve(academicSemester?.let { settings.semesterPeriods[it] }, courseId, rules, exceptions,
            parityForDate = settings::effectiveParity)
    }
}

@Composable
internal fun TeachingWeekDateSelector(
    options: TeachingWeekOptions,
    selectedWeek: Int?,
    dueDate: LocalDate?,
    lessonId: String?,
    onSelectWeek: (TeachingWeek) -> Unit,
    onSelectOccurrence: (ResolvedScheduleEvent) -> Unit,
) {
    val formatter = DateTimeFormatter.ofPattern("EEE d MMM", TaktI18n.locale)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(t("Teaching week"), style = MaterialTheme.typography.titleSmall)
        when (options.setupIssue) {
            TeachingWeekSetupIssue.SEMESTER_PERIOD -> Text(
                t("Set the semester study period in settings or choose Date."),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TeachingWeekSetupIssue.COURSE_SCHEDULE -> Text(
                t("Add recurring classes for this course or choose Date."),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            null -> {
                var expanded by remember { mutableStateOf(false) }
                var lessonExpanded by remember { mutableStateOf(false) }
                val week = options.weeks.firstOrNull { it.number == selectedWeek }
                val triggerInteraction = remember { MutableInteractionSource() }
                val triggerShape = RoundedCornerShape(18.dp)
                Box(Modifier.fillMaxWidth()) {
                    TaktSurface(modifier = Modifier.fillMaxWidth().taktPressFeedback(triggerInteraction).clip(triggerShape)
                        .clickable(indication = null, interactionSource = triggerInteraction, role = Role.Button,
                            onClick = { expanded = true })
                        .testTag("grade-teaching-week"), shape = triggerShape) {
                        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(week?.let { "${t("Teaching week")} ${it.number}" }
                                    ?: t("Choose a teaching week"), style = MaterialTheme.typography.bodyLarge)
                                week?.let {
                                    Text("${it.start.format(formatter)} – ${it.end.format(formatter)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Icon(Icons.Outlined.ExpandMore, null)
                        }
                    }
                    TaktChoiceMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        options.weeks.forEach { option ->
                            TaktChoiceMenuItem(
                                text = {
                                    Column {
                                        Text("${t("Teaching week")} ${option.number}")
                                        Text("${option.start.format(formatter)} – ${option.end.format(formatter)}",
                                            style = MaterialTheme.typography.bodySmall)
                                    }
                                },
                                modifier = Modifier.testTag("grade-week-${option.number}")
                                    .semantics { selected = selectedWeek == option.number },
                                leadingIcon = { Icon(Icons.Outlined.Event, null) },
                                trailingIcon = { if (selectedWeek == option.number) Icon(Icons.Outlined.Check, null) },
                                onClick = {
                                    expanded = false
                                    onSelectWeek(option)
                                    lessonExpanded = option.occurrences.size > 1
                                },
                            )
                        }
                    }
                }
                if (week != null) {
                    if (week.occurrences.isEmpty()) Text(
                        t("No scheduled class this teaching week. Choose Date."),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else {
                        val selectedOccurrence = week.occurrences.firstOrNull { dueDate == it.date && lessonId == it.id }
                        val lessonShape = RoundedCornerShape(18.dp)
                        val lessonInteraction = remember { MutableInteractionSource() }
                        Text(t("Choose a lesson this week"), style = MaterialTheme.typography.titleSmall)
                        Box(Modifier.fillMaxWidth()) {
                            TaktSurface(modifier = Modifier.fillMaxWidth().taktPressFeedback(lessonInteraction).clip(lessonShape)
                                .clickable(indication = null, interactionSource = lessonInteraction, role = Role.Button,
                                    onClick = { lessonExpanded = true }).testTag("grade-week-lesson-trigger"), shape = lessonShape) {
                                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Icon(Icons.Outlined.Event, null)
                                    Column(Modifier.weight(1f)) {
                                        if (selectedOccurrence == null) Text(t("Choose a lesson this week"))
                                        else {
                                            Text("${selectedOccurrence.date.format(formatter)} · ${selectedOccurrence.startTime}–${selectedOccurrence.endTime}")
                                            Text(t(selectedOccurrence.lessonType.label) + selectedOccurrence.room?.let { " · $it" }.orEmpty(),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Icon(Icons.Outlined.ExpandMore, null)
                                }
                            }
                            TaktChoiceMenu(lessonExpanded, onDismissRequest = { lessonExpanded = false }) {
                                week.occurrences.forEach { occurrence ->
                                    val isSelected = dueDate == occurrence.date && lessonId == occurrence.id
                                    TaktChoiceMenuItem(text = {
                                        Column {
                                            Text("${occurrence.date.format(formatter)} · ${occurrence.startTime}–${occurrence.endTime}")
                                            Text(t(occurrence.lessonType.label) + occurrence.room?.let { " · $it" }.orEmpty(),
                                                style = MaterialTheme.typography.bodySmall)
                                        }
                                    }, leadingIcon = { LessonTypeIcon(occurrence.lessonType) },
                                        trailingIcon = { if (isSelected) Icon(Icons.Outlined.Check, null) },
                                        modifier = Modifier.testTag("grade-week-occurrence-${occurrence.id}-${occurrence.date.toEpochDay()}")
                                            .semantics { selected = isSelected },
                                        onClick = { onSelectOccurrence(occurrence); lessonExpanded = false })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
