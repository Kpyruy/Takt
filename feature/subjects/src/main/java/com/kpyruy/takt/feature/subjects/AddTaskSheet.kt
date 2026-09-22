package com.kpyruy.takt.feature.subjects

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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.StudyTask
import com.kpyruy.takt.core.ui.components.TaktDatePickerField
import java.time.LocalDate
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskSheet(
    courseId: String,
    initialTask: StudyTask? = null,
    onDismiss: () -> Unit,
    onSave: (StudyTask) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        AddTaskForm(
            courseId = courseId,
            initialTask = initialTask,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            onSave = onSave,
        )
    }
}

@Composable
fun AddTaskForm(
    courseId: String,
    initialTask: StudyTask? = null,
    initialTitle: String = "",
    initialDescription: String = "",
    initialDueDate: LocalDate? = null,
    modifier: Modifier = Modifier,
    showHeading: Boolean = true,
    onSave: (StudyTask) -> Unit,
) {
    var title by remember(initialTask?.id, initialTitle) {
        mutableStateOf(initialTask?.title ?: initialTitle)
    }
    var description by remember(initialTask?.id, initialDescription) {
        mutableStateOf(initialTask?.description ?: initialDescription)
    }
    var dueDate by remember(initialTask?.id, initialDueDate) {
        mutableStateOf(initialTask?.dueDate ?: initialDueDate)
    }
    var requiredForExam by remember(initialTask?.id) {
        mutableStateOf(initialTask?.requiredForExam ?: false)
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (showHeading) {
            Text(
                if (initialTask == null) "Нове завдання" else "Редагувати завдання",
                style = MaterialTheme.typography.headlineSmall,
            )
        }
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Назва") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Опис") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
        )
        TaktDatePickerField(
            label = "Дедлайн",
            value = dueDate,
            onValueChange = { dueDate = it },
            modifier = Modifier.fillMaxWidth(),
        )
        if (dueDate != null) {
            TextButton(onClick = { dueDate = null }) { Text("Без дедлайну") }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Потрібно для допуску до екзамену")
                Text(
                    "Позначай навіть роботи без балів.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = requiredForExam, onCheckedChange = { requiredForExam = it })
        }

        Button(
            onClick = {
                onSave(
                    StudyTask(
                        id = initialTask?.id ?: UUID.randomUUID().toString(),
                        courseId = courseId,
                        title = title.trim(),
                        description = description.trim().ifBlank { null },
                        dueDate = dueDate,
                        completed = initialTask?.completed ?: false,
                        requiredForExam = requiredForExam,
                    )
                )
            },
            enabled = title.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (initialTask == null) "Зберегти" else "Оновити")
        }
    }
}
