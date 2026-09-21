package com.kpyruy.takt.feature.subjects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.kpyruy.takt.core.model.StudyTask
import com.kpyruy.takt.core.ui.components.TaktDatePickerField
import java.time.LocalDate
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskSheet(
    courseId: String,
    onDismiss: () -> Unit,
    onSave: (StudyTask) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf<LocalDate?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Нове завдання", style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Назва") },
                placeholder = { Text("Домашня робота") },
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
                TextButton(onClick = { dueDate = null }) {
                    Text("Без дедлайну")
                }
            }
            Button(
                onClick = {
                    onSave(
                        StudyTask(
                            id = UUID.randomUUID().toString(),
                            courseId = courseId,
                            title = title.trim(),
                            description = description.trim().ifBlank { null },
                            dueDate = dueDate,
                            completed = false,
                        )
                    )
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Зберегти")
            }
        }
    }
}
