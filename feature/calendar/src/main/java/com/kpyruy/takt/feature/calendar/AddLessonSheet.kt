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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.kpyruy.takt.core.model.ScheduleRecurrence
import com.kpyruy.takt.core.model.ScheduleRule
import com.kpyruy.takt.core.ui.components.TaktTimePickerField
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLessonSheet(
    initialDay: DayOfWeek,
    courses: List<Course> = emptyList(),
    initialRule: ScheduleRule? = null,
    onDismiss: () -> Unit,
    onSave: (ScheduleRule) -> Unit,
) {
    TaktFullSheet(onDismissRequest = onDismiss) {
        AddLessonForm(
            initialDay = initialDay,
            initialRule = initialRule,
            courses = courses,
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
fun AddLessonForm(
    initialDay: DayOfWeek,
    courses: List<Course> = emptyList(),
    initialRule: ScheduleRule? = null,
    initialTitle: String = "",
    initialCourseId: String? = null,
    initialLessonType: LessonType = LessonType.UNSPECIFIED,
    initialRoom: String = "",
    initialStartTime: LocalTime? = null,
    initialEndTime: LocalTime? = null,
    modifier: Modifier = Modifier,
    showHeading: Boolean = true,
    onSave: (ScheduleRule) -> Unit,
) {
    var linkedCourseId by remember(initialRule?.id, initialCourseId) { mutableStateOf(initialRule?.courseId ?: initialCourseId) }
    var lessonType by remember(initialRule?.id, initialLessonType) { mutableStateOf(initialRule?.lessonType ?: initialLessonType) }
    var title by remember(initialRule?.id, initialTitle) {
        mutableStateOf(initialRule?.title ?: initialTitle)
    }
    var room by remember(initialRule?.id, initialRoom) {
        mutableStateOf(initialRule?.room ?: initialRoom)
    }
    var startTime by remember(initialRule?.id, initialStartTime) {
        mutableStateOf(initialRule?.startTime ?: initialStartTime ?: LocalTime.of(8, 0))
    }
    var endTime by remember(initialRule?.id, initialEndTime) {
        mutableStateOf(initialRule?.endTime ?: initialEndTime ?: LocalTime.of(9, 50))
    }
    var day by remember(initialRule?.id, initialDay) {
        mutableStateOf(initialRule?.dayOfWeek ?: initialDay)
    }
    var recurrence by remember(initialRule?.id) {
        mutableStateOf(initialRule?.recurrence ?: ScheduleRecurrence.WEEKLY)
    }
    var showTimeError by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (showHeading) {
            Text(
                text = if (initialRule == null) t("Додати пару") else t("Редагувати пару"),
                style = MaterialTheme.typography.headlineSmall,
            )
        }

        CourseLinkSelector(courses, linkedCourseId) { course ->
            if (title.isBlank() || title == courses.firstOrNull { it.id == linkedCourseId }?.title) title = course?.title.orEmpty()
            linkedCourseId = course?.id
        }
        LessonTypeSelector(lessonType) { lessonType = it }

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text(t("Назва предмета")) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        OutlinedTextField(
            value = room,
            onValueChange = { room = it },
            label = { Text(t("Аудиторія")) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Text(t("День тижня"), style = MaterialTheme.typography.titleSmall)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(DayOfWeek.entries) { option ->
                FilterChip(
                    selected = day == option,
                    onClick = { day = option },
                    label = { Text(option.shortLabel()) },
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TaktTimePickerField(
                label = t("Початок"),
                value = startTime,
                onValueChange = {
                    startTime = it
                    showTimeError = false
                },
                modifier = Modifier.weight(1f),
            )
            TaktTimePickerField(
                label = t("Кінець"),
                value = endTime,
                onValueChange = {
                    endTime = it
                    showTimeError = false
                },
                modifier = Modifier.weight(1f),
            )
        }

        Text(t("Повторення"), style = MaterialTheme.typography.titleSmall)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        ScheduleRecurrence.WEEKLY to t("Щотижня"),
                        ScheduleRecurrence.ODD_WEEKS to t("Непарні"),
                        ScheduleRecurrence.EVEN_WEEKS to t("Парні"),
                    ).forEach { (option, label) ->
                        FilterChip(
                            selected = recurrence == option,
                            onClick = { recurrence = option },
                            label = { Text(label) },
                        )
                    }
                }
            }
        }

        if (showTimeError) {
            Text(
                text = t("Кінець має бути пізніше початку."),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Button(
            onClick = {
                if (endTime <= startTime) {
                    showTimeError = true
                    return@Button
                }

                onSave(
                    ScheduleRule(
                        id = initialRule?.id ?: UUID.randomUUID().toString(),
                        courseId = linkedCourseId,
                        lessonType = lessonType,
                        title = title.trim(),
                        dayOfWeek = day,
                        startTime = startTime,
                        endTime = endTime,
                        recurrence = recurrence,
                        room = room.trim().ifBlank { null },
                    )
                )
            },
            enabled = title.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (initialRule == null) t("Зберегти") else t("Оновити"))
        }
    }
}

private fun DayOfWeek.shortLabel(): String = when (this) {
    DayOfWeek.MONDAY -> t("Пн")
    DayOfWeek.TUESDAY -> t("Вт")
    DayOfWeek.WEDNESDAY -> t("Ср")
    DayOfWeek.THURSDAY -> t("Чт")
    DayOfWeek.FRIDAY -> t("Пт")
    DayOfWeek.SATURDAY -> t("Сб")
    DayOfWeek.SUNDAY -> t("Нд")
}
