package com.kpyruy.takt.app

import com.kpyruy.takt.core.model.uiMessage
import com.kpyruy.takt.core.model.UiKey
import com.kpyruy.takt.core.ui.i18n.t

import com.kpyruy.takt.core.ui.components.TaktFullSheet
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import com.kpyruy.takt.core.ui.components.TaktPrimaryButton
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import com.kpyruy.takt.core.ui.components.TaktTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.Course
import com.kpyruy.takt.core.model.activeCourseChoices
import com.kpyruy.takt.core.model.CourseNote
import com.kpyruy.takt.core.model.ScheduleRecurrence
import com.kpyruy.takt.core.model.ScheduleRule
import com.kpyruy.takt.core.model.StudyTask
import com.kpyruy.takt.core.ui.components.TaktDatePickerField
import com.kpyruy.takt.core.ui.components.TaktTimePickerField
import com.kpyruy.takt.core.ui.motion.rememberTaktHaptics
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

private enum class QuickAddType(private val sourceLabel: String) {
    TASK(UiKey.TEXT_ASSIGNMENT),
    CLASS(UiKey.TEXT_CLASS),
    NOTE(UiKey.TEXT_NOTE),

    ;
    val label: String get() = t(sourceLabel)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddSheet(
    courses: List<Course>,
    uisManaged: Boolean = false,
    onDismiss: () -> Unit,
    onSaveTask: (StudyTask) -> Unit,
    onSaveLesson: (ScheduleRule) -> Unit,
    onSaveNote: (CourseNote) -> Unit,
    onOpenFull: (CreateItemDraft) -> Unit,
) {
    val haptics = rememberTaktHaptics()
    var lessonType by remember { mutableStateOf(com.kpyruy.takt.core.model.LessonType.UNSPECIFIED) }
    var type by remember { mutableStateOf(QuickAddType.TASK) }
    var title by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var courseId by remember { mutableStateOf<String?>(null) }
    var dueDate by remember { mutableStateOf<LocalDate?>(null) }
    var requiredForExam by remember { mutableStateOf(false) }
    var startTime by remember { mutableStateOf(LocalTime.of(8, 0)) }
    var endTime by remember { mutableStateOf(LocalTime.of(9, 50)) }
    var error by remember { mutableStateOf<String?>(null) }

    val needsCourse = type == QuickAddType.TASK || type == QuickAddType.NOTE
    val activeCourses = courses.activeCourseChoices()

    TaktFullSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(t(UiKey.TEXT_QUICK_ADD), style = MaterialTheme.typography.headlineSmall)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickAddType.entries.forEach { option ->
                    FilterChip(
                        selected = type == option,
                        onClick = {
                            if (uisManaged && option == QuickAddType.CLASS) {
                                onOpenFull(CreateItemDraft(type = CreateItemType.CLASS))
                                return@FilterChip
                            }
                            type = option
                            haptics.tick()
                            error = null
                        },
                        label = { Text(option.label) },
                    )
                }
            }

            if (needsCourse) {
                Text(t(UiKey.TEXT_SUBJECT_68971283), style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(activeCourses, key = { it.id }) { course ->
                        FilterChip(
                            selected = courseId == course.id,
                            onClick = {
                                courseId = course.id
                                haptics.tick()
                            },
                            label = { Text(course.title, maxLines = 1) },
                        )
                    }
                }
                if (activeCourses.isEmpty()) {
                    Text(t(UiKey.TEXT_THERE_ARE_NO_ACTIVE_SUBJECTS_MARK_THE_SUBJECT_AS_ACTIVE_IN_PROGRESS), style = MaterialTheme.typography.bodySmall)
                }
            }

