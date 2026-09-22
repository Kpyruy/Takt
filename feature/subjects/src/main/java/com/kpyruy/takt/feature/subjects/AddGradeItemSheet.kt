package com.kpyruy.takt.feature.subjects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.GradeItem
import com.kpyruy.takt.core.model.GradeItemType
import com.kpyruy.takt.core.ui.components.TaktDatePickerField
import java.time.LocalDate
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGradeItemSheet(
    courseId: String,
    initialType: GradeItemType = GradeItemType.TEST,
    onDismiss: () -> Unit,
    onSave: (GradeItem) -> Unit,
) {
    var title by remember(initialType) {
        mutableStateOf(if (initialType == GradeItemType.EXAM) "Екзамен" else "")
    }
    var earnedText by remember { mutableStateOf("0") }
    var maxText by remember { mutableStateOf("") }
    var type by remember(initialType) { mutableStateOf(initialType) }
    var dueDate by remember { mutableStateOf<LocalDate?>(null) }
    var completed by remember(initialType) { mutableStateOf(initialType != GradeItemType.EXAM) }
    var requiredForExam by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                if (initialType == GradeItemType.EXAM) "Додати екзамен" else "Додати оцінювання",
                style = MaterialTheme.typography.headlineSmall,
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Назва") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Text("Тип", style = MaterialTheme.typography.titleSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    GradeTypeChips(
                        selected = type,
                        onSelected = {
                            type = it
                            if (it == GradeItemType.EXAM) requiredForExam = false
                        },
                    )
                }
            }

            TaktDatePickerField(
                label = "Дата / дедлайн",
                value = dueDate,
                onValueChange = { dueDate = it },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(if (type == GradeItemType.EXAM) "Екзамен складено" else "Результат уже відомий")
                    Text(
                        if (completed) "Бали зафіксовані" else "Ще можна заробити ці бали",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = completed,
                    onCheckedChange = {
                        completed = it
                        if (!it) earnedText = "0"
                    },
                )
            }

            if (type != GradeItemType.EXAM) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Потрібно для допуску")
                        Text(
                            "Окремо від кількості балів.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = requiredForExam, onCheckedChange = { requiredForExam = it })
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = earnedText,
                    onValueChange = {
                        earnedText = it
                        error = null
                    },
                    enabled = completed,
                    label = { Text("Отримано") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    isError = error != null,
                )
                OutlinedTextField(
                    value = maxText,
                    onValueChange = {
                        maxText = it
                        error = null
                    },
                    label = { Text("Максимум") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    isError = error != null,
                )
            }

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    val earned = if (completed) earnedText.replace(',', '.').toDoubleOrNull() else 0.0
                    val max = maxText.replace(',', '.').toDoubleOrNull()
                    when {
                        earned == null || max == null -> error = "Введіть числові значення балів."
                        earned < 0.0 -> error = "Отримані бали не можуть бути від'ємними."
                        max <= 0.0 -> error = "Максимум має бути більшим за 0."
                        earned > max -> error = "Отримані бали не можуть перевищувати максимум."
                        else -> onSave(
                            GradeItem(
                                id = UUID.randomUUID().toString(),
                                courseId = courseId,
                                title = title.trim(),
                                type = type,
                                earnedPoints = earned,
                                maxPoints = max,
                                recordedAtEpochMillis = System.currentTimeMillis(),
                                dueDate = dueDate,
                                completed = completed,
                                requiredForExam = requiredForExam,
                            )
                        )
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Зберегти")
            }
        }
    }
}

@Composable
private fun GradeTypeChips(
    selected: GradeItemType,
    onSelected: (GradeItemType) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            GradeItemType.TEST to "Тест",
            GradeItemType.MIDTERM to "Модуль",
            GradeItemType.LAB to "Лаба",
            GradeItemType.SEMINAR to "Семінар",
            GradeItemType.HOMEWORK to "ДЗ",
            GradeItemType.PROJECT to "Проєкт",
            GradeItemType.ORAL to "Усне",
            GradeItemType.EXAM to "Екзамен",
            GradeItemType.OTHER to "Інше",
        ).forEach { (option, label) ->
            FilterChip(
                selected = selected == option,
                onClick = { onSelected(option) },
                label = { Text(label) },
            )
        }
    }
}
