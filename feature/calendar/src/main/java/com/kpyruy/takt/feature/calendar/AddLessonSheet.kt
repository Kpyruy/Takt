package com.kpyruy.takt.feature.calendar

import com.kpyruy.takt.core.model.UiKey
import com.kpyruy.takt.core.ui.i18n.t
import androidx.compose.runtime.saveable.rememberSaveable
import com.kpyruy.takt.core.ui.components.LocalDateStateSaver
import com.kpyruy.takt.core.ui.components.LocalTimeStateSaver

import com.kpyruy.takt.core.ui.components.TaktFullSheet
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import com.kpyruy.takt.core.ui.components.TaktPrimaryButton as Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.kpyruy.takt.core.ui.components.TaktTextField as OutlinedTextField
import androidx.compose.material3.Text
import com.kpyruy.takt.core.ui.components.TaktSurface as Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.kpyruy.takt.core.model.Course
import com.kpyruy.takt.core.data.UisLessonOption
import com.kpyruy.takt.core.model.LessonType
import com.kpyruy.takt.core.ui.components.TaktChoiceMenu
import com.kpyruy.takt.core.ui.components.TaktChoiceMenuItem
import com.kpyruy.takt.core.ui.components.LessonTypeSelector
import com.kpyruy.takt.core.ui.components.glassSurface
import com.kpyruy.takt.core.ui.components.taktPressFeedback
import com.kpyruy.takt.core.ui.components.CourseLinkSelector
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.ScheduleRecurrence
import com.kpyruy.takt.core.model.ScheduleRule
import com.kpyruy.takt.core.model.OneOffScheduleEvent
import com.kpyruy.takt.core.model.LessonScheduleSave
import com.kpyruy.takt.core.model.prepareLessonSchedule
import com.kpyruy.takt.core.ui.components.TaktTimePickerField
import com.kpyruy.takt.core.ui.components.TaktDatePickerField
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLessonSheet(
    initialDay: DayOfWeek,
    courses: List<Course> = emptyList(),
    initialRule: ScheduleRule? = null,
    initialOneOff: OneOffScheduleEvent? = null,
    initialDate: LocalDate? = null,
    onDismiss: () -> Unit,
    onSaveOneOff: ((OneOffScheduleEvent) -> Unit)? = null,
    onSave: (ScheduleRule) -> Unit,
) {
    TaktFullSheet(onDismissRequest = onDismiss) {
        AddLessonForm(
            initialDay = initialDay,
            initialRule = initialRule,
            initialOneOff = initialOneOff,
            initialDate = initialDate,
            courses = courses,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            onSaveOneOff = onSaveOneOff,
            onSave = onSave,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddLessonForm(
    initialDay: DayOfWeek,
    courses: List<Course> = emptyList(),
    initialRule: ScheduleRule? = null,
    initialOneOff: OneOffScheduleEvent? = null,
    initialDate: LocalDate? = null,
    initialTitle: String = "",
    initialCourseId: String? = null,
    initialLessonType: LessonType = LessonType.UNSPECIFIED,
    initialRoom: String = "",
    initialStartTime: LocalTime? = null,
    initialEndTime: LocalTime? = null,
    modifier: Modifier = Modifier,
    showHeading: Boolean = true,
    onLoadUisLessons: (suspend (Course) -> List<UisLessonOption>)? = null,
    onSaveOneOff: ((OneOffScheduleEvent) -> Unit)? = null,
    onSave: (ScheduleRule) -> Unit,
) {
    val editingId = initialRule?.id ?: initialOneOff?.id
    var linkedCourseId by rememberSaveable(editingId, initialCourseId) { mutableStateOf(initialRule?.courseId ?: initialOneOff?.courseId ?: initialCourseId) }
    var lessonType by rememberSaveable(editingId, initialLessonType) { mutableStateOf(initialRule?.lessonType ?: initialOneOff?.lessonType ?: initialLessonType) }
    var title by rememberSaveable(editingId, initialTitle) {
        mutableStateOf(initialRule?.title ?: initialOneOff?.title ?: initialTitle)
    }
    var room by rememberSaveable(editingId, initialRoom) {
        mutableStateOf(initialRule?.room ?: initialOneOff?.room ?: initialRoom)
    }
    var startTime by rememberSaveable(editingId, initialStartTime, stateSaver = LocalTimeStateSaver) {
        mutableStateOf(initialRule?.startTime ?: initialOneOff?.startTime ?: initialStartTime ?: LocalTime.of(8, 0))
    }
    var endTime by rememberSaveable(editingId, initialEndTime, stateSaver = LocalTimeStateSaver) {
        mutableStateOf(initialRule?.endTime ?: initialOneOff?.endTime ?: initialEndTime ?: LocalTime.of(9, 50))
    }
    var day by rememberSaveable(editingId, initialDay) {
        mutableStateOf(initialRule?.dayOfWeek ?: initialOneOff?.date?.dayOfWeek ?: initialDay)
    }
    var recurrence by rememberSaveable(editingId) {
        mutableStateOf(initialRule?.recurrence ?: ScheduleRecurrence.WEEKLY)
    }
    var repeatMenuExpanded by remember { mutableStateOf(false) }
    val linkedCourse = courses.firstOrNull { it.id == linkedCourseId }
    val savedTitle = if (lessonType == LessonType.EVENT) title else linkedCourse?.title ?: title
    var showTimeError by remember { mutableStateOf(false) }
    var uisLessons by remember { mutableStateOf<List<UisLessonOption>>(emptyList()) }
    var uisLoading by remember { mutableStateOf(false) }
    var uisLoadFailed by remember { mutableStateOf(false) }
    var uisReload by remember { mutableStateOf(0) }
    var chosenUisKey by remember { mutableStateOf<String?>(null) }
    var isOneOff by rememberSaveable(editingId, initialDate) { mutableStateOf(initialOneOff != null || initialDate != null && initialRule == null) }
    var selectedDate by rememberSaveable(editingId, initialDate, stateSaver = LocalDateStateSaver) { mutableStateOf(initialOneOff?.date ?: initialDate ?: LocalDate.now()) }
    LaunchedEffect(linkedCourseId, uisReload) {
        uisLessons = emptyList()
        chosenUisKey = null
        uisLoadFailed = false
        val course = courses.firstOrNull { it.id == linkedCourseId } ?: return@LaunchedEffect
        val load = onLoadUisLessons ?: return@LaunchedEffect
        uisLoading = true
        try {
            uisLessons = load(course).distinctBy { listOf(it.day, it.start, it.end, it.room, it.lessonType, it.date) }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            uisLoadFailed = true
        } finally {
            uisLoading = false
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (showHeading) {
            Text(
                text = if (editingId == null) t(UiKey.TEXT_ADD_CLASS) else t(UiKey.TEXT_EDIT_CLASS),
                style = MaterialTheme.typography.headlineSmall,
            )
        }

        CourseLinkSelector(courses, linkedCourseId) { course ->
            linkedCourseId = course?.id
        }
        if (onLoadUisLessons != null && linkedCourseId != null) {
            Text(t(UiKey.TEXT_LESSONS_FROM_UIS), style = MaterialTheme.typography.titleSmall)
            when {
                uisLoading -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(Modifier.padding(3.dp), strokeWidth = 2.dp)
                    Text(t(UiKey.TEXT_LOADING_LESSONS_FROM_UIS))
                }
                uisLoadFailed -> Column {
                    Text(t(UiKey.TEXT_COULD_NOT_LOAD_LESSONS_FROM_UIS_CHECK_THE_CONNECTION_IN_SETTINGS),
                        color = MaterialTheme.colorScheme.error)
                    Button(onClick = { uisReload++ }) { Text(t(UiKey.TEXT_TRY_ONCE_MORE)) }
                }
                uisLessons.isEmpty() -> Text(t(UiKey.TEXT_NO_RECURRING_LESSONS_FOR_THIS_SUBJECT_IN_THE_UIS_TIMETABLE),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> uisLessons.forEach { option ->
                    Surface(onClick = {
                        chosenUisKey = option.key
                        day = option.day
                        startTime = option.start
                        endTime = option.end
                        room = option.room.orEmpty()
                        lessonType = option.lessonType
                        isOneOff = option.date != null && onSaveOneOff != null
                        option.date?.let { selectedDate = it }
                        recurrence = ScheduleRecurrence.WEEKLY
                        showTimeError = false
                    }, modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        color = if (chosenUisKey == option.key) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerHigh) {
                        Column(Modifier.padding(12.dp)) {
                            Text("${option.date?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) ?: option.day.shortLabel()} · ${option.start}–${option.end}",
                                style = MaterialTheme.typography.titleSmall)
                            Text("${t(option.lessonType.label)}${if (option.date != null) " · ${t(UiKey.TEXT_ONE_TIME_LESSON)}" else ""}${option.room?.let { " · $it" }.orEmpty()}",
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        if (linkedCourse == null || lessonType == LessonType.EVENT) OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text(if (lessonType == LessonType.EVENT) t(UiKey.COMMON_NAME) else t(UiKey.TEXT_SUBJECT_NAME)) },
            modifier = Modifier.fillMaxWidth().testTag("lesson-custom-title"),
            singleLine = true,
        )

        LessonTypeSelector(lessonType) { lessonType = it }

        OutlinedTextField(
            value = room,
            onValueChange = { room = it },
            label = { Text(if (lessonType == LessonType.EVENT) t(UiKey.TEXT_PLACE) else t(UiKey.TEXT_ROOM_911EA89C)) },
            modifier = Modifier.fillMaxWidth().testTag("lesson-room"),
            singleLine = true,
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(t(UiKey.TEXT_RECURRENCE), style = MaterialTheme.typography.titleSmall)
            val recurrenceOptions = listOf(
                ScheduleRecurrence.WEEKLY to t(UiKey.TEXT_EVERY_WEEK),
                ScheduleRecurrence.ODD_WEEKS to t(UiKey.TEXT_ODD_WEEK_PATTERN),
                ScheduleRecurrence.EVEN_WEEKS to t(UiKey.TEXT_EVEN_WEEK_PATTERN),
            )
            Box(Modifier.fillMaxWidth()) {
                LessonChoiceTrigger(
                    if (isOneOff && onSaveOneOff != null) t(UiKey.TEXT_ONCE) else recurrenceOptions.first { it.first == recurrence }.second,
                    "lesson-repeat-trigger", { repeatMenuExpanded = true },
                ) {
                    Icon(if (isOneOff && onSaveOneOff != null) Icons.Outlined.Event else Icons.Outlined.Repeat, null)
                }
                TaktChoiceMenu(repeatMenuExpanded, onDismissRequest = { repeatMenuExpanded = false }) {
                    if (onSaveOneOff != null) TaktChoiceMenuItem(text = { Text(t(UiKey.TEXT_ONCE)) },
                        modifier = Modifier.testTag("lesson-repeat-once").semantics { selected = isOneOff },
                        leadingIcon = { Icon(Icons.Outlined.Event, null) },
                        trailingIcon = { if (isOneOff) Icon(Icons.Outlined.Check, null) },
                        onClick = { isOneOff = true; chosenUisKey = null; repeatMenuExpanded = false })
                    recurrenceOptions.forEach { (option, label) ->
                        TaktChoiceMenuItem(text = { Text(label) },
                            modifier = Modifier.testTag("lesson-repeat-${option.name}")
                                .semantics { selected = !isOneOff && recurrence == option },
                            leadingIcon = { Icon(if (option == ScheduleRecurrence.WEEKLY) Icons.Outlined.Repeat else Icons.Outlined.DateRange, null) },
                            trailingIcon = { if (!isOneOff && recurrence == option) Icon(Icons.Outlined.Check, null) },
                            onClick = { isOneOff = false; recurrence = option; chosenUisKey = null; repeatMenuExpanded = false })
                    }
                }
            }
        }
        if (isOneOff && onSaveOneOff != null) {
            TaktDatePickerField(label = t(UiKey.TEXT_LESSON_DATE), value = selectedDate,
                onValueChange = { selectedDate = it; chosenUisKey = null },
                modifier = Modifier.fillMaxWidth().testTag("lesson-once-date"))
        } else {
            Text(t(UiKey.TEXT_DAY_OF_THE_WEEK), style = MaterialTheme.typography.titleSmall)
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DayOfWeek.entries.forEach { option ->
                    LessonChoiceChip(option.shortLabel(), day == option,
                        onClick = { day = option; chosenUisKey = null })
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TaktTimePickerField(
                label = t(UiKey.TEXT_START_TIME),
                value = startTime,
                onValueChange = {
                    startTime = it
                    showTimeError = false
                },
                modifier = Modifier.weight(1f),
            )
            TaktTimePickerField(
                label = t(UiKey.TEXT_END),
                value = endTime,
                onValueChange = {
                    endTime = it
                    showTimeError = false
                },
                modifier = Modifier.weight(1f),
            )
        }

        if (showTimeError) {
            Text(
                text = t(UiKey.TEXT_THE_END_MUST_BE_LATER_THAN_THE_BEGINNING),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Button(
            onClick = {
                if (endTime <= startTime) {
                    showTimeError = true
                    return@Button
                }

                when (val saved = prepareLessonSchedule(
                    id = editingId ?: UUID.randomUUID().toString(), courseId = linkedCourseId,
                    title = savedTitle, lessonType = lessonType, room = room, startTime = startTime,
                    endTime = endTime, day = day,
                    recurrence = if (isOneOff && onSaveOneOff != null) null else recurrence,
                    date = selectedDate,
                )) {
                    is LessonScheduleSave.Once -> onSaveOneOff?.invoke(saved.event)
                    is LessonScheduleSave.Recurring -> onSave(saved.rule)
                }
            },
            enabled = savedTitle.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (editingId == null) t(UiKey.ACTION_SAVE) else t(UiKey.ACTION_UPDATE))
        }
    }
}

private fun DayOfWeek.shortLabel(): String = when (this) {
    DayOfWeek.MONDAY -> t(UiKey.TEXT_MON)
    DayOfWeek.TUESDAY -> t(UiKey.TEXT_TUE)
    DayOfWeek.WEDNESDAY -> t(UiKey.TEXT_WED)
    DayOfWeek.THURSDAY -> t(UiKey.TEXT_THU)
    DayOfWeek.FRIDAY -> t(UiKey.TEXT_FRI)
    DayOfWeek.SATURDAY -> t(UiKey.TEXT_SAT)
    DayOfWeek.SUNDAY -> t(UiKey.TEXT_SUN)
}

@Composable
private fun LessonChoiceTrigger(label: String, tag: String, onClick: () -> Unit, icon: @Composable () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    val interaction = remember { MutableInteractionSource() }
    Surface(modifier = Modifier.fillMaxWidth().taktPressFeedback(interaction).clip(shape)
        .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
        .testTag(tag), shape = shape) {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            icon()
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Icon(Icons.Outlined.ExpandMore, null)
        }
    }
}

/** The complete label and icon stay in one chip; rows wrap between chips only. */
@Composable
private fun LessonChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(12.dp)
    val interaction = remember { MutableInteractionSource() }
    Row(modifier.taktPressFeedback(interaction).clip(shape).glassSurface(shape,
        if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Unspecified)
        .selectable(selected = selected, role = Role.RadioButton,
            interactionSource = interaction, indication = null, onClick = onClick)
        .heightIn(min = 44.dp).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        leadingIcon?.invoke()
        Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false)
    }
}
