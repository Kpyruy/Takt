package com.kpyruy.takt.feature.subjects

import com.kpyruy.takt.core.ui.i18n.t

import com.kpyruy.takt.core.ui.components.TaktFullSheet
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.GradeItem
import com.kpyruy.takt.core.model.GradeItemType
import com.kpyruy.takt.core.data.AppSettingsRepository
import com.kpyruy.takt.core.data.ScheduleRepository
import com.kpyruy.takt.core.ui.components.TaktDatePickerField
import java.time.LocalDate
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGradeItemSheet(
    courseId: String,
    scheduleRepository: ScheduleRepository? = null,
    settingsRepository: AppSettingsRepository? = null,
    initialItem: GradeItem? = null,
    initialType: GradeItemType = GradeItemType.TEST,
    onDismiss: () -> Unit,
    onSave: (GradeItem) -> Unit,
) {
    TaktFullSheet(onDismissRequest = onDismiss) {
        AddGradeItemForm(
            courseId = courseId,
            scheduleRepository = scheduleRepository,
            settingsRepository = settingsRepository,
            initialItem = initialItem,
            initialType = initialType,
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
fun AddGradeItemForm(
    courseId: String,
    scheduleRepository: ScheduleRepository? = null,
    settingsRepository: AppSettingsRepository? = null,
    initialItem: GradeItem? = null,
    initialType: GradeItemType = GradeItemType.TEST,
    initialTitle: String = "",
    modifier: Modifier = Modifier,
    showHeading: Boolean = true,
    onSave: (GradeItem) -> Unit,
) {
    val effectiveType = (initialItem?.type ?: initialType).let {
        if (it == GradeItemType.MIDTERM) GradeItemType.TEST else it
    }
    var title by remember(initialItem?.id, initialTitle, effectiveType) {
        mutableStateOf(
            initialItem?.title
                ?: initialTitle.ifBlank { if (effectiveType == GradeItemType.EXAM) t("Екзамен") else "" }
        )
    }
    var earnedText by remember(initialItem?.id) {
        mutableStateOf(initialItem?.earnedPoints?.toEditableNumber() ?: "0")
    }
    var maxText by remember(initialItem?.id) {
        mutableStateOf(initialItem?.maxPoints?.toEditableNumber().orEmpty())
    }
    var minimumText by remember(initialItem?.id) {
        mutableStateOf(initialItem?.minimumPointsForExam?.toEditableNumber().orEmpty())
    }
    var durationText by remember(initialItem?.id) {
        mutableStateOf(initialItem?.durationMinutes?.toString().orEmpty())
    }
    var type by remember(initialItem?.id, initialType) { mutableStateOf(effectiveType) }
    var dueDate by remember(initialItem?.id) { mutableStateOf<LocalDate?>(initialItem?.dueDate) }
    var lessonId by remember(initialItem?.id) { mutableStateOf(initialItem?.lessonId) }
    var completed by remember(initialItem?.id, effectiveType) {
        mutableStateOf(initialItem?.completed ?: false)
    }
    var requiredForExam by remember(initialItem?.id) {
        mutableStateOf(initialItem?.requiredForExam ?: false)
    }
    var error by remember { mutableStateOf<String?>(null) }
    val lessonOptions = rememberGradeLessonOptions(scheduleRepository, settingsRepository, courseId, dueDate)
    val selectedLessonId = if (lessonOptions.isEmpty()) lessonId else {
        lessonId?.takeIf { selected -> lessonOptions.any { it.id == selected } }
            ?: lessonOptions.first().id
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (showHeading) {
            Text(
                when {
                    initialItem != null -> t("Редагувати оцінювання")
                    initialType == GradeItemType.EXAM -> t("Додати екзамен")
                    else -> t("Додати оцінювання")
                },
                style = MaterialTheme.typography.headlineSmall,
            )
        }

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text(t("Назва")) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Text(t("Тип"), style = MaterialTheme.typography.titleSmall)
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
            label = if (type == GradeItemType.TEST || type == GradeItemType.MIDTERM) t("Дата тесту") else t("Дедлайн / дата"),
            value = dueDate,
            onValueChange = {
                if (dueDate != it) lessonId = null
                dueDate = it
            },
            modifier = Modifier.fillMaxWidth(),
        )

        if (type == GradeItemType.TEST || type == GradeItemType.MIDTERM) {
            OutlinedTextField(
                value = durationText,
                onValueChange = { durationText = it; error = null },
                label = { Text(t("Тривалість тесту (хв)")) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                isError = error != null,
            )
            if (lessonOptions.isNotEmpty()) {
                Text(t("Пара з тестом"), style = MaterialTheme.typography.titleSmall)
                lessonOptions.forEach { lesson ->
                    val selected = lesson.id == selectedLessonId
                    Row(
                        modifier = Modifier.fillMaxWidth().selectable(
                            selected = selected,
                            role = Role.RadioButton,
                            onClick = { lessonId = lesson.id },
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        RadioButton(selected = selected, onClick = null)
                        Text("${lesson.startTime}–${lesson.endTime} · ${lesson.title}" +
                            lesson.room?.let { " · $it" }.orEmpty())
                    }
                }
            } else if (dueDate != null && scheduleRepository != null) {
                Text(t("На цю дату немає пари цього предмета"), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(if (type == GradeItemType.EXAM) t("Екзамен складено") else t("Результат уже відомий"))
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
                modifier = Modifier.fillMaxWidth().toggleable(
                    value = requiredForExam,
                    role = Role.Checkbox,
                    onValueChange = { requiredForExam = it },
                ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = requiredForExam, onCheckedChange = null)
                Text(t("Потрібно для допуску"))
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
                label = { Text(t("Отримано")) },
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
                label = { Text(t("Максимум")) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                isError = error != null,
            )
        }

        if (requiredForExam && type != GradeItemType.EXAM) {
            OutlinedTextField(
                value = minimumText,
                onValueChange = { minimumText = it; error = null },
                label = { Text(t("Мінімум балів для допуску")) },
                modifier = Modifier.fillMaxWidth(),
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
                val minimumInput = minimumText.takeIf { requiredForExam && type != GradeItemType.EXAM && it.isNotBlank() }
                val minimum = minimumInput?.replace(',', '.')?.toDoubleOrNull()
                val durationInput = durationText.takeIf {
                    (type == GradeItemType.TEST || type == GradeItemType.MIDTERM) && it.isNotBlank()
                }
                val duration = durationInput?.toIntOrNull()
                when {
                    earned == null || max == null -> error = t("Введіть числові значення балів.")
                    !earned.isFinite() || earned < 0.0 -> error = t("Отримані бали не можуть бути від'ємними.")
                    !max.isFinite() || max <= 0.0 -> error = t("Максимум має бути більшим за 0.")
                    earned > max -> error = t("Отримані бали не можуть перевищувати максимум.")
                    minimumInput != null && minimum == null -> error = t("Введіть числовий мінімум балів.")
                    minimum != null && (!minimum.isFinite() || minimum < 0.0 || minimum > max) ->
                        error = t("Мінімум має бути від 0 до максимуму балів.")
                    durationInput != null && (duration == null || duration <= 0) ->
                        error = t("Тривалість тесту має бути додатним числом хвилин.")
                    else -> onSave(
                        GradeItem(
                            id = initialItem?.id ?: UUID.randomUUID().toString(),
                            courseId = courseId,
                            title = title.trim(),
                            type = type,
                            earnedPoints = earned,
                            maxPoints = max,
                            recordedAtEpochMillis = initialItem?.recordedAtEpochMillis
                                ?: System.currentTimeMillis(),
                            dueDate = dueDate,
                            completed = completed,
                            requiredForExam = requiredForExam,
                            minimumPointsForExam = minimum,
                            lessonId = if (type == GradeItemType.TEST || type == GradeItemType.MIDTERM) selectedLessonId else null,
                            durationMinutes = duration,
                        )
                    )
                }
            },
            enabled = title.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (initialItem == null) t("Зберегти") else t("Оновити"))
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
            GradeItemType.TEST to t("Тест"),
            GradeItemType.LAB to t("Лаба"),
            GradeItemType.SEMINAR to t("Семінар"),
            GradeItemType.HOMEWORK to t("ДЗ"),
            GradeItemType.PROJECT to t("Проєкт"),
            GradeItemType.ORAL to t("Усне"),
            GradeItemType.EXAM to t("Екзамен"),
            GradeItemType.OTHER to t("Інше"),
        ).forEach { (option, label) ->
            FilterChip(
                selected = selected == option,
                onClick = { onSelected(option) },
                label = { Text(label) },
            )
        }
    }
}

private fun Double.toEditableNumber(): String =
    if (this % 1.0 == 0.0) toInt().toString() else toString()
