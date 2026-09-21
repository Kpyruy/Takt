package com.kpyruy.takt.feature.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.kpyruy.takt.core.model.ResolvedScheduleEvent
import com.kpyruy.takt.core.model.ScheduleException
import com.kpyruy.takt.core.model.ScheduleExceptionType
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveLessonSheet(
    event: ResolvedScheduleEvent,
    onDismiss: () -> Unit,
    onSave: (ScheduleException) -> Unit,
) {
    var dateText by remember(event) { mutableStateOf(event.date.toString()) }
    var startText by remember(event) { mutableStateOf(event.startTime.toString()) }
    var endText by remember(event) { mutableStateOf(event.endTime.toString()) }
    var room by remember(event) { mutableStateOf(event.room.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Перенести пару", style = MaterialTheme.typography.headlineSmall)
            Text(event.title, color = MaterialTheme.colorScheme.onSurfaceVariant)

            OutlinedTextField(
                value = dateText,
                onValueChange = {
                    dateText = it
                    error = null
                },
                label = { Text("Нова дата") },
                placeholder = { Text("2026-09-25") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = error != null,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = startText,
                    onValueChange = {
                        startText = it
                        error = null
                    },
                    label = { Text("Початок") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    isError = error != null,
                )
                OutlinedTextField(
                    value = endText,
                    onValueChange = {
                        endText = it
                        error = null
                    },
                    label = { Text("Кінець") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    isError = error != null,
                )
            }

            OutlinedTextField(
                value = room,
                onValueChange = { room = it },
                label = { Text("Аудиторія") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    val targetDate = runCatching { LocalDate.parse(dateText) }.getOrNull()
                    val start = runCatching { LocalTime.parse(startText) }.getOrNull()
                    val end = runCatching { LocalTime.parse(endText) }.getOrNull()
                    if (targetDate == null || start == null || end == null || end <= start) {
                        error = "Перевір дату у форматі YYYY-MM-DD і час у форматі HH:mm."
                        return@Button
                    }
                    onSave(
                        ScheduleException(
                            id = event.exceptionId ?: UUID.randomUUID().toString(),
                            ruleId = event.id,
                            date = event.sourceDate ?: event.date,
                            type = ScheduleExceptionType.MOVED,
                            replacementDate = targetDate,
                            replacementStartTime = start,
                            replacementEndTime = end,
                            replacementRoom = room.trim().ifBlank { null },
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Перенести")
            }
        }
    }
}
