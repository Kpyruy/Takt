package com.kpyruy.takt.feature.calendar

import androidx.compose.runtime.*
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
    val scope = rememberCoroutineScope()
    var selectedEvent by remember(event) { mutableStateOf<ResolvedScheduleEvent?>(event) }
    var editingRule by remember { mutableStateOf<ScheduleRule?>(null) }
    var editingOneOff by remember { mutableStateOf<OneOffScheduleEvent?>(null) }
    var movingEvent by remember { mutableStateOf<ResolvedScheduleEvent?>(null) }
    LaunchedEffect(selectedEvent, editingRule, editingOneOff, movingEvent) {
        if (selectedEvent == null && editingRule == null && editingOneOff == null && movingEvent == null) onDismiss()
    }
    selectedEvent?.let { event ->
        val recurringRule = rules.firstOrNull { it.id == event.id }
        val oneOff = oneOffEvents.firstOrNull { it.id == event.id }

        LessonActionsSheet(
            event = event,
            isRecurringRule = recurringRule != null,
            onDismiss = { selectedEvent = null },
            onToggleAbsence = {
                scope.launch { scheduleRepository.setAbsent(event, !event.isAbsent); selectedEvent = null }
            },
            onCancelOccurrence = {
                scope.launch {
                    scheduleRepository.upsertException(
                        ScheduleException(
                            id = UUID.randomUUID().toString(),
                            ruleId = event.id,
                            date = event.date,
                            type = ScheduleExceptionType.CANCELLED,
                        )
                    )
                    selectedEvent = null
                }
            },
            onMoveOccurrence = {
                movingEvent = event
                selectedEvent = null
            },
            onRestoreOccurrence = {
                scope.launch {
                    event.exceptionId?.let { scheduleRepository.deleteException(it) }
                    selectedEvent = null
                }
            },
            onEditRule = {
                editingRule = recurringRule
                selectedEvent = null
            },
            onDeleteRule = {
                scope.launch {
                    scheduleRepository.deleteRule(event.id)
                    selectedEvent = null
                }
            },
            onEditOneOff = {
                editingOneOff = oneOff
                selectedEvent = null
            },
            onDeleteOneOff = {
                scope.launch {
                    scheduleRepository.deleteOneOffEvent(event.id)
                    selectedEvent = null
                }
            },
        )
    }

    editingRule?.let { rule ->
        AddLessonSheet(
            courses = courses,
            initialDay = rule.dayOfWeek,
            initialRule = rule,
            onDismiss = { editingRule = null },
            onSave = {
                scope.launch {
                    scheduleRepository.upsertRule(it)
                    editingRule = null
                }
            },
        )
    }

    editingOneOff?.let { event ->
        OneOffEventSheet(
            courses = courses,
            initialDate = event.date,
            initialEvent = event,
            onDismiss = { editingOneOff = null },
            onSave = {
                scope.launch {
                    scheduleRepository.upsertOneOffEvent(it)
                    editingOneOff = null
                }
            },
        )
    }

    movingEvent?.let { event ->
        MoveLessonSheet(
            event = event,
            onDismiss = { movingEvent = null },
            onSave = { exception ->
                scope.launch {
                    scheduleRepository.upsertException(exception)
                    movingEvent = null
                }
            },
        )
    }
}
