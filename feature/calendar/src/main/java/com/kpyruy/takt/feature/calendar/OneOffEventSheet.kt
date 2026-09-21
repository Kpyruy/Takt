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
import com.kpyruy.takt.core.model.OneOffEventDraft
import com.kpyruy.takt.core.model.OneOffScheduleEvent
import com.kpyruy.takt.core.model.OneOffScheduleEventType
import java.time.LocalDate
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OneOffEventSheet(
    initialDate: LocalDate,
    initialEvent: OneOffScheduleEvent? = null,
    onDismiss: () -> Unit,
    onSave: (OneOffScheduleEvent) -> Unit,
) {
    var title by remember(initialEvent) { mutableStateOf(initialEvent?.title.orEmpty()) }
    var dateText by remember(initialEvent, initialDate) {
        mutableStateOf((initialEvent?.date ?: initialDate).toString())
    }
    var startText by remember(initialEvent) {
        mutableStateOf(initialEvent?.startTime?.toString() ?: "08:00")
    }
    var endText by remember(initialEvent) {
        mutableStateOf(initialEvent?.endTime?.toString() ?: "09:50")
    }
    var room by remember(initialEvent) { mutableStateOf(initialEvent?.room.orEmpty()) }
    var blockAction by remember(initialEvent) {
        mutableStateOf(initialEvent?.type == OneOffScheduleEventType.BLOCK_ACTION)
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
                    selected = !blockAction,
                    onClick = { blockAction = false },
                    label = { Text("Додаткова пара") },
                )
                FilterChip(
                    selected = blockAction,
                    onClick = { blockAction = true },
                    label = { Text("Блокова акція") },
                )
            }

            OutlinedTextField(
                value = dateText,
                onValueChange = {
                    dateText = it
                    error = null
                },
                label = { Text("Дата") },
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
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Button(
                onClick = {
                    val result = OneOffEventDraft(
                        title = title,
                        date = dateText,
                        startTime = startText,
                        endTime = endText,
                        room = room,
                        blockAction = blockAction,
                    ).toEvent(
                        id = initialEvent?.id ?: UUID.randomUUID().toString(),
                        courseId = initialEvent?.courseId,
                    )

                    result.fold(
                        onSuccess = onSave,
                        onFailure = {
                            error = "Перевір назву, дату YYYY-MM-DD і час HH:mm."
                        },
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
