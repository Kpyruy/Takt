package com.kpyruy.takt.feature.subjects

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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.ExamInfo
import com.kpyruy.takt.core.model.GradeItem
import com.kpyruy.takt.core.model.GradeItemType
import com.kpyruy.takt.core.ui.components.TaktDatePickerField
import com.kpyruy.takt.core.ui.components.TaktTimePickerField
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditExamInfoSheet(
    courseId: String,
    initial: ExamInfo?,
    gradeItems: List<GradeItem>,
    onDismiss: () -> Unit,
    onSave: (ExamInfo) -> Unit,
) {
    val examItems = gradeItems.filter { it.type == GradeItemType.EXAM }
    var date by remember(initial) { mutableStateOf(initial?.date) }
    var startTime by remember(initial) { mutableStateOf(initial?.startTime ?: LocalTime.of(9, 0)) }
    var endTime by remember(initial) { mutableStateOf(initial?.endTime ?: LocalTime.of(10, 30)) }
    var room by remember(initial) { mutableStateOf(initial?.room.orEmpty()) }
    var attempt by remember(initial) { mutableStateOf(initial?.attemptNumber ?: 1) }
    var notes by remember(initial) { mutableStateOf(initial?.notes.orEmpty()) }
    var linkedGradeItemId by remember(initial) { mutableStateOf(initial?.gradeItemId) }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Екзамен", style = MaterialTheme.typography.headlineSmall)

            TaktDatePickerField(
                label = "Дата",
                value = date,
                onValueChange = {
                    date = it
                    error = null
                },
                modifier = Modifier.fillMaxWidth(),
            )
            if (date != null) {
                TextButton(onClick = { date = null }) {
                    Text("Дата ще не відома")
                }

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
            }

            OutlinedTextField(
                value = room,
                onValueChange = { room = it },
                label = { Text("Аудиторія") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Text("Спроба · максимум 3", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..3).forEach { number ->
                    FilterChip(
                        selected = attempt == number,
                        onClick = { attempt = number },
                        label = { Text(number.toString() + "/3") },
                    )
                }
            }

            if (examItems.isNotEmpty()) {
                Text("Пов'язаний результат", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = linkedGradeItemId == null,
                            onClick = { linkedGradeItemId = null },
                            label = { Text("Не вибрано") },
                        )
                    }
                    items(examItems, key = { it.id }) { item ->
                        FilterChip(
                            selected = linkedGradeItemId == item.id,
                            onClick = { linkedGradeItemId = item.id },
                            label = { Text(item.title, maxLines = 1) },
                        )
                    }
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Нотатки") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    if (date != null && endTime <= startTime) {
                        error = "Кінець має бути пізніше початку."
                        return@Button
                    }
                    onSave(
                        ExamInfo(
                            courseId = courseId,
                            gradeItemId = linkedGradeItemId,
                            date = date,
                            startTime = if (date == null) null else startTime,
                            endTime = if (date == null) null else endTime,
                            room = room.trim().ifBlank { null },
                            attemptNumber = attempt,
                            maxAttempts = 3,
                            notes = notes.trim(),
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Зберегти")
            }
        }
    }
}
