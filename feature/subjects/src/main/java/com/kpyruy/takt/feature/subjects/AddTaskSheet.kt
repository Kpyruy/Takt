package com.kpyruy.takt.feature.subjects

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
        if (showHeading) Text(t(if (initialTask == null) "Add a task" else "Edit task"), style = MaterialTheme.typography.headlineMedium)
        TaktTextField(title, { title = it; error = null }, label = { Text(t("Name")) },
            modifier = Modifier.fillMaxWidth().testTag("task-title"), singleLine = true)
        Text(t("Type"), style = MaterialTheme.typography.titleSmall)
        TaktSurface(Modifier.fillMaxWidth().testTag("task-type-HOMEWORK"), shape = RoundedCornerShape(16.dp)) {
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Outlined.Assignment, null)
                Text(t("Homework"))
            }
        }
        TaktTextField(maxText, {
            maxText = it; error = null
            if (it.isBlank()) { earnedText = ""; minimumText = "" }
        }, label = { Text(t("Maximum")) }, supportingText = { Text(t("Leave empty for ungraded homework.")) },
            modifier = Modifier.fillMaxWidth().testTag("task-maximum"), singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), isError = error != null && errorTab == null)
        WorkDetailsButton { detailsVisible = true }
        if (errorTab == null) error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        TaktPrimaryButton(onClick = {
            error = null; errorTab = null
            val max = maxText.takeIf(String::isNotBlank)?.replace(',', '.')?.toDoubleOrNull()
            val earned = earnedText.takeIf(String::isNotBlank)?.replace(',', '.')?.toDoubleOrNull()
            val minimumInput = minimumText.takeIf { requiredForExam && it.isNotBlank() }
            val minimum = minimumInput?.replace(',', '.')?.toDoubleOrNull()
            when {
                maxText.isNotBlank() && (max == null || !max.isFinite() || max <= 0.0) -> error = t("Maximum must be greater than 0.")
                earnedText.isNotBlank() && (earned == null || !earned.isFinite() || earned < 0.0 || max == null || earned > max) ->
                    invalid(GradeDetailsTab.RESULT, t("The obtained points cannot exceed the maximum."))
                minimumInput != null && (minimum == null || !minimum.isFinite() || minimum < 0.0 || max == null || minimum > max) ->
                    invalid(GradeDetailsTab.RULES, t("The minimum must be from 0 to the maximum of points."))
                else -> onSave(StudyTask(initialTask?.id ?: UUID.randomUUID().toString(), courseId, title.trim(),
                    description.trim().ifBlank { null }, dueDate, earned != null || completed, requiredForExam,
                    earned, max, minimum))
            }
        }, enabled = title.isNotBlank(), modifier = Modifier.fillMaxWidth().testTag("task-save")) {
            Text(t(if (initialTask == null) "Save" else "Update"))
        }
    }
    if (detailsVisible) GradeDetailsSheet(detailsTab, { detailsTab = it }, { detailsVisible = false }, error.takeIf { errorTab == detailsTab }) {
        when (detailsTab) {
            GradeDetailsTab.WHEN -> {
                TaktTextField(description, { description = it }, label = { Text(t("Description")) },
                    modifier = Modifier.fillMaxWidth().testTag("task-description"), minLines = 2)
                TaktDatePickerField(t("Deadline"), dueDate, { dueDate = it }, Modifier.fillMaxWidth().testTag("task-date"))
                if (dueDate != null) TextButton(onClick = { dueDate = null }) { Text(t("No deadline")) }
            }
            GradeDetailsTab.RESULT -> {
                GradeRuleToggle("task-completed", "Work done", completed) {
                    completed = it
                    if (!it) earnedText = ""
                }
                if (maxText.isNotBlank()) TaktTextField(earnedText, { earnedText = it; error = null },
                    label = { Text(t("Received")) }, modifier = Modifier.fillMaxWidth().testTag("task-earned"),
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            }
            GradeDetailsTab.RULES -> {
                GradeRuleToggle("task-required", "Required for the exam", requiredForExam) { requiredForExam = it }
                if (requiredForExam && maxText.isNotBlank()) TaktTextField(minimumText, { minimumText = it; error = null },
                    label = { Text(t("Minimum points for exam eligibility")) }, modifier = Modifier.fillMaxWidth().testTag("task-minimum"),
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            }
        }
    }
}

private fun Double.toTaskPointText() = if (this % 1.0 == 0.0) toLong().toString() else toString()
