package com.kpyruy.takt.feature.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
    initialEvent: OneOffScheduleEvent? = null,
    initialType: OneOffScheduleEventType = OneOffScheduleEventType.EXTRA,
    onDismiss: () -> Unit,
    onSave: (OneOffScheduleEvent) -> Unit,
) {
    var title by remember(initialEvent) { mutableStateOf(initialEvent?.title.orEmpty()) }
    var date by remember(initialEvent, initialDate) {
        mutableStateOf(initialEvent?.date ?: initialDate)
    }
    var startTime by remember(initialEvent) {
        mutableStateOf(initialEvent?.startTime ?: LocalTime.of(8, 0))
    }
    var endTime by remember(initialEvent) {
        mutableStateOf(initialEvent?.endTime ?: LocalTime.of(9, 50))
    }
    var room by remember(initialEvent) { mutableStateOf(initialEvent?.room.orEmpty()) }
    var type by remember(initialEvent, initialType) {
        mutableStateOf(initialEvent?.type ?: initialType)
    }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                if (initialEvent == null) "Разова подія" else "Редагувати подію",
                style = MaterialTheme.typography.headlineSmall,
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Назва") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = type == OneOffScheduleEventType.EXTRA,
                    onClick = { type = OneOffScheduleEventType.EXTRA },
                    label = { Text("Додаткова пара") },
                )
                FilterChip(
                    selected = type == OneOffScheduleEventType.BLOCK_ACTION,
                    onClick = { type = OneOffScheduleEventType.BLOCK_ACTION },
                    label = { Text("Блокова акція") },
                )
            }

            TaktDatePickerField(
                label = "Дата",
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
                    label = "Початок",
                    value = startTime,
                    onValueChange = {
                        startTime = it
                        error = null
                    },
                    modifier = Modifier.weight(1f),
                )
                TaktTimePickerField(
                    label = "Кінець",
                    value = endTime,
                    onValueChange = {
                        endTime = it
                        error = null
                    },
                    modifier = Modifier.weight(1f),
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
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Button(
                onClick = {
                    if (endTime <= startTime) {
                        error = "Кінець має бути пізніше початку."
                        return@Button
                    }
                    onSave(
                        OneOffScheduleEvent(
                            id = initialEvent?.id ?: UUID.randomUUID().toString(),
                            courseId = initialEvent?.courseId,
                            title = title.trim(),
                            date = date,
                            startTime = startTime,
                            endTime = endTime,
                            room = room.trim().ifBlank { null },
                            type = type,
                        )
                    )
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (initialEvent == null) "Додати подію" else "Оновити")
            }
        }
    }
}
