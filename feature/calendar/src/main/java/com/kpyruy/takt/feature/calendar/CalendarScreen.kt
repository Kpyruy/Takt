package com.kpyruy.takt.feature.calendar

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.background
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.produceState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.data.AppSettingsRepository
import com.kpyruy.takt.core.data.GradeRepository
import com.kpyruy.takt.core.data.ScheduleRepository
import com.kpyruy.takt.core.data.StudyContentRepository
import com.kpyruy.takt.core.data.StudyPlanRepository
import com.kpyruy.takt.core.model.AppSettings
import com.kpyruy.takt.core.model.GradeItem
import com.kpyruy.takt.core.model.CourseWork
import com.kpyruy.takt.core.model.CalendarMonthGrid
import com.kpyruy.takt.core.model.CalendarDayMarkers
import com.kpyruy.takt.core.model.OneOffScheduleEvent
import com.kpyruy.takt.core.model.ParityOverride
import com.kpyruy.takt.core.model.ResolvedScheduleEvent
import com.kpyruy.takt.core.model.ScheduleException
import com.kpyruy.takt.core.model.ScheduleExceptionType
import com.kpyruy.takt.core.model.ScheduleResolver
import com.kpyruy.takt.core.model.ScheduleRule
import com.kpyruy.takt.core.model.WeekLayout
import com.kpyruy.takt.core.model.WeekParity
import com.kpyruy.takt.core.ui.components.ScreenHeader
import com.kpyruy.takt.core.ui.components.SectionCard
import com.kpyruy.takt.core.ui.components.StatusPill
import com.kpyruy.takt.core.ui.components.TaktSegmentedTabs
import com.kpyruy.takt.core.ui.motion.TaktMotion
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import com.kpyruy.takt.core.ui.components.TaktIconButton
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
    gradeRepository: GradeRepository,
    studyPlanRepository: StudyPlanRepository,
    settingsRepository: AppSettingsRepository,
    onOpenCourse: (String) -> Unit,
    onOpenAssessment: (GradeItem) -> Unit,
    onEventLongClick: (ResolvedScheduleEvent) -> Unit,
) {
    val absences by remember(scheduleRepository) { scheduleRepository.observeAbsences() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val rules by remember(scheduleRepository) { scheduleRepository.observeRules() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val oneOffEvents by remember(scheduleRepository) { scheduleRepository.observeOneOffEvents() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val exceptions by remember(scheduleRepository) { scheduleRepository.observeExceptions() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val tasks by remember(studyContentRepository) { studyContentRepository.observeAllTasks() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val assessments by remember(gradeRepository) { gradeRepository.observeAllItems() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val courses by remember(studyPlanRepository) { studyPlanRepository.observeCourses() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val settings by settingsRepository.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val today by produceState(LocalDate.now(), lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) { value = LocalDate.now(); delay(30_000L) }
        }
    }

    var selectedDate by rememberSaveable(stateSaver = Saver<LocalDate, Long>({ it.toEpochDay() }, { LocalDate.ofEpochDay(it) })) { mutableStateOf(today) }
    var weekStart by rememberSaveable(stateSaver = Saver<LocalDate, Long>({ it.toEpochDay() }, { LocalDate.ofEpochDay(it) })) {
        mutableStateOf(today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))
    }
    var visibleMonth by rememberSaveable(stateSaver = Saver<YearMonth, String>({ it.toString() }, { YearMonth.parse(it) })) { mutableStateOf(YearMonth.from(today)) }
    var viewMode by rememberSaveable { mutableStateOf(CalendarViewMode.DAY) }

    val dates = remember(weekStart) { (0L..6L).map(weekStart::plusDays) }
    val week = selectedDate.get(WeekFields.ISO.weekOfWeekBasedYear())
    val parity = settings.effectiveParity(selectedDate)
    val courseTitles = remember(courses) { courses.associate { it.id to it.title } }
    val courseSemesters = remember(courses) { courses.associate { it.id to it.semester } }

    val shortDateFormatter = remember { DateTimeFormatter.ofPattern("d MMM", Locale("uk")) }
    val monthTitleFormatter = remember { DateTimeFormatter.ofPattern("LLLL yyyy", Locale("uk")) }
    val selectedDateFormatter = remember { DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("uk")) }

    val monthDays = remember(visibleMonth) { CalendarMonthGrid.days(visibleMonth) }
    val visibleDates = remember(
        viewMode,
        selectedDate.takeIf { viewMode == CalendarViewMode.DAY },
        dates.takeIf { viewMode == CalendarViewMode.WEEK },
        monthDays.takeIf { viewMode == CalendarViewMode.MONTH },
    ) {
        when (viewMode) {
            CalendarViewMode.DAY -> listOf(selectedDate)
            CalendarViewMode.WEEK -> dates
            CalendarViewMode.MONTH -> monthDays
        }
    }
    fun resolveEvents(date: LocalDate): List<ResolvedScheduleEvent> =
        settings.filterScheduleEvents(ScheduleResolver.eventsForDate(
            rules = rules,
            exceptions = exceptions,
            oneOffEvents = oneOffEvents,
            date = date,
            parityOverride = settings.effectiveParity(date),
            absences = absences,
            ruleAllowed = { rule, occurrenceDate ->
                settings.allowsRecurringLesson(rule, courseSemesters, occurrenceDate)
            },
        ))
    val eventsByDate = remember(visibleDates, rules, exceptions, oneOffEvents, absences, settings, courseSemesters) {
        visibleDates.associateWith(::resolveEvents)
    }
    val pendingTasksByDate = remember(tasks) {
        tasks.filter { !it.completed || (it.requiredForExam && !it.meetsAdmissionRequirement) }
            .filter { it.dueDate != null }.groupBy { it.dueDate }
    }
    val allTasksByDate = remember(tasks) { tasks.filter { it.dueDate != null }.groupBy { it.dueDate } }
    val assessmentsByDate = remember(assessments) {
        assessments.filter { it.dueDate != null }.groupBy { it.dueDate }
    }
    fun eventsForDate(date: LocalDate): List<ResolvedScheduleEvent> = eventsByDate[date] ?: resolveEvents(date)
    fun hasTest(event: ResolvedScheduleEvent): Boolean =
        CourseWork.hasTestOnLesson(event, eventsForDate(event.date), assessmentsByDate[event.date].orEmpty())

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
                val newMonth = visibleMonth.minusMonths(1)
                visibleMonth = newMonth
                selectDate(newMonth.atDay(1))
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
                val newMonth = visibleMonth.plusMonths(1)
                visibleMonth = newMonth
                selectDate(newMonth.atDay(1))
            }
        }
    }

    val headerSubtitle = when (viewMode) {
        CalendarViewMode.DAY -> selectedDate.format(monthTitleFormatter).replaceFirstChar { it.uppercase() }
        CalendarViewMode.WEEK -> "${weekStart.format(shortDateFormatter)} – ${weekStart.plusDays(6).format(shortDateFormatter)}"
        CalendarViewMode.MONTH -> visibleMonth.atDay(1).format(monthTitleFormatter).replaceFirstChar { it.uppercase() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp).padding(top = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ScreenHeader(
            title = "Календар",
            subtitle = headerSubtitle,
        )

        TaktSegmentedTabs(
            labels = listOf("День", "Тиждень", "Місяць"),
            selectedIndex = viewMode.ordinal,
            onSelected = { viewMode = CalendarViewMode.entries[it] },
            modifier = Modifier.fillMaxWidth(),
        )

        if (viewMode != CalendarViewMode.MONTH) {
            WeekDaySelector(
                dates = dates,
                selectedDate = selectedDate,
                onWeekChange = ::selectDate,
                onSelect = {
                    selectDate(it)
                    if (viewMode == CalendarViewMode.WEEK) viewMode = CalendarViewMode.DAY
                },
            )
        }

        if (viewMode == CalendarViewMode.WEEK) {
            TaktSegmentedTabs(
                labels = listOf("Таймтейбл", "Список"),
                selectedIndex = if (settings.weekLayout == WeekLayout.TIMETABLE) 0 else 1,
                onSelected = { index ->
                    scope.launch {
                        settingsRepository.setWeekLayout(
                            if (index == 0) WeekLayout.TIMETABLE else WeekLayout.COMPACT_LIST
                        )
                    }
                },
            )
        }

        if (viewMode == CalendarViewMode.MONTH || selectedDate != today) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = ::navigatePrevious) { Icon(Icons.Default.ChevronLeft, "Попередній період") }
                TextButton(onClick = { selectDate(today) }) { Text("Сьогодні") }
                IconButton(onClick = ::navigateNext) { Icon(Icons.Default.ChevronRight, "Наступний період") }
            }
        }

        AnimatedContent(
            targetState = viewMode,
            modifier = Modifier.weight(1f),
            transitionSpec = {
                fadeIn(animationSpec = TaktMotion.fast()) togetherWith
                    fadeOut(animationSpec = TaktMotion.fast())
            },
            label = "calendar-view",
        ) { mode ->
            when (mode) {
                CalendarViewMode.DAY -> {
                    SelectedDayAgenda(
                        date = selectedDate,
                        events = eventsForDate(selectedDate),
                        deadlines = pendingTasksByDate[selectedDate].orEmpty(),
                        assessments = assessmentsByDate[selectedDate].orEmpty(),
                        courseTitles = courseTitles,
                        today = today,
                        settings = settings,
                        onEventClick = { event -> event.courseId?.let(onOpenCourse) ?: onEventLongClick(event) },
                        onEventLongClick = onEventLongClick,
                        onOpenAssessment = onOpenAssessment,
                        hasTest = ::hasTest,
                        onDeadlineCompleted = { task, completed ->
                            scope.launch {
                                studyContentRepository.setTaskCompleted(task.id, completed)
                            }
                        },
                    )
                }

                CalendarViewMode.WEEK -> {
                    when (settings.weekLayout) {
                        WeekLayout.TIMETABLE -> WeekTimetable(
                            dates = dates,
                            eventsForDate = ::eventsForDate,
                            assessmentCountForDate = { date -> pendingTasksByDate[date].orEmpty().size +
                                assessmentsByDate[date].orEmpty().size },
                            hasTest = ::hasTest,
                            onEventClick = { event -> event.courseId?.let(onOpenCourse) ?: onEventLongClick(event) },
                            onEventLongClick = onEventLongClick,
                        )
                        WeekLayout.COMPACT_LIST -> WeekCompactList(
                            dates = dates,
                            eventsForDate = ::eventsForDate,
                            workCountForDate = { date -> pendingTasksByDate[date].orEmpty().size +
                                assessmentsByDate[date].orEmpty().size },
                            hasTest = ::hasTest,
                            onSelectDate = {
                                selectDate(it)
                                viewMode = CalendarViewMode.DAY
                            },
                            onEventClick = { event -> event.courseId?.let(onOpenCourse) ?: onEventLongClick(event) },
                            onEventLongClick = onEventLongClick,
                        )
                    }
                }

                CalendarViewMode.MONTH -> {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        MonthCalendar(
                            month = visibleMonth,
                            days = monthDays,
                            selectedDate = selectedDate,
                            today = today,
                            markersForDate = { date -> CalendarDayMarkers.from(
                                eventsForDate(date), allTasksByDate[date].orEmpty(),
                                assessmentsByDate[date].orEmpty(),
                            ) },
                            onSelect = ::selectDate,
                        )

                        Text(
                            selectedDate.format(selectedDateFormatter).replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleMedium,
                        )

                        val deadlines = pendingTasksByDate[selectedDate].orEmpty()
                        val datedAssessments = assessmentsByDate[selectedDate].orEmpty()
                        if (deadlines.isNotEmpty() || datedAssessments.isNotEmpty()) {
                            SectionCard {
                                Text("Дедлайни", style = MaterialTheme.typography.titleMedium)
                                deadlines.forEachIndexed { index, task ->
                                    if (index > 0) HorizontalDivider()
                                    CalendarDeadlineRow(
                                        task = task,
                                        courseTitle = courseTitles[task.courseId] ?: task.courseId,
                                        onCompletedChange = { completed ->
                                            scope.launch {
                                                studyContentRepository.setTaskCompleted(task.id, completed)
                                            }
                                        },
                                    )
                                }
                                datedAssessments.forEach { item ->
                                    HorizontalDivider()
                                    CalendarAssessmentStrip(
                                        item = item,
                                        courseTitle = courseTitles[item.courseId] ?: item.courseId,
                                        onClick = { onOpenAssessment(item) },
                                    )
                                }
                            }
                        }

                        DayTimelineView(
                            events = eventsForDate(selectedDate),
                            selectedDate = selectedDate,
                            today = today,
                            cancellationStyle = settings.cancellationStyle,
                            hasTest = ::hasTest,
                            onEventClick = { event -> event.courseId?.let(onOpenCourse) ?: onEventLongClick(event) },
                            onEventLongClick = onEventLongClick,
                        )
                        Spacer(Modifier.height(80.dp))
                    }
                }
            }
        }
    }

}

