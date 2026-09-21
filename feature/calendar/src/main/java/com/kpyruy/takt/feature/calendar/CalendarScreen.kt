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
import androidx.compose.material3.FilterChip
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
import com.kpyruy.takt.core.data.AppSettingsRepository
import com.kpyruy.takt.core.data.ScheduleRepository
import com.kpyruy.takt.core.data.StudyContentRepository
import com.kpyruy.takt.core.data.StudyPlanRepository
import com.kpyruy.takt.core.model.AppSettings
import com.kpyruy.takt.core.model.CalendarMonthGrid
import com.kpyruy.takt.core.model.OneOffScheduleEvent
import com.kpyruy.takt.core.model.ParityOverride
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
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.launch

private enum class CalendarViewMode {
    DAY,
    WEEK,
    MONTH,
}

@Composable
fun CalendarScreen(
    scheduleRepository: ScheduleRepository,
    studyContentRepository: StudyContentRepository,
    studyPlanRepository: StudyPlanRepository,
    settingsRepository: AppSettingsRepository,
) {
    val rules by scheduleRepository.observeRules().collectAsState(initial = emptyList())
    val oneOffEvents by scheduleRepository.observeOneOffEvents().collectAsState(initial = emptyList())
    val exceptions by scheduleRepository.observeExceptions().collectAsState(initial = emptyList())
    val tasks by studyContentRepository.observeAllTasks().collectAsState(initial = emptyList())
    val courses by studyPlanRepository.observeCourses().collectAsState(initial = emptyList())
    val settings by settingsRepository.settings.collectAsState(initial = AppSettings())
    val scope = rememberCoroutineScope()
    val today = LocalDate.now()

    var selectedDate by remember { mutableStateOf(today) }
    var weekStart by remember {
        mutableStateOf(today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))
    }
    var visibleMonth by remember { mutableStateOf(YearMonth.from(today)) }
    var viewMode by remember { mutableStateOf(CalendarViewMode.WEEK) }
    var showAddChoice by remember { mutableStateOf(false) }
    var showAddRecurring by remember { mutableStateOf(false) }
    var showAddOneOff by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<ScheduleRule?>(null) }
    var editingOneOff by remember { mutableStateOf<OneOffScheduleEvent?>(null) }
    var selectedEvent by remember { mutableStateOf<ResolvedScheduleEvent?>(null) }
    var movingEvent by remember { mutableStateOf<ResolvedScheduleEvent?>(null) }

    val dates = (0L..6L).map { weekStart.plusDays(it) }
    val week = selectedDate.get(WeekFields.ISO.weekOfWeekBasedYear())
    val parity = settings.effectiveParity(selectedDate)
    val resolvedEvents = ScheduleResolver.eventsForDate(
        rules = rules,
        exceptions = exceptions,
        oneOffEvents = oneOffEvents,
        date = selectedDate,
        parityOverride = parity,
    )
    val events = settings.filterScheduleEvents(resolvedEvents)
    val deadlines = tasks.filter { !it.completed && it.dueDate == selectedDate }
    val courseTitles = courses.associate { it.id to it.title }

    val shortDateFormatter = DateTimeFormatter.ofPattern("d MMMM", Locale("uk"))
    val monthTitleFormatter = DateTimeFormatter.ofPattern("LLLL yyyy", Locale("uk"))
    val selectedDateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("uk"))

    fun selectDate(date: LocalDate) {
        selectedDate = date
        weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        visibleMonth = YearMonth.from(date)
    }

    fun navigatePrevious() {
        when (viewMode) {
            CalendarViewMode.DAY -> selectDate(selectedDate.minusDays(1))
            CalendarViewMode.WEEK -> {
                val newStart = weekStart.minusWeeks(1)
                weekStart = newStart
                selectedDate = newStart
                visibleMonth = YearMonth.from(newStart)
            }
            CalendarViewMode.MONTH -> {
                visibleMonth = visibleMonth.minusMonths(1)
                selectDate(visibleMonth.atDay(1))
            }
        }
    }

    fun navigateNext() {
        when (viewMode) {
            CalendarViewMode.DAY -> selectDate(selectedDate.plusDays(1))
            CalendarViewMode.WEEK -> {
                val newStart = weekStart.plusWeeks(1)
                weekStart = newStart
                selectedDate = newStart
                visibleMonth = YearMonth.from(newStart)
            }
            CalendarViewMode.MONTH -> {
                visibleMonth = visibleMonth.plusMonths(1)
                selectDate(visibleMonth.atDay(1))
            }
        }
    }

    fun hasCalendarContent(date: LocalDate): Boolean {
        if (tasks.any { !it.completed && it.dueDate == date }) return true
        val dateParity = settings.effectiveParity(date)
        val dateEvents = ScheduleResolver.eventsForDate(
            rules = rules,
            exceptions = exceptions,
            oneOffEvents = oneOffEvents,
            date = date,
            parityOverride = dateParity,
        )
        return settings.filterScheduleEvents(dateEvents).isNotEmpty()
    }

    val headerSubtitle = when (viewMode) {
        CalendarViewMode.DAY -> selectedDate.format(selectedDateFormatter).replaceFirstChar { it.uppercase() }
        CalendarViewMode.WEEK -> "${weekStart.format(shortDateFormatter)} – ${weekStart.plusDays(6).format(shortDateFormatter)}"
        CalendarViewMode.MONTH -> visibleMonth.atDay(1).format(monthTitleFormatter).replaceFirstChar { it.uppercase() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ScreenHeader(
                title = "Календар",
                subtitle = headerSubtitle,
            )

            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    CalendarViewMode.DAY to "День",
                    CalendarViewMode.WEEK to "Тиждень",
                    CalendarViewMode.MONTH to "Місяць",
                ).forEach { (mode, label) ->
                    FilterChip(
                        selected = viewMode == mode,
                        onClick = { viewMode = mode },
                        label = { Text(label) },
                    )
                }

                FilterChip(
                    selected = selectedDate == today,
                    onClick = { selectDate(today) },
                    label = { Text("Сьогодні") },
                )
            }

            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = ::navigatePrevious) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Назад")
                }
                StatusPill(
                    text = buildString {
                        append(week)
                        append(" · ")
                        append(if (parity == WeekParity.EVEN) "Парний" else "Непарний")
                        if (settings.parityOverride != ParityOverride.AUTO) append(" · вручну")
                    },
                )
                IconButton(onClick = ::navigateNext) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Вперед")
                }
            }

            when (viewMode) {
                CalendarViewMode.DAY -> Unit
                CalendarViewMode.WEEK -> {
                    WeekDaySelector(
                        dates = dates,
                        selectedDate = selectedDate,
                        onSelect = ::selectDate,
                    )
                }
                CalendarViewMode.MONTH -> {
                    MonthCalendar(
                        month = visibleMonth,
                        days = CalendarMonthGrid.days(visibleMonth),
                        selectedDate = selectedDate,
                        today = today,
                        hasContent = ::hasCalendarContent,
                        onSelect = ::selectDate,
                    )
                }
            }

            Text(
                text = selectedDate.format(selectedDateFormatter).replaceFirstChar { it.uppercase() },
                modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
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
                                "Натисни +, щоб додати пару або разову подію.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    items(events, key = { "${it.id}-${it.date}-${it.status}" }) { event ->
                        ScheduleEventCard(
                            event = event,
                            cancellationStyle = settings.cancellationStyle,
                            onClick = { selectedEvent = event },
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddChoice = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = "Додати в календар")
        }
    }

    if (showAddChoice) {
        AddCalendarItemSheet(
            onDismiss = { showAddChoice = false },
            onAddRecurring = {
                showAddChoice = false
                showAddRecurring = true
            },
            onAddOneOff = {
                showAddChoice = false
                showAddOneOff = true
            },
        )
    }

    if (showAddRecurring || editingRule != null) {
        AddLessonSheet(
            initialDay = selectedDate.dayOfWeek,
            initialRule = editingRule,
            onDismiss = {
                showAddRecurring = false
                editingRule = null
            },
            onSave = { rule ->
                scope.launch {
                    scheduleRepository.upsertRule(rule)
                    showAddRecurring = false
                    editingRule = null
                }
            },
        )
    }

    if (showAddOneOff || editingOneOff != null) {
        OneOffEventSheet(
            initialDate = selectedDate,
            initialEvent = editingOneOff,
            onDismiss = {
                showAddOneOff = false
                editingOneOff = null
            },
            onSave = { event ->
                scope.launch {
                    scheduleRepository.upsertOneOffEvent(event)
                    showAddOneOff = false
                    editingOneOff = null
                    selectDate(event.date)
                }
            },
        )
    }

    selectedEvent?.let { event ->
        val recurringRule = rules.firstOrNull { it.id == event.id }
        val oneOff = oneOffEvents.firstOrNull { it.id == event.id }

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
