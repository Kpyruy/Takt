package com.kpyruy.takt.feature.calendar

import com.kpyruy.takt.core.ui.i18n.t

import com.kpyruy.takt.core.ui.components.TaktFullSheet
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.kpyruy.takt.core.model.Course
import com.kpyruy.takt.core.model.LessonType
import com.kpyruy.takt.core.ui.components.LessonTypeSelector
import com.kpyruy.takt.core.ui.components.CourseLinkSelector
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.OneOffScheduleEvent
import com.kpyruy.takt.core.model.OneOffScheduleEventType
import com.kpyruy.takt.core.ui.components.TaktDatePickerField
import com.kpyruy.takt.core.ui.components.TaktTimePickerField
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OneOffEventSheet(
    initialDate: LocalDate,
    courses: List<Course> = emptyList(),
    initialEvent: OneOffScheduleEvent? = null,
    initialType: OneOffScheduleEventType = OneOffScheduleEventType.EXTRA,
    onDismiss: () -> Unit,
    onSave: (OneOffScheduleEvent) -> Unit,
) {
    TaktFullSheet(onDismissRequest = onDismiss) {
        OneOffEventForm(
            initialDate = initialDate,
            initialEvent = initialEvent,
            courses = courses,
            initialType = initialType,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            onSave = onSave,
        )
    }
}

@Composable
fun OneOffEventForm(
    initialDate: LocalDate,
    courses: List<Course> = emptyList(),
    initialEvent: OneOffScheduleEvent? = null,
    initialType: OneOffScheduleEventType = OneOffScheduleEventType.EXTRA,
    initialTitle: String = "",
    initialCourseId: String? = null,
    initialLessonType: LessonType = LessonType.UNSPECIFIED,
    initialStartTime: LocalTime? = null,
    initialEndTime: LocalTime? = null,
    initialRoom: String = "",
    modifier: Modifier = Modifier,
    showHeading: Boolean = true,
    onSave: (OneOffScheduleEvent) -> Unit,
) {
    var linkedCourseId by remember(initialEvent?.id, initialCourseId) { mutableStateOf(initialEvent?.courseId ?: initialCourseId) }
    var lessonType by remember(initialEvent?.id, initialLessonType) { mutableStateOf(initialEvent?.lessonType ?: initialLessonType) }
    var title by remember(initialEvent?.id, initialTitle) {
        mutableStateOf(initialEvent?.title ?: initialTitle)
    }
    var date by remember(initialEvent?.id, initialDate) {
        mutableStateOf(initialEvent?.date ?: initialDate)
    }
    var startTime by remember(initialEvent?.id, initialStartTime) {
        mutableStateOf(initialEvent?.startTime ?: initialStartTime ?: LocalTime.of(8, 0))
    }
    var endTime by remember(initialEvent?.id, initialType, initialEndTime) {
        mutableStateOf(
            initialEvent?.endTime
                ?: initialEndTime
                ?: if (initialType == OneOffScheduleEventType.REMINDER) LocalTime.of(8, 5)
                else LocalTime.of(9, 50)
        )
    }
    var room by remember(initialEvent?.id, initialRoom) {
        mutableStateOf(initialEvent?.room ?: initialRoom)
    }
    var type by remember(initialEvent?.id, initialType) {
        mutableStateOf(initialEvent?.type ?: initialType)
    }
    var error by remember { mutableStateOf<String?>(null) }
    val isReminder = type == OneOffScheduleEventType.REMINDER

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (showHeading) {
            Text(
                when {
                    initialEvent != null -> t("Редагувати подію")
                    isReminder -> t("Нагадування")
                    else -> t("Разова подія")
                },
                style = MaterialTheme.typography.headlineSmall,
            )
        }

        if (!isReminder) {
        CourseLinkSelector(courses, linkedCourseId) { course ->
            if (title.isBlank() || title == courses.firstOrNull { it.id == linkedCourseId }?.title) title = course?.title.orEmpty()
            linkedCourseId = course?.id
        }
        LessonTypeSelector(lessonType) { lessonType = it }

        }
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text(if (isReminder) t("Що нагадати") else t("Назва")) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        if (!isReminder) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = type == OneOffScheduleEventType.EXTRA,
                    onClick = { type = OneOffScheduleEventType.EXTRA },
                    label = { Text(t("Додаткова пара")) },
                )
                FilterChip(
                    selected = type == OneOffScheduleEventType.BLOCK_ACTION,
                    onClick = { type = OneOffScheduleEventType.BLOCK_ACTION },
                    label = { Text(t("Блокова акція")) },
                )
            }
        } else {
            Text(
                t("Це нагадування відображається всередині Takt; системне Android-сповіщення не створюється."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        TaktDatePickerField(
            label = t("Дата"),
            value = date,
            onValueChange = {
                date = it
                error = null
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TaktTimePickerField(
                label = t("Початок"),
                value = startTime,
                onValueChange = {
                    startTime = it
                    if (isReminder && endTime <= it) endTime = it.plusMinutes(5)
                    error = null
                },
                modifier = Modifier.weight(1f),
            )
            TaktTimePickerField(
                label = t("Кінець"),
                value = endTime,
                onValueChange = {
                    endTime = it
                    error = null
                },
                modifier = Modifier.weight(1f),
            )
        }

        if (!isReminder) {
            OutlinedTextField(
                value = room,
                onValueChange = { room = it },
                label = { Text(t("Аудиторія")) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }

        error?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Button(
            onClick = {
                if (endTime <= startTime) {
                    error = t("Кінець має бути пізніше початку.")
                    return@Button
                }
                onSave(
                    OneOffScheduleEvent(
                        id = initialEvent?.id ?: UUID.randomUUID().toString(),
                        courseId = linkedCourseId,
                        lessonType = if (isReminder) LessonType.UNSPECIFIED else lessonType,
                        title = title.trim(),
                        date = date,
                        startTime = startTime,
                        endTime = endTime,
                        room = if (isReminder) null else room.trim().ifBlank { null },
                        type = type,
                    )
                )
            },
            enabled = title.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (initialEvent == null) t("Додати") else t("Оновити"))
        }
    }
}
