package com.kpyruy.takt.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.Course
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

private enum class QuickAddType(val label: String) {
    TASK("Завдання"),
    CLASS("Пара"),
    NOTE("Нотатка"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddSheet(
    courses: List<Course>,
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
    var startTime by remember { mutableStateOf(LocalTime.of(8, 0)) }
    var endTime by remember { mutableStateOf(LocalTime.of(9, 50)) }
    var error by remember { mutableStateOf<String?>(null) }

    val needsCourse = type == QuickAddType.TASK || type == QuickAddType.NOTE

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Швидке додавання", style = MaterialTheme.typography.headlineSmall)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickAddType.entries.forEach { option ->
                    FilterChip(
                        selected = type == option,
                        onClick = {
                            type = option
                            haptics.tick()
                            error = null
                        },
                        label = { Text(option.label) },
                    )
                }
            }

            if (needsCourse) {
                Text("Предмет", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(courses, key = { it.id }) { course ->
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
            }

            if (type == QuickAddType.CLASS) {
                com.kpyruy.takt.core.ui.components.CourseLinkSelector(courses, courseId) { course ->
                    if (title.isBlank() || title == courses.firstOrNull { it.id == courseId }?.title) title = course?.title.orEmpty()
                    courseId = course?.id
                }
                com.kpyruy.takt.core.ui.components.LessonTypeSelector(lessonType) { lessonType = it }
            }
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    error = null
                },
                label = { Text(if (type == QuickAddType.CLASS) "Назва пари" else "Назва") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            when (type) {
                QuickAddType.TASK -> TaktDatePickerField(
                    label = "Дедлайн",
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    modifier = Modifier.fillMaxWidth(),
                )

                QuickAddType.CLASS -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TaktTimePickerField(
                        label = "Початок",
                        value = startTime,
                        onValueChange = { startTime = it },
                        modifier = Modifier.weight(1f),
                    )
                    TaktTimePickerField(
                        label = "Кінець",
                        value = endTime,
                        onValueChange = { endTime = it },
                        modifier = Modifier.weight(1f),
                    )
                }

                QuickAddType.NOTE -> OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = { Text("Текст") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
            }

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    if (title.isBlank()) {
                        error = "Введіть назву."
                        return@Button
                    }
                    if (needsCourse && courseId == null) {
                        error = "Оберіть предмет."
                        return@Button
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
                            )
                        )

                        QuickAddType.CLASS -> {
                            if (endTime <= startTime) {
                                error = "Кінець має бути пізніше початку."
                                return@Button
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
                                error = "Додайте текст нотатки."
                                return@Button
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
                Text("Зберегти")
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
                            courseId = courseId,
                            title = title,
                            details = details,
                            dueDate = dueDate,
                            startTime = startTime,
                            endTime = endTime,
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Більше параметрів")
            }
        }
    }
}
