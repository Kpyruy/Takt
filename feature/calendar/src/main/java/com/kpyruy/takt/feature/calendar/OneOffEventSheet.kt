package com.kpyruy.takt.feature.calendar

import com.kpyruy.takt.core.model.uiMessage
import com.kpyruy.takt.core.model.UiKey
import com.kpyruy.takt.core.ui.i18n.t
import androidx.compose.runtime.saveable.rememberSaveable
import com.kpyruy.takt.core.ui.components.LocalDateStateSaver
import com.kpyruy.takt.core.ui.components.LocalTimeStateSaver

import com.kpyruy.takt.core.ui.components.TaktFullSheet
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.kpyruy.takt.core.ui.components.TaktPrimaryButton as Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import com.kpyruy.takt.core.ui.components.TaktTextField as OutlinedTextField
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
import androidx.compose.ui.platform.testTag
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
    initialLessonType: LessonType = if (initialType == OneOffScheduleEventType.EXTRA) LessonType.EVENT else LessonType.UNSPECIFIED,
    initialStartTime: LocalTime? = null,
    initialEndTime: LocalTime? = null,
    initialRoom: String = "",
    modifier: Modifier = Modifier,
    showHeading: Boolean = true,
    onSave: (OneOffScheduleEvent) -> Unit,
) {
    var linkedCourseId by rememberSaveable(initialEvent?.id, initialCourseId) { mutableStateOf(initialEvent?.courseId ?: initialCourseId) }
    var lessonType by rememberSaveable(initialEvent?.id, initialLessonType) { mutableStateOf(initialEvent?.lessonType ?: initialLessonType) }
    var title by rememberSaveable(initialEvent?.id, initialTitle) {
        mutableStateOf(initialEvent?.title ?: initialTitle)
    }
    var date by rememberSaveable(initialEvent?.id, initialDate, stateSaver = LocalDateStateSaver) {
        mutableStateOf(initialEvent?.date ?: initialDate)
    }
    var startTime by rememberSaveable(initialEvent?.id, initialStartTime, stateSaver = LocalTimeStateSaver) {
        mutableStateOf(initialEvent?.startTime ?: initialStartTime ?: LocalTime.of(8, 0))
    }
    var endTime by rememberSaveable(initialEvent?.id, initialType, initialEndTime, stateSaver = LocalTimeStateSaver) {
        mutableStateOf(
            initialEvent?.endTime
                ?: initialEndTime
                ?: if (initialType == OneOffScheduleEventType.REMINDER) LocalTime.of(8, 5)
                else LocalTime.of(9, 50)
        )
    }
    var room by rememberSaveable(initialEvent?.id, initialRoom) {
        mutableStateOf(initialEvent?.room ?: initialRoom)
    }
    var type by rememberSaveable(initialEvent?.id, initialType) {
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
                    initialEvent != null -> t(UiKey.TEXT_EDIT_THE_EVENT)
                    isReminder -> t(UiKey.TEXT_REMINDER)
                    else -> t(UiKey.TEXT_ONE_TIME_EVENT)
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
            label = { Text(if (isReminder) t(UiKey.TEXT_WHAT_TO_REMIND) else t(UiKey.COMMON_NAME)) },
            modifier = Modifier.fillMaxWidth().testTag("oneoff-title"),
            singleLine = true,
        )

        if (!isReminder && lessonType != LessonType.EVENT) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = type == OneOffScheduleEventType.EXTRA,
                    onClick = { type = OneOffScheduleEventType.EXTRA },
                    label = { Text(t(UiKey.TEXT_EXTRA_CLASS)) },
                )
                FilterChip(
                    selected = type == OneOffScheduleEventType.BLOCK_ACTION,
                    onClick = { type = OneOffScheduleEventType.BLOCK_ACTION },
                    label = { Text(t(UiKey.TEXT_BLOCK_ACTION)) },
                )
            }
        } else if (isReminder) {
            Text(
                t(UiKey.TEXT_CONFIGURE_REMINDER_NOTIFICATIONS_IN_PERSONALIZE_NOTIFICATIONS),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        TaktDatePickerField(
            label = t(UiKey.TEXT_DATE_99C40AB4),
            value = date,
            onValueChange = {
                date = it
                error = null
            },
            modifier = Modifier.fillMaxWidth().testTag("oneoff-date"),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TaktTimePickerField(
                label = t(UiKey.TEXT_START_TIME),
                value = startTime,
                onValueChange = {
                    startTime = it
                    if (isReminder && endTime <= it) endTime = it.plusMinutes(5)
                    error = null
                },
                modifier = Modifier.weight(1f),
            )
            TaktTimePickerField(
                label = t(UiKey.TEXT_END),
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
                label = { Text(if (lessonType == LessonType.EVENT) t(UiKey.TEXT_PLACE) else t(UiKey.TEXT_ROOM_911EA89C)) },
                modifier = Modifier.fillMaxWidth().testTag("oneoff-room"),
                singleLine = true,
            )
        }

        error?.let {
            Text(
                t(it),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Button(
            onClick = {
                if (endTime <= startTime) {
                    error = uiMessage(UiKey.TEXT_THE_END_MUST_BE_LATER_THAN_THE_BEGINNING)
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
                        type = if (!isReminder && lessonType == LessonType.EVENT) OneOffScheduleEventType.EXTRA else type,
                    )
                )
            },
            enabled = title.isNotBlank(),
            modifier = Modifier.fillMaxWidth().testTag("oneoff-save"),
        ) {
            Text(if (initialEvent == null) t(UiKey.TEXT_ADD) else t(UiKey.ACTION_UPDATE))
        }
    }
}
