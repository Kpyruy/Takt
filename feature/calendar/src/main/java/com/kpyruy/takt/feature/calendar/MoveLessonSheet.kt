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
import androidx.compose.material3.MaterialTheme
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
import com.kpyruy.takt.core.ui.components.TaktDatePickerField
import com.kpyruy.takt.core.ui.components.TaktTimePickerField
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveLessonSheet(
    event: ResolvedScheduleEvent,
    onDismiss: () -> Unit,
    onSave: (ScheduleException) -> Unit,
) {
    var targetDate by remember(event) { mutableStateOf(event.date) }
    var startTime by remember(event) { mutableStateOf(event.startTime) }
    var endTime by remember(event) { mutableStateOf(event.endTime) }
    var room by remember(event) { mutableStateOf(event.room.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }

    TaktFullSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(t("Перенести пару"), style = MaterialTheme.typography.headlineSmall)
            Text(event.title, color = MaterialTheme.colorScheme.onSurfaceVariant)

            TaktDatePickerField(
                label = t("Нова дата"),
                value = targetDate,
                onValueChange = {
                    targetDate = it
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

            OutlinedTextField(
                value = room,
                onValueChange = { room = it },
                label = { Text(t("Аудиторія")) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    if (endTime <= startTime) {
                        error = t("Кінець має бути пізніше початку.")
                        return@Button
                    }
                    onSave(
                        ScheduleException(
                            id = event.exceptionId ?: UUID.randomUUID().toString(),
                            ruleId = event.id,
                            date = event.sourceDate ?: event.date,
                            type = ScheduleExceptionType.MOVED,
                            replacementDate = targetDate,
                            replacementStartTime = startTime,
                            replacementEndTime = endTime,
                            replacementRoom = room.trim().ifBlank { null },
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(t("Перенести"))
            }
        }
    }
}