            if (type == QuickAddType.CLASS) {
                com.kpyruy.takt.core.ui.components.CourseLinkSelector(courses, courseId) { course ->
                    if (title.isBlank() || title == courses.firstOrNull { it.id == courseId }?.title) title = course?.title.orEmpty()
                    courseId = course?.id
                }
                com.kpyruy.takt.core.ui.components.LessonTypeSelector(lessonType) { lessonType = it }
            }
            TaktTextField(
                value = title,
                onValueChange = {
                    title = it
                    error = null
                },
                label = { Text(if (type == QuickAddType.CLASS && lessonType != com.kpyruy.takt.core.model.LessonType.EVENT) t(UiKey.TEXT_CLASS_NAME) else t(UiKey.COMMON_NAME)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            when (type) {
                QuickAddType.TASK -> {
                    TaktDatePickerField(
                        label = t(UiKey.WORK_DEADLINE),
                        value = dueDate,
                        onValueChange = { dueDate = it },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().toggleable(
                            value = requiredForExam,
                            role = Role.Checkbox,
                            onValueChange = { requiredForExam = it },
                        ),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = requiredForExam, onCheckedChange = null)
                        Text(t(UiKey.TEXT_REQUIRED_FOR_THE_EXAM))
                    }
                }

                QuickAddType.CLASS -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TaktTimePickerField(
                        label = t(UiKey.TEXT_START_TIME),
                        value = startTime,
                        onValueChange = { startTime = it },
                        modifier = Modifier.weight(1f),
                    )
                    TaktTimePickerField(
                        label = t(UiKey.TEXT_END),
                        value = endTime,
                        onValueChange = { endTime = it },
                        modifier = Modifier.weight(1f),
                    )
                }

                QuickAddType.NOTE -> TaktTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = { Text(t(UiKey.TEXT_TEXT)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
            }

            error?.let {
                Text(t(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            TaktPrimaryButton(
                onClick = {
                    if (title.isBlank()) {
                        error = uiMessage(UiKey.TEXT_ENTER_A_NAME)
                        return@TaktPrimaryButton
                    }
                    if (needsCourse && courseId == null) {
                        error = uiMessage(UiKey.TEXT_CHOOSE_A_SUBJECT_62A7E2E3)
                        return@TaktPrimaryButton
                    }
                    if (courseId != null && activeCourses.none { it.id == courseId }) {
                        error = uiMessage(UiKey.TEXT_THIS_SUBJECT_IS_NO_LONGER_ACTIVE_SELECT_THE_ACTIVE_SUBJECT)
                        return@TaktPrimaryButton
                    }

                    when (type) {
                        QuickAddType.TASK -> onSaveTask(
                            StudyTask(
                                id = UUID.randomUUID().toString(),
                                courseId = courseId!!,
                                title = title.trim(),
                                description = null,
                                dueDate = dueDate,
                                completed = false,
                                requiredForExam = requiredForExam,
                            )
                        )

                        QuickAddType.CLASS -> {
                            if (endTime <= startTime) {
                                error = uiMessage(UiKey.TEXT_THE_END_MUST_BE_LATER_THAN_THE_BEGINNING)
                                return@TaktPrimaryButton
                            }
                            onSaveLesson(
                                ScheduleRule(
                                    id = UUID.randomUUID().toString(),
                                    courseId = courseId,
                                    lessonType = lessonType,
                                    title = title.trim(),
                                    dayOfWeek = LocalDate.now().dayOfWeek,
                                    startTime = startTime,
                                    endTime = endTime,
                                    recurrence = ScheduleRecurrence.WEEKLY,
                                    room = null,
                                )
                            )
                        }

                        QuickAddType.NOTE -> {
                            if (details.isBlank()) {
                                error = uiMessage(UiKey.TEXT_ADD_NOTE_TEXT)
                                return@TaktPrimaryButton
                            }
                            onSaveNote(
                                CourseNote(
                                    id = UUID.randomUUID().toString(),
                                    courseId = courseId!!,
                                    title = title.trim(),
                                    content = details.trim(),
                                    updatedAtEpochMillis = System.currentTimeMillis(),
                                )
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(t(UiKey.ACTION_SAVE))
            }

            TextButton(
                onClick = {
                    val fullType = when (type) {
                        QuickAddType.TASK -> CreateItemType.TASK
                        QuickAddType.CLASS -> CreateItemType.CLASS
                        QuickAddType.NOTE -> CreateItemType.NOTE
                    }
                    haptics.tick()
                    onOpenFull(
                        CreateItemDraft(
                            type = fullType,
                            lessonType = lessonType,
                            courseId = courseId?.takeIf { id -> activeCourses.any { it.id == id } },
                            title = title,
                            details = details,
                            dueDate = dueDate,
                            requiredForExam = requiredForExam,
                            startTime = startTime,
                            endTime = endTime,
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(t(UiKey.TEXT_MORE_OPTIONS))
            }
        }
    }
}
