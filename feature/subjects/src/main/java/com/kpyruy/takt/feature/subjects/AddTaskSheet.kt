package com.kpyruy.takt.feature.subjects

import com.kpyruy.takt.core.ui.components.TaktFullSheet
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
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
    TaktFullSheet(onDismissRequest = onDismiss) {
        AddTaskForm(
            courseId = courseId,
            initialTask = initialTask,
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
fun AddTaskForm(
    courseId: String,
    initialTask: StudyTask? = null,
    initialTitle: String = "",
    initialDescription: String = "",
    initialDueDate: LocalDate? = null,
    initialRequiredForExam: Boolean = false,
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
    var requiredForExam by remember(initialTask?.id, initialRequiredForExam) {
        mutableStateOf(initialTask?.requiredForExam ?: initialRequiredForExam)
    }
    var earnedText by remember(initialTask?.id) {
        mutableStateOf(initialTask?.earnedPoints?.toTaskPointText().orEmpty())
    }
    var maxText by remember(initialTask?.id) {
        mutableStateOf(initialTask?.maxPoints?.toTaskPointText().orEmpty())
    }
    var minimumText by remember(initialTask?.id) {
        mutableStateOf(initialTask?.minimumPointsForExam?.toTaskPointText().orEmpty())
    }
    var error by remember(initialTask?.id) { mutableStateOf<String?>(null) }

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
            modifier = Modifier.fillMaxWidth().toggleable(
                value = requiredForExam,
                role = Role.Checkbox,
                onValueChange = { requiredForExam = it },
            ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = requiredForExam, onCheckedChange = null)
            Text("Потрібно для допуску до екзамену")
        }

        Text("Бали (необов’язково)", style = MaterialTheme.typography.titleSmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = earnedText,
                onValueChange = { earnedText = it; error = null },
                label = { Text("Отримано") },
                enabled = maxText.isNotBlank(),
                modifier = Modifier.weight(1f),
                singleLine = true,
                isError = error != null,
            )
            OutlinedTextField(
                value = maxText,
                onValueChange = {
                    maxText = it
                    if (it.isBlank()) { earnedText = ""; minimumText = "" }
                    error = null
                },
                label = { Text("Максимум") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                isError = error != null,
            )
        }
        if (requiredForExam && maxText.isNotBlank()) {
            OutlinedTextField(
                value = minimumText,
                onValueChange = { minimumText = it; error = null },
                label = { Text("Мінімум балів для допуску") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = error != null,
            )
        }

        error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }

        Button(
            onClick = {
                val maxInput = maxText.trim().takeIf { it.isNotEmpty() }
                val max = maxInput?.replace(',', '.')?.toDoubleOrNull()
                val earnedInput = earnedText.trim().takeIf { maxInput != null && it.isNotEmpty() }
                val earned = earnedInput?.replace(',', '.')?.toDoubleOrNull()
                val minimumInput = minimumText.trim().takeIf { requiredForExam && maxInput != null && it.isNotEmpty() }
                val minimum = minimumInput?.replace(',', '.')?.toDoubleOrNull()
                when {
                    maxInput != null && (max == null || !max.isFinite() || max <= 0.0) ->
                        error = "Максимум має бути числом більшим за 0."
                    earnedInput != null && (earned == null || !earned.isFinite() || earned < 0.0 || earned > (max ?: 0.0)) ->
                        error = "Отримані бали мають бути від 0 до максимуму."
                    minimumInput != null && (minimum == null || !minimum.isFinite() || minimum < 0.0 || minimum > (max ?: 0.0)) ->
                        error = "Мінімум має бути від 0 до максимуму балів."
                    else -> onSave(
                        StudyTask(
                            id = initialTask?.id ?: UUID.randomUUID().toString(),
                            courseId = courseId,
                            title = title.trim(),
                            description = description.trim().ifBlank { null },
                            dueDate = dueDate,
                            completed = earned != null || initialTask?.completed == true,
                            requiredForExam = requiredForExam,
                            earnedPoints = earned,
                            maxPoints = max,
                            minimumPointsForExam = minimum,
                        )
                    )
                }
            },
            enabled = title.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (initialTask == null) "Зберегти" else "Оновити")
        }
    }
}

private fun Double.toTaskPointText(): String =
    if (this % 1.0 == 0.0) toInt().toString() else toString()
