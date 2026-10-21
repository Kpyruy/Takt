package com.kpyruy.takt.feature.subjects

import com.kpyruy.takt.core.model.uiMessage
import com.kpyruy.takt.core.model.UiKey
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.StudyTask
import com.kpyruy.takt.core.ui.components.*
import com.kpyruy.takt.core.ui.i18n.t
import java.time.LocalDate
import java.util.UUID

@Composable
fun AddTaskSheet(courseId: String, initialTask: StudyTask? = null, onDismiss: () -> Unit, onSave: (StudyTask) -> Unit) {
    TaktFullSheet(onDismissRequest = onDismiss) {
        AddTaskForm(courseId, initialTask, modifier = Modifier.fillMaxWidth()
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp), onSave = onSave)
    }
}

/** Same compact work-editor layout; existing task IDs, descriptions and optional scores stay intact. */
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
    var title by rememberSaveable(initialTask?.id, initialTitle) { mutableStateOf(initialTask?.title ?: initialTitle) }
    var description by rememberSaveable(initialTask?.id, initialDescription) { mutableStateOf(initialTask?.description ?: initialDescription) }
    var dueDate by rememberSaveable(initialTask?.id, initialDueDate) { mutableStateOf(initialTask?.dueDate ?: initialDueDate) }
    var requiredForExam by rememberSaveable(initialTask?.id, initialRequiredForExam) { mutableStateOf(initialTask?.requiredForExam ?: initialRequiredForExam) }
    var completed by rememberSaveable(initialTask?.id) { mutableStateOf(initialTask?.completed ?: false) }
    var earnedText by rememberSaveable(initialTask?.id) { mutableStateOf(initialTask?.earnedPoints?.toTaskPointText().orEmpty()) }
    var maxText by rememberSaveable(initialTask?.id) { mutableStateOf(initialTask?.maxPoints?.toTaskPointText().orEmpty()) }
    var minimumText by rememberSaveable(initialTask?.id) { mutableStateOf(initialTask?.minimumPointsForExam?.toTaskPointText().orEmpty()) }
    var detailsVisible by rememberSaveable(initialTask?.id) { mutableStateOf(false) }
    var detailsTab by rememberSaveable(initialTask?.id) { mutableStateOf(GradeDetailsTab.WHEN) }
    var errorTab by rememberSaveable(initialTask?.id) { mutableStateOf<GradeDetailsTab?>(null) }
    var error by rememberSaveable(initialTask?.id) { mutableStateOf<String?>(null) }
    fun invalid(tab: GradeDetailsTab, message: String) { error = message; errorTab = tab; detailsTab = tab; detailsVisible = true }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (showHeading) Text(t(if (initialTask == null) UiKey.WORK_ADD_TASK else UiKey.WORK_EDIT_TASK), style = MaterialTheme.typography.headlineMedium)
        TaktTextField(title, { title = it; error = null }, label = { Text(t(UiKey.COMMON_NAME)) },
            modifier = Modifier.fillMaxWidth().testTag("task-title"), singleLine = true)
        Text(t(UiKey.WORK_TYPE), style = MaterialTheme.typography.titleSmall)
        TaktSurface(Modifier.fillMaxWidth().testTag("task-type-HOMEWORK"), shape = RoundedCornerShape(16.dp)) {
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Outlined.Assignment, null)
                Text(t(UiKey.WORK_TYPE_HOMEWORK))
            }
        }
        WorkDetailsButton { detailsVisible = true }
        if (errorTab == null) error?.let { Text(t(it), color = MaterialTheme.colorScheme.error) }
        TaktPrimaryButton(onClick = {
            error = null; errorTab = null
            val max = maxText.takeIf(String::isNotBlank)?.replace(',', '.')?.toDoubleOrNull()
            val earned = earnedText.takeIf(String::isNotBlank)?.replace(',', '.')?.toDoubleOrNull()
            val minimumInput = minimumText.takeIf { requiredForExam && it.isNotBlank() }
            val minimum = minimumInput?.replace(',', '.')?.toDoubleOrNull()
            when {
                maxText.isNotBlank() && (max == null || !max.isFinite() || max <= 0.0) -> invalid(GradeDetailsTab.RESULT, uiMessage(UiKey.TEXT_MAXIMUM_MUST_BE_GREATER_THAN_0))
                earnedText.isNotBlank() && (earned == null || !earned.isFinite() || earned < 0.0 || max == null || earned > max) ->
                    invalid(GradeDetailsTab.RESULT, uiMessage(UiKey.TEXT_THE_OBTAINED_POINTS_CANNOT_EXCEED_THE_MAXIMUM))
                minimumInput != null && (minimum == null || !minimum.isFinite() || minimum < 0.0 || max == null || minimum > max) ->
                    invalid(GradeDetailsTab.RULES, uiMessage(UiKey.TEXT_THE_MINIMUM_MUST_BE_FROM_0_TO_THE_MAXIMUM_OF_POINTS))
                else -> onSave(StudyTask(initialTask?.id ?: UUID.randomUUID().toString(), courseId, title.trim(),
                    description.trim().ifBlank { null }, dueDate, earned != null || completed, requiredForExam,
                    earned, max, minimum))
            }
        }, enabled = title.isNotBlank(), modifier = Modifier.fillMaxWidth().testTag("task-save")) {
            Text(t(if (initialTask == null) UiKey.ACTION_SAVE else UiKey.ACTION_UPDATE))
        }
    }
    if (detailsVisible) GradeDetailsSheet(detailsTab, { detailsTab = it }, { detailsVisible = false }, error.takeIf { errorTab == detailsTab }) {
        when (detailsTab) {
            GradeDetailsTab.WHEN -> {
                TaktTextField(description, { description = it }, label = { Text(t(UiKey.WORK_DESCRIPTION)) },
                    modifier = Modifier.fillMaxWidth().testTag("task-description"), minLines = 2)
                TaktDatePickerField(t(UiKey.WORK_DEADLINE), dueDate, { dueDate = it }, Modifier.fillMaxWidth().testTag("task-date"))
                if (dueDate != null) TextButton(onClick = { dueDate = null }) { Text(t(UiKey.TEXT_NO_DEADLINE)) }
            }
            GradeDetailsTab.RESULT -> {
                TaktTextField(maxText, {
                    maxText = it; error = null
                    if (it.isBlank()) { earnedText = ""; minimumText = "" }
                }, label = { Text(t(UiKey.WORK_MAXIMUM_POINTS)) },
                    modifier = Modifier.fillMaxWidth().testTag("task-maximum"), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), isError = error != null && errorTab == GradeDetailsTab.RESULT)
                Text(t(UiKey.TEXT_LEAVE_EMPTY_FOR_UNGRADED_HOMEWORK), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)

                GradeRuleToggle("task-completed", UiKey.TEXT_WORK_DONE, completed) {
                    completed = it
                    if (!it) earnedText = ""
                }
                if (maxText.isNotBlank()) TaktTextField(earnedText, { earnedText = it; error = null },
                    label = { Text(t(UiKey.WORK_EARNED_POINTS)) }, modifier = Modifier.fillMaxWidth().testTag("task-earned"),
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            }
            GradeDetailsTab.RULES -> {
                GradeRuleToggle("task-required", UiKey.TEXT_REQUIRED_FOR_THE_EXAM, requiredForExam) { requiredForExam = it }
                if (requiredForExam && maxText.isNotBlank()) TaktTextField(minimumText, { minimumText = it; error = null },
                    label = { Text(t(UiKey.TEXT_MINIMUM_POINTS_FOR_EXAM_ELIGIBILITY)) }, modifier = Modifier.fillMaxWidth().testTag("task-minimum"),
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            }
        }
    }
}

private fun Double.toTaskPointText() = if (this % 1.0 == 0.0) toLong().toString() else toString()
