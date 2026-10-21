package com.kpyruy.takt.feature.subjects

import com.kpyruy.takt.core.ui.i18n.t

import com.kpyruy.takt.core.ui.components.TaktFullSheet
import com.kpyruy.takt.core.ui.components.taktPressFeedback
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.kpyruy.takt.core.ui.components.TaktPrimaryButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.kpyruy.takt.core.ui.components.TaktTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.GradeItem
import com.kpyruy.takt.core.model.GradeItemType
import com.kpyruy.takt.core.data.AppSettingsRepository
import com.kpyruy.takt.core.data.ScheduleRepository
import com.kpyruy.takt.core.ui.components.TaktDatePickerField
import com.kpyruy.takt.core.ui.components.TaktChoiceMenu
import com.kpyruy.takt.core.ui.components.TaktChoiceMenuItem
import com.kpyruy.takt.core.ui.components.TaktSurface
import com.kpyruy.takt.core.ui.components.TaktSegmentedTabs
import com.kpyruy.takt.core.ui.components.LessonTypeIcon
import java.time.LocalDate
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGradeItemSheet(
    courseId: String,
    academicSemester: Int? = null,
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
            academicSemester = academicSemester,
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
    academicSemester: Int? = null,
    scheduleRepository: ScheduleRepository? = null,
    settingsRepository: AppSettingsRepository? = null,
    initialItem: GradeItem? = null,
    initialType: GradeItemType = GradeItemType.TEST,
    initialTitle: String = "",
    initialDueDate: LocalDate? = null,
    initialRequiredForExam: Boolean = false,
    modifier: Modifier = Modifier,
    showHeading: Boolean = true,
    onSave: (GradeItem) -> Unit,
) {
    val effectiveType = (initialItem?.type ?: initialType).let {
        if (it == GradeItemType.MIDTERM) GradeItemType.TEST else it
    }
    var title by rememberSaveable(initialItem?.id, initialTitle, effectiveType) {
        mutableStateOf(
            initialItem?.title
                ?: initialTitle.ifBlank { if (effectiveType == GradeItemType.EXAM) t("Exam") else "" }
        )
    }
    var earnedText by rememberSaveable(initialItem?.id) {
        mutableStateOf(initialItem?.earnedPoints?.toEditableNumber() ?: "0")
    }
    var maxText by rememberSaveable(initialItem?.id) {
        mutableStateOf(initialItem?.maxPoints?.toEditableNumber().orEmpty())
    }
    var minimumText by rememberSaveable(initialItem?.id) {
        mutableStateOf(initialItem?.minimumPointsForExam?.toEditableNumber().orEmpty())
    }
    var durationText by rememberSaveable(initialItem?.id) {
        mutableStateOf(initialItem?.durationMinutes?.toString().orEmpty())
    }
    var type by rememberSaveable(initialItem?.id, initialType) { mutableStateOf(effectiveType) }
    var dueDate by rememberSaveable(initialItem?.id, initialDueDate) { mutableStateOf<LocalDate?>(initialItem?.dueDate ?: initialDueDate) }
    var selectedTeachingWeek by rememberSaveable(initialItem?.id) { mutableStateOf<Int?>(null) }
    var lessonId by rememberSaveable(initialItem?.id) { mutableStateOf(initialItem?.lessonId) }
    var completed by rememberSaveable(initialItem?.id, effectiveType) {
        mutableStateOf(initialItem?.completed ?: false)
    }
    var requiredForExam by rememberSaveable(initialItem?.id) {
        mutableStateOf(initialItem?.requiredForExam ?: initialRequiredForExam)
    }
    var contributesToGrade by rememberSaveable(initialItem?.id) { mutableStateOf(initialItem?.contributesToGrade ?: true) }
    var awaitingResult by rememberSaveable(initialItem?.id) { mutableStateOf(initialItem?.isAwaitingResult ?: false) }
    var detailsVisible by rememberSaveable(initialItem?.id) { mutableStateOf(false) }
    var detailsTab by rememberSaveable(initialItem?.id) { mutableStateOf(GradeDetailsTab.WHEN) }
    var dateMode by rememberSaveable(initialItem?.id) { mutableStateOf(GradeDateMode.DATE) }
    var errorTab by rememberSaveable { mutableStateOf<GradeDetailsTab?>(null) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val teachingWeeks = rememberTeachingWeekOptions(scheduleRepository, settingsRepository, courseId, academicSemester)
    val lessonOptions = rememberGradeLessonOptions(scheduleRepository, settingsRepository, courseId, dueDate)
    val selectedLessonId = if (lessonOptions.isEmpty()) lessonId else {
        lessonId?.takeIf { selected -> lessonOptions.any { it.id == selected } }
            ?: lessonOptions.first().id
    }

    fun invalidDetails(tab: GradeDetailsTab, message: String) {
        detailsTab = tab
        errorTab = tab
        error = message
        detailsVisible = true
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (showHeading) {
            Text(
                when {
                    initialItem != null -> t("Edit assessment")
                    initialType == GradeItemType.EXAM -> t("Add an exam")
                    initialType == GradeItemType.TEST -> t("Add test")
                    else -> t("Add assessment")
                },
                style = MaterialTheme.typography.headlineMedium,
            )
        }

        TaktTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text(t("Name")) },
            modifier = Modifier.fillMaxWidth().testTag("grade-title"),
            singleLine = true,
        )

        Text(t("Type"), style = MaterialTheme.typography.titleSmall)
        GradeTypeSelector(
            selected = type,
            onSelected = {
                type = it
                if (it == GradeItemType.EXAM) { requiredForExam = false; selectedTeachingWeek = null }
            },
        )

        // A normal single-line field sizes itself; do not stretch it to another field's wrapped label.
        TaktTextField(
            value = maxText, onValueChange = { maxText = it; error = null },
            label = { Text(t("Maximum")) }, modifier = Modifier.fillMaxWidth().testTag("grade-maximum"),
            singleLine = true, isError = error != null && errorTab == null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )

        WorkDetailsButton { detailsVisible = true }
        if (errorTab == null) error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.testTag("grade-form-error"))
        }

        TaktPrimaryButton(
            onClick = {
                val earned = if (completed) earnedText.replace(',', '.').toDoubleOrNull() else 0.0
                val max = maxText.replace(',', '.').toDoubleOrNull()
                val minimumInput = minimumText.takeIf { requiredForExam && type != GradeItemType.EXAM && it.isNotBlank() }
                val minimum = minimumInput?.replace(',', '.')?.toDoubleOrNull()
                val durationInput = durationText.takeIf {
                    (type == GradeItemType.TEST || type == GradeItemType.MIDTERM) && it.isNotBlank()
                }
                val duration = durationInput?.toIntOrNull()
                error = null
                errorTab = null
                when {
                    max == null -> error = t("Enter the numerical values of the points.")
                    !max.isFinite() || max <= 0.0 -> error = t("Maximum must be greater than 0.")
                    earned == null -> invalidDetails(GradeDetailsTab.RESULT, t("Enter the numerical values of the points."))
                    !earned.isFinite() || earned < 0.0 ->
                        invalidDetails(GradeDetailsTab.RESULT, t("The obtained points cannot be negative."))
                    earned > max -> invalidDetails(GradeDetailsTab.RESULT, t("The obtained points cannot exceed the maximum."))
                    selectedTeachingWeek != null && dueDate == null && teachingWeeks.weeks
                        .any { it.number == selectedTeachingWeek && it.occurrences.size > 1 } -> {
                        dateMode = GradeDateMode.SCHEDULE
                        invalidDetails(GradeDetailsTab.WHEN, t("Choose a lesson this week or enter a date."))
                    }
                    minimumInput != null && minimum == null ->
                        invalidDetails(GradeDetailsTab.RULES, t("Enter the numerical minimum score."))
                    minimum != null && (!minimum.isFinite() || minimum < 0.0 || minimum > max) ->
                        invalidDetails(GradeDetailsTab.RULES, t("The minimum must be from 0 to the maximum of points."))
                    durationInput != null && (duration == null || duration <= 0) ->
                        invalidDetails(GradeDetailsTab.WHEN, t("The duration of the test must be a positive number of minutes."))
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
                            contributesToGrade = contributesToGrade,
                            awaitingResult = awaitingResult && !completed,
                        )
                    )
                }
            },
            enabled = title.isNotBlank(),
            modifier = Modifier.fillMaxWidth().testTag("grade-save"),
        ) {
            Text(if (initialItem == null) t("Save") else t("Update"))
        }
    }

    if (detailsVisible) GradeDetailsSheet(
        selectedTab = detailsTab,
        onTabSelected = { detailsTab = it },
        onDismiss = { detailsVisible = false },
        error = error.takeIf { errorTab == detailsTab },
    ) {
        when (detailsTab) {
            GradeDetailsTab.WHEN -> {
                if (type != GradeItemType.EXAM) TaktSegmentedTabs(
                    labels = GradeDateMode.entries.map { t(it.label) },
                    selectedIndex = dateMode.ordinal,
                    onSelected = { dateMode = GradeDateMode.entries[it] },
                    railCornerRadius = 12.dp,
                    tabTag = { "grade-date-mode-${GradeDateMode.entries[it].name}" },
                )
                if (type != GradeItemType.EXAM && dateMode == GradeDateMode.SCHEDULE) {
                    TeachingWeekDateSelector(
                        options = teachingWeeks, selectedWeek = selectedTeachingWeek, dueDate = dueDate, lessonId = lessonId,
                        onSelectWeek = { week ->
                            selectedTeachingWeek = week.number
                            // A week with multiple lessons remains unresolved until an occurrence is chosen.
                            val single = week.occurrences.singleOrNull()
                            dueDate = single?.date
                            lessonId = single?.id
                            error = null
                        },
                        onSelectOccurrence = { occurrence -> dueDate = occurrence.date; lessonId = occurrence.id; error = null },
                    )
                } else {
                    TaktDatePickerField(
                        label = if (type == GradeItemType.TEST || type == GradeItemType.MIDTERM) t("Test date") else t("Deadline / date"),
                        value = dueDate,
                        onValueChange = {
                            if (dueDate != it) lessonId = null
                            dueDate = it
                            selectedTeachingWeek = null
                            error = null
                        },
                        modifier = Modifier.fillMaxWidth().testTag("grade-explicit-date"),
                    )
                    if ((type == GradeItemType.TEST || type == GradeItemType.MIDTERM) && selectedTeachingWeek == null) {
                        if (lessonOptions.isNotEmpty()) GradeLessonSelector(
                            lessons = lessonOptions, selectedId = selectedLessonId,
                            onSelected = { lessonId = it.id; error = null },
                        ) else if (dueDate != null && scheduleRepository != null) {
                            Text(t("There are no classes for this subject on this date"), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (type == GradeItemType.TEST || type == GradeItemType.MIDTERM) TaktTextField(
                    value = durationText, onValueChange = { durationText = it; error = null },
                    label = { Text(t("Test duration (min)")) },
                    modifier = Modifier.fillMaxWidth().testTag("grade-duration"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, isError = error != null && errorTab == GradeDetailsTab.WHEN,
                )
            }
            GradeDetailsTab.RESULT -> {
                val result = when {
                    completed -> GradeResultState.KNOWN
                    awaitingResult -> GradeResultState.AWAITING
                    else -> GradeResultState.PLANNED
                }
                GradeResultChoices(result) {
                    completed = it == GradeResultState.KNOWN
                    awaitingResult = it == GradeResultState.AWAITING
                    // Keep typed points as a draft while trying another status.
                    error = null
                }
                if (completed) TaktTextField(
                    value = earnedText, onValueChange = { earnedText = it; error = null },
                    label = { Text(t("Received")) }, modifier = Modifier.fillMaxWidth().testTag("grade-earned"),
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = error != null && errorTab == GradeDetailsTab.RESULT,
                )
            }
            GradeDetailsTab.RULES -> {
                GradeRuleToggle("grade-contributes", "Points count toward the final grade", contributesToGrade) {
                    contributesToGrade = it
                    error = null
                }
                if (type != GradeItemType.EXAM) {
                    GradeRuleToggle("grade-required", "Required for exam eligibility", requiredForExam) {
                        requiredForExam = it
                        error = null
                    }
                    if (requiredForExam) TaktTextField(
                        value = minimumText, onValueChange = { minimumText = it; error = null },
                        label = { Text(t("Minimum")) }, modifier = Modifier.fillMaxWidth().testTag("grade-minimum"),
                        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = error != null && errorTab == GradeDetailsTab.RULES,
                    )
                }
            }
        }
    }

}

@Composable
private fun GradeTypeSelector(selected: GradeItemType, onSelected: (GradeItemType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val choices = listOf(
        GradeItemType.TEST to t("Test"), GradeItemType.LAB to t("Laboratory"),
        GradeItemType.HOMEWORK to t("Homework (short)"),
        GradeItemType.PROJECT to t("Project"), GradeItemType.ORAL to t("Oral"),
        GradeItemType.EXAM to t("Exam"), GradeItemType.OTHER to t("Other"),
    )
    Box(Modifier.fillMaxWidth()) {
        GradeChoiceTrigger("grade-type-trigger", { expanded = true },
            icon = { GradeTypeIcon(selected) },
            text = { Text(choices.firstOrNull { it.first == selected }?.second ?: t(selected.label)) })
        TaktChoiceMenu(expanded, onDismissRequest = { expanded = false }) {
            choices.forEach { (option, label) ->
                TaktChoiceMenuItem(text = { Text(label) }, leadingIcon = { GradeTypeIcon(option) },
                    modifier = Modifier.testTag("grade-type-${option.name}").semantics { this.selected = selected == option },
                    trailingIcon = { if (selected == option) Icon(Icons.Outlined.Check, null) },
                    onClick = { onSelected(option); expanded = false })
            }
        }
    }
}

@Composable
private fun GradeTypeIcon(type: GradeItemType) {
    Icon(when (type) {
        GradeItemType.TEST, GradeItemType.MIDTERM -> Icons.Outlined.Quiz
        GradeItemType.LAB -> Icons.Outlined.Science
        GradeItemType.SEMINAR -> Icons.Outlined.Forum
        GradeItemType.HOMEWORK -> Icons.Outlined.Assignment
        GradeItemType.PROJECT -> Icons.Outlined.AccountTree
        GradeItemType.ORAL -> Icons.Outlined.Mic
        GradeItemType.EXAM -> Icons.Outlined.School
        GradeItemType.OTHER -> Icons.Outlined.MoreHoriz
    }, contentDescription = null)
}

@Composable
private fun GradeLessonSelector(
    lessons: List<com.kpyruy.takt.core.model.ResolvedScheduleEvent>,
    selectedId: String?,
    onSelected: (com.kpyruy.takt.core.model.ResolvedScheduleEvent) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLesson = lessons.firstOrNull { it.id == selectedId }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(t("Class with a test"), style = MaterialTheme.typography.titleSmall)
        Box(Modifier.fillMaxWidth()) {
            GradeChoiceTrigger("grade-date-lesson-trigger", { expanded = true },
                icon = { selectedLesson?.let { LessonTypeIcon(it.lessonType) } },
                text = { selectedLesson?.let { Text("${it.startTime}–${it.endTime}" + it.room?.let { " · $it" }.orEmpty()) }
                    ?: Text(t("Class with a test")) })
            TaktChoiceMenu(expanded, onDismissRequest = { expanded = false }) {
                lessons.forEach { lesson ->
                    TaktChoiceMenuItem(text = {
                        Column {
                            Text("${lesson.startTime}–${lesson.endTime} · ${lesson.title}")
                            Text(t(lesson.lessonType.label) + lesson.room?.let { " · $it" }.orEmpty(),
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }, leadingIcon = { LessonTypeIcon(lesson.lessonType) },
                        trailingIcon = { if (lesson.id == selectedId) Icon(Icons.Outlined.Check, null) },
                        modifier = Modifier.testTag("grade-date-lesson-${lesson.id}")
                            .semantics { selected = lesson.id == selectedId },
                        onClick = { onSelected(lesson); expanded = false })
                }
            }
        }
    }
}

@Composable
private fun GradeChoiceTrigger(
    tag: String,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    text: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    val interaction = remember { MutableInteractionSource() }
    TaktSurface(modifier = Modifier.fillMaxWidth().taktPressFeedback(interaction).clip(shape)
        .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
        .testTag(tag), shape = shape) {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            icon()
            Box(Modifier.weight(1f)) { text() }
            Icon(Icons.Outlined.ExpandMore, null)
        }
    }
}

private fun Double.toEditableNumber(): String =
    if (this % 1.0 == 0.0) toInt().toString() else toString()
