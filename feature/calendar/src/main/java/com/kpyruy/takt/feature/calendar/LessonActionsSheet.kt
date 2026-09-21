package com.kpyruy.takt.feature.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.ResolvedScheduleEvent
import com.kpyruy.takt.core.model.ScheduleEventActions
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonActionsSheet(
    event: ResolvedScheduleEvent,
    isRecurringRule: Boolean,
    onDismiss: () -> Unit,
    onCancelOccurrence: () -> Unit,
    onMoveOccurrence: () -> Unit,
    onRestoreOccurrence: () -> Unit,
    onEditRule: () -> Unit,
    onDeleteRule: () -> Unit,
    onEditOneOff: () -> Unit,
    onDeleteOneOff: () -> Unit,
) {
    val time = DateTimeFormatter.ofPattern("HH:mm")
    val actions = ScheduleEventActions.forEvent(event, isRecurringRule)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(event.title, style = MaterialTheme.typography.headlineSmall)
            Text(
                "${event.date} · ${event.startTime.format(time)}–${event.endTime.format(time)}" +
                    if (event.room.isNullOrBlank()) "" else " · ${event.room}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (actions.canCancelOccurrence) {
                Button(onClick = onCancelOccurrence, modifier = Modifier.fillMaxWidth()) {
                    Text("Скасувати тільки цю пару")
                }
            }
            if (actions.canMoveOccurrence) {
                OutlinedButton(onClick = onMoveOccurrence, modifier = Modifier.fillMaxWidth()) {
                    Text("Перенести цю пару")
                }
            }
            if (actions.canRestoreOccurrence) {
                Button(onClick = onRestoreOccurrence, modifier = Modifier.fillMaxWidth()) {
                    Text("Повернути початкову пару")
                }
            }
            if (actions.canEditRecurringRule) {
                OutlinedButton(onClick = onEditRule, modifier = Modifier.fillMaxWidth()) {
                    Text("Редагувати повторення")
                }
            }
            if (actions.canDeleteRecurringRule) {
                OutlinedButton(onClick = onDeleteRule, modifier = Modifier.fillMaxWidth()) {
                    Text("Видалити з розкладу")
                }
            }
            if (actions.canEditOneOff) {
                Button(onClick = onEditOneOff, modifier = Modifier.fillMaxWidth()) {
                    Text("Редагувати разову подію")
                }
            }
            if (actions.canDeleteOneOff) {
                OutlinedButton(onClick = onDeleteOneOff, modifier = Modifier.fillMaxWidth()) {
                    Text("Видалити разову подію")
                }
            }
        }
    }
}
