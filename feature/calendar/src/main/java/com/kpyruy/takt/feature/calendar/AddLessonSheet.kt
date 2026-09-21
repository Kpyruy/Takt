package com.kpyruy.takt.feature.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.ScheduleRecurrence
import com.kpyruy.takt.core.model.ScheduleRule
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLessonSheet(
    initialDay: DayOfWeek,
    initialRule: ScheduleRule? = null,
    onDismiss: () -> Unit,
    onSave: (ScheduleRule) -> Unit,
) {
    var title by remember(initialRule) { mutableStateOf(initialRule?.title.orEmpty()) }
    var room by remember(initialRule) { mutableStateOf(initialRule?.room.orEmpty()) }
    var startText by remember(initialRule) {
        mutableStateOf(initialRule?.startTime?.toString() ?: "08:00")
    }
    var endText by remember(initialRule) {
        mutableStateOf(initialRule?.endTime?.toString() ?: "09:50")
    }
    var day by remember(initialRule, initialDay) {
        mutableStateOf(initialRule?.dayOfWeek ?: initialDay)
    }
    var recurrence by remember(initialRule) {
        mutableStateOf(initialRule?.recurrence ?: ScheduleRecurrence.WEEKLY)
    }
    var showTimeError by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = if (initialRule == null) "Додати пару" else "Редагувати пару",
                style = MaterialTheme.typography.headlineSmall,
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Назва предмета") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            OutlinedTextField(
                value = room,
                onValueChange = { room = it },
                label = { Text("Аудиторія") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Text("День тижня", style = MaterialTheme.typography.titleSmall)
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
                OutlinedTextField(
                    value = startText,
                    onValueChange = {
                        startText = it
                        showTimeError = false
                    },
                    label = { Text("Початок") },
                    placeholder = { Text("08:00") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    isError = showTimeError,
                )
                OutlinedTextField(
                    value = endText,
                    onValueChange = {
                        endText = it
                        showTimeError = false
                    },
                    label = { Text("Кінець") },
                    placeholder = { Text("09:50") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    isError = showTimeError,
                )
            }

            Text("Повторення", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    ScheduleRecurrence.WEEKLY to "Щотижня",
                    ScheduleRecurrence.ODD_WEEKS to "Непарні",
                    ScheduleRecurrence.EVEN_WEEKS to "Парні",
                ).forEach { (option, label) ->
                    FilterChip(
                        selected = recurrence == option,
                        onClick = { recurrence = option },
                        label = { Text(label) },
                    )
                }
            }

            if (showTimeError) {
                Text(
                    text = "Перевір формат часу та переконайся, що кінець пізніше початку.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Button(
                onClick = {
                    val start = runCatching { LocalTime.parse(startText) }.getOrNull()
                    val end = runCatching { LocalTime.parse(endText) }.getOrNull()
                    if (start == null || end == null || end <= start) {
                        showTimeError = true
                        return@Button
                    }

                    onSave(
                        ScheduleRule(
                            id = initialRule?.id ?: UUID.randomUUID().toString(),
                            courseId = initialRule?.courseId,
                            title = title.trim(),
                            dayOfWeek = day,
                            startTime = start,
                            endTime = end,
                            recurrence = recurrence,
                            room = room.trim().ifBlank { null },
                        )
                    )
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (initialRule == null) "Зберегти" else "Оновити")
            }
        }
    }
}

private fun DayOfWeek.shortLabel(): String = when (this) {
    DayOfWeek.MONDAY -> "Пн"
    DayOfWeek.TUESDAY -> "Вт"
    DayOfWeek.WEDNESDAY -> "Ср"
    DayOfWeek.THURSDAY -> "Чт"
    DayOfWeek.FRIDAY -> "Пт"
    DayOfWeek.SATURDAY -> "Сб"
    DayOfWeek.SUNDAY -> "Нд"
}
