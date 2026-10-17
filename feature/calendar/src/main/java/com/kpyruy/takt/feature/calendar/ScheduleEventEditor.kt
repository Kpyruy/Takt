package com.kpyruy.takt.feature.calendar

import com.kpyruy.takt.core.ui.components.rememberSaveAction
import com.kpyruy.takt.core.ui.components.SaveErrorDialog
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.kpyruy.takt.core.ui.components.ResolvedEventStateSaver
import com.kpyruy.takt.core.ui.components.ScheduleRuleStateSaver
import com.kpyruy.takt.core.ui.components.OneOffEventStateSaver
import com.kpyruy.takt.core.data.ScheduleRepository
import com.kpyruy.takt.core.model.*
import java.util.UUID
import kotlinx.coroutines.launch

@Composable
fun ScheduleEventEditor(
    event: ResolvedScheduleEvent,
    scheduleRepository: ScheduleRepository,
    courses: List<Course>,
    onDismiss: () -> Unit,
) {
    val rules by scheduleRepository.observeRules().collectAsState(initial = emptyList())
    val oneOffEvents by scheduleRepository.observeOneOffEvents().collectAsState(initial = emptyList())
    val saves = rememberSaveAction()
    SaveErrorDialog(saves)
    var showActions by rememberSaveable(event.id, event.date.toEpochDay()) { mutableStateOf(true) }
    val selectedEvent = event.takeIf { showActions }
    var editingRule by rememberSaveable(stateSaver = ScheduleRuleStateSaver) { mutableStateOf<ScheduleRule?>(null) }
    var editingOneOff by rememberSaveable(stateSaver = OneOffEventStateSaver) { mutableStateOf<OneOffScheduleEvent?>(null) }
    var movingEvent by rememberSaveable(stateSaver = ResolvedEventStateSaver) { mutableStateOf<ResolvedScheduleEvent?>(null) }
    LaunchedEffect(selectedEvent, editingRule, editingOneOff, movingEvent) {
        if (selectedEvent == null && editingRule == null && editingOneOff == null && movingEvent == null) onDismiss()
    }
    selectedEvent?.let { event ->
        val recurringRule = rules.firstOrNull { it.id == event.id }
        val oneOff = oneOffEvents.firstOrNull { it.id == event.id }

        LessonActionsSheet(
            event = event,
            isRecurringRule = recurringRule != null,
            onDismiss = { showActions = false },
            onToggleAbsence = {
                saves.launch { scheduleRepository.setAbsent(event, !event.isAbsent); showActions = false }
            },
            onCancelOccurrence = {
                saves.launch {
                    scheduleRepository.upsertException(
                        ScheduleException(
                            id = UUID.randomUUID().toString(),
                            ruleId = event.id,
                            date = event.date,
                            type = ScheduleExceptionType.CANCELLED,
                        )
                    )
                    showActions = false
                }
            },
            onMoveOccurrence = {
                movingEvent = event
                showActions = false
            },
            onRestoreOccurrence = {
                saves.launch {
                    event.exceptionId?.let { scheduleRepository.deleteException(it) }
                    showActions = false
                }
            },
            onEditRule = {
                editingRule = recurringRule
                showActions = false
            },
            onDeleteRule = {
                saves.launch {
                    scheduleRepository.deleteRule(event.id)
                    showActions = false
                }
            },
            onEditOneOff = {
                editingOneOff = oneOff
                showActions = false
            },
            onDeleteOneOff = {
                saves.launch {
                    scheduleRepository.deleteOneOffEvent(event.id)
                    showActions = false
                }
            },
        )
    }

    editingRule?.let { rule ->
        AddLessonSheet(
            courses = courses,
            initialDay = rule.dayOfWeek,
            initialRule = rule,
            initialDate = event.date,
            onSaveOneOff = {
                saves.launch {
                    scheduleRepository.convertRuleToOneOff(rule.id, it)
                    editingRule = null
                }
            },
            onDismiss = { editingRule = null },
            onSave = {
                saves.launch {
                    scheduleRepository.upsertRule(it)
                    editingRule = null
                }
            },
        )
    }

    editingOneOff?.let { event ->
        if (event.type == OneOffScheduleEventType.BLOCK_ACTION) AddLessonSheet(
            courses = courses,
            initialDay = event.date.dayOfWeek,
            initialOneOff = event,
            initialDate = event.date,
            onDismiss = { editingOneOff = null },
            onSaveOneOff = {
                saves.launch { scheduleRepository.upsertOneOffEvent(it); editingOneOff = null }
            },
            onSave = {
                saves.launch {
                    scheduleRepository.convertOneOffToRule(event.id, it)
                    editingOneOff = null
                }
            },
        ) else OneOffEventSheet(
            courses = courses,
            initialDate = event.date,
            initialEvent = event,
            onDismiss = { editingOneOff = null },
            onSave = {
                saves.launch { scheduleRepository.upsertOneOffEvent(it); editingOneOff = null }
            },
        )
    }

    movingEvent?.let { event ->
        MoveLessonSheet(
            event = event,
            onDismiss = { movingEvent = null },
            onSave = { exception ->
                saves.launch {
                    scheduleRepository.upsertException(exception)
                    movingEvent = null
                }
            },
        )
    }
}
