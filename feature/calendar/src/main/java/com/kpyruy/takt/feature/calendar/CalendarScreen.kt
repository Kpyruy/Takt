package com.kpyruy.takt.feature.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.data.ScheduleRepository
import com.kpyruy.takt.core.data.StudyContentRepository
import com.kpyruy.takt.core.data.StudyPlanRepository
import com.kpyruy.takt.core.model.ResolvedScheduleEvent
import com.kpyruy.takt.core.model.ScheduleException
import com.kpyruy.takt.core.model.ScheduleExceptionType
import com.kpyruy.takt.core.model.ScheduleResolver
import com.kpyruy.takt.core.model.ScheduleRule
import com.kpyruy.takt.core.model.WeekParity
import com.kpyruy.takt.core.ui.components.ScreenHeader
import com.kpyruy.takt.core.ui.components.SectionCard
import com.kpyruy.takt.core.ui.components.StatusPill
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.launch

@Composable
fun CalendarScreen(
    scheduleRepository: ScheduleRepository,
    studyContentRepository: StudyContentRepository,
    studyPlanRepository: StudyPlanRepository,
) {
    val rules by scheduleRepository.observeRules().collectAsState(initial = emptyList())
    val oneOffEvents by scheduleRepository.observeOneOffEvents().collectAsState(initial = emptyList())
    val exceptions by scheduleRepository.observeExceptions().collectAsState(initial = emptyList())
    val tasks by studyContentRepository.observeAllTasks().collectAsState(initial = emptyList())
    val courses by studyPlanRepository.observeCourses().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val today = LocalDate.now()
    var selectedDate by remember { mutableStateOf(today) }
    var weekStart by remember {
        mutableStateOf(today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))
    }
    var showAddSheet by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<ScheduleRule?>(null) }
    var selectedEvent by remember { mutableStateOf<ResolvedScheduleEvent?>(null) }
    var movingEvent by remember { mutableStateOf<ResolvedScheduleEvent?>(null) }

    val dates = (0L..6L).map { weekStart.plusDays(it) }
    val week = selectedDate.get(WeekFields.ISO.weekOfWeekBasedYear())
    val parity = WeekParity.fromIsoWeek(week)
    val events = ScheduleResolver.eventsForDate(
        rules = rules,
        exceptions = exceptions,
        oneOffEvents = oneOffEvents,
        date = selectedDate,
    )
    val deadlines = tasks.filter { !it.completed && it.dueDate == selectedDate }
    val courseTitles = courses.associate { it.id to it.title }
    val monthFormatter = DateTimeFormatter.ofPattern("d MMMM", Locale("uk"))

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            ScreenHeader(
                title = "Календар",
                subtitle = "${weekStart.format(monthFormatter)} – ${weekStart.plusDays(6).format(monthFormatter)}",
            )

            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = {
                        weekStart = weekStart.minusWeeks(1)
                        selectedDate = weekStart
                    }
                ) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Попередній тиждень")
                }
                StatusPill(
                    text = "$week · ${if (parity == WeekParity.EVEN) "Парний" else "Непарний"}",
                )
                IconButton(
                    onClick = {
                        weekStart = weekStart.plusWeeks(1)
                        selectedDate = weekStart
                    }
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Наступний тиждень")
                }
            }

            WeekDaySelector(
                dates = dates,
                selectedDate = selectedDate,
                onSelect = { selectedDate = it },
            )

            Text(
                text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("uk")))
                    .replaceFirstChar { it.uppercase() },
                modifier = Modifier.padding(top = 20.dp, bottom = 10.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 80.dp),
            ) {
                if (deadlines.isNotEmpty()) {
                    item {
                        SectionCard {
                            Text("Дедлайни", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            deadlines.forEachIndexed { index, task ->
                                if (index > 0) HorizontalDivider()
                                CalendarDeadlineRow(
                                    task = task,
                                    courseTitle = courseTitles[task.courseId] ?: task.courseId,
                                )
                            }
                        }
                    }
                }

                if (events.isEmpty()) {
                    item {
                        SectionCard {
                            Text("На цей день занять немає")
                            Text(
                                "Натисни +, щоб додати пару.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    items(events, key = { "${it.id}-${it.date}-${it.status}" }) { event ->
                        ScheduleEventCard(
                            event = event,
                            onClick = { selectedEvent = event },
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddSheet = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = "Додати пару")
        }
    }

    if (showAddSheet || editingRule != null) {
        AddLessonSheet(
            initialDay = selectedDate.dayOfWeek,
            initialRule = editingRule,
            onDismiss = {
                showAddSheet = false
                editingRule = null
            },
            onSave = { rule ->
                scope.launch {
                    scheduleRepository.upsertRule(rule)
                    showAddSheet = false
                    editingRule = null
                }
            },
        )
    }

    selectedEvent?.let { event ->
        val recurringRule = rules.firstOrNull { it.id == event.id }
        LessonActionsSheet(
            event = event,
            isRecurringRule = recurringRule != null,
            onDismiss = { selectedEvent = null },
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
                    val exceptionId = event.exceptionId
                    if (exceptionId != null) {
                        scheduleRepository.deleteException(exceptionId)
                    }
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