@Composable
private fun SelectedDayAgenda(
    date: LocalDate,
    events: List<ResolvedScheduleEvent>,
    deadlines: List<com.kpyruy.takt.core.model.StudyTask>,
    assessments: List<GradeItem>,
    courseTitles: Map<String, String>,
    today: LocalDate,
    settings: AppSettings,
    onEventClick: (ResolvedScheduleEvent) -> Unit,
    onEventLongClick: (ResolvedScheduleEvent) -> Unit,
    onOpenAssessment: (GradeItem) -> Unit,
    hasTest: (ResolvedScheduleEvent) -> Boolean,
    onDeadlineCompleted: (com.kpyruy.takt.core.model.StudyTask, Boolean) -> Unit,
) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (deadlines.isNotEmpty()) {
            deadlines.forEach { task ->
                CalendarDeadlineStrip(task = task, onCompletedChange = { onDeadlineCompleted(task, it) })
            }
        }
        assessments.forEach { item ->
            CalendarAssessmentStrip(
                item = item,
                courseTitle = courseTitles[item.courseId] ?: item.courseId,
                onClick = { onOpenAssessment(item) },
            )
        }

        if (events.isEmpty()) {
            SectionCard {
                Text("На цей день занять немає")
                Text(
                    "Додай пару або разову подію кнопкою + внизу праворуч.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            DayTimelineView(
                events = events,
                selectedDate = date,
                today = today,
                cancellationStyle = settings.cancellationStyle,
                hasTest = hasTest,
                onEventClick = onEventClick,
                onEventLongClick = onEventLongClick,
            )
        }
        Spacer(Modifier.height(80.dp))
    }
}
