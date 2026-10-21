package com.kpyruy.takt.feature.home

import com.kpyruy.takt.core.model.uiMessage
import com.kpyruy.takt.core.model.UiKey
import com.kpyruy.takt.core.ui.components.tourTarget
import com.kpyruy.takt.core.ui.i18n.t
import com.kpyruy.takt.core.ui.i18n.TaktI18n
import com.kpyruy.takt.core.ui.i18n.formatDurationMinutes

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.background
import androidx.compose.material.icons.outlined.PersonOff
import com.kpyruy.takt.core.ui.components.lessonInteraction
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import com.kpyruy.takt.core.ui.components.CourseInlineIcon
import com.kpyruy.takt.core.ui.components.EventAvatar
import com.kpyruy.takt.core.ui.components.LessonTestBadge
import com.kpyruy.takt.core.model.LessonType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.outlined.ChevronRight
import com.kpyruy.takt.core.ui.components.GlassSurface
import com.kpyruy.takt.core.ui.components.glassSurface
import com.kpyruy.takt.core.ui.components.CourseAvatar
import com.kpyruy.takt.core.ui.components.StatusPill
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.kpyruy.takt.core.ui.components.TaktIconButton
import com.kpyruy.takt.core.ui.components.SectionCard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.kpyruy.takt.core.data.*
import com.kpyruy.takt.core.model.*
import com.kpyruy.takt.core.ui.theme.taktSubjectColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

@Composable
fun HomeScreen(
    planningSnapshot: kotlinx.coroutines.flow.StateFlow<PlanningSnapshot?>,
    studyContentRepository: StudyContentRepository,
    settingsRepository: AppSettingsRepository,
    onOpenSettings: () -> Unit,
    onAddCourse: () -> Unit,
    allowCourseCreation: Boolean = true,
    onOpenCourse: (String) -> Unit,
    onOpenAssessment: (GradeItem) -> Unit,
    onEventLongClick: (ResolvedScheduleEvent) -> Unit,
) {
    val current by planningSnapshot.collectAsStateWithLifecycle()
    val planning = current ?: return
    val courses = planning.courses
    val absences = planning.absences
    val rules = planning.rules
    val oneOffEvents = planning.oneOffEvents
    val exceptions = planning.exceptions
    val tasks = planning.tasks
    val assessments = planning.assessments
    val settings = planning.settings
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val clock by produceState(initialValue = LocalDateTime.now(), lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) { value = LocalDateTime.now(); delay(30_000L) }
        }
    }
    val today = clock.toLocalDate()
    var selectedDate by remember(today) { mutableStateOf(today) }
    val now = clock.toLocalTime()
    val timeWidth = 43.dp * maxOf(1f, LocalDensity.current.fontScale * .9f)
    val currentSemester = settings.effectiveCurrentSemester(courses)
    val courseSemesters = remember(courses, currentSemester) {
        courses.associate { it.id to settings.academicSemester(it, currentSemester) }
    }
    val courseTitles = remember(courses) { courses.associate { it.id to it.title } }
    val events = remember(rules, exceptions, oneOffEvents, selectedDate, settings, absences, courseSemesters, courseTitles) {
        settings.filterScheduleEvents(ScheduleResolver.eventsForDate(
            rules, exceptions, oneOffEvents, selectedDate, settings.effectiveParity(selectedDate), absences,
            ruleAllowed = { rule, occurrenceDate ->
                settings.allowsRecurringLesson(rule, courseSemesters, occurrenceDate)
            },
        )).map { event ->
            if (event.id.startsWith("uis:")) event.copy(title = courseTitles[event.courseId] ?: event.title)
            else event
        }.sortedBy { it.startTime }
    }
    val next = if (selectedDate == today) ScheduleTimeline.nextEvent(
        events.filter { it.status != ScheduleEventStatus.CANCELLED && !it.isAbsent }, now,
    ) else null
    val deadlines = remember(tasks, selectedDate) {
        tasks.filter { it.dueDate == selectedDate && (!it.completed || (it.requiredForExam && !it.meetsAdmissionRequirement)) }
    }
    val assessmentDeadlines = remember(assessments, selectedDate) { assessments.filter { it.dueDate == selectedDate && !it.isAwaitingResult } }
    val locale = TaktI18n.locale
    val time = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val weekday = remember(locale) { DateTimeFormatter.ofPattern("EEEE", locale) }
    val month = remember(locale) { DateTimeFormatter.ofPattern("LLLL", locale) }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(t(UiKey.TEXT_TODAY), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text(selectedDate.format(weekday).replaceFirstChar { it.uppercase() } + ", " +
                    selectedDate.format(DateTimeFormatter.ofPattern("d MMMM", locale)),
                    style = MaterialTheme.typography.bodyLarge, color = muted)
            }
            TaktIconButton(Icons.Outlined.Settings, t(UiKey.TEXT_SETTINGS), onClick = onOpenSettings)
        }
        Spacer(Modifier.height(16.dp))
        key(today) { HomeWeekStrip(selected = selectedDate, onSelect = { selectedDate = it }) }
        Spacer(Modifier.height(22.dp))
        if (courses.isEmpty()) {
            SectionCard {
                Text(t(UiKey.TEXT_CREATE_YOUR_SCHEDULE), style = MaterialTheme.typography.titleMedium)
                Text(t(if (allowCourseCreation) UiKey.TEXT_ADD_A_SUBJECT_FIRST_THEN_A_CLASS else UiKey.TEXT_SUBJECTS_ARE_LOADED_FROM_UIS), color = muted)
                if (allowCourseCreation) Button(onClick = onAddCourse, modifier = Modifier.fillMaxWidth()) { Text(t(UiKey.TEXT_ADD_COURSE)) }
            }
            Spacer(Modifier.height(16.dp))
        }
        Text(t(UiKey.TEXT_YOUR_DAY), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        if (events.isEmpty()) Text(t(UiKey.TEXT_NO_CLASSES_ON_THIS_DAY), style = MaterialTheme.typography.bodyMedium, color = muted)
        Column(Modifier.tourTarget("home-lessons").drawBehind {
            if (events.isNotEmpty()) drawLine(accent.copy(alpha = .24f),
                Offset(timeWidth.toPx() + 4.dp.toPx(), 20.dp.toPx()),
                Offset(timeWidth.toPx() + 4.dp.toPx(), size.height - 20.dp.toPx()), strokeWidth = 1.dp.toPx())
        }) {
            events.forEachIndexed { index, event ->
                val visual = HomeLessonVisual.forEvent(event, selectedDate, today, now, next?.id)
                val active = visual.active
                val cancelled = event.status == ScheduleEventStatus.CANCELLED
                val subjectColor = taktSubjectColor(event.courseId ?: event.title)
                val course = courses.firstOrNull { it.id == event.courseId }
                val status = when (event.status) {
                    ScheduleEventStatus.CANCELLED -> t(UiKey.TEXT_CANCELLED)
                    ScheduleEventStatus.MOVED -> t(UiKey.TEXT_RESCHEDULED)
                    ScheduleEventStatus.ONE_OFF -> t(UiKey.TEXT_ONE_TIME_EVENT)
                    ScheduleEventStatus.NORMAL -> null
                }
                Row(Modifier.fillMaxWidth().drawBehind {
                    drawCircle(if (active) accent else subjectColor, 3.5.dp.toPx(),
                        Offset(timeWidth.toPx() + 4.dp.toPx(), size.height / 2f))
                }, horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(event.startTime.format(time), Modifier.width(timeWidth), style = MaterialTheme.typography.bodySmall,
                        color = if (active) accent else muted)
                    Row(Modifier.weight(1f).testTag("home-lesson-${event.id}")
                        .glassSurface(RoundedCornerShape(18.dp), if (active) accent.copy(alpha = .12f) else Color.Unspecified)
                        .lessonInteraction({ event.courseId?.let(onOpenCourse) ?: onEventLongClick(event) }, { onEventLongClick(event) })
                        .padding(8.dp).alpha(if (visual.muted) .85f else 1f),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (course != null) CourseAvatar(course, Modifier.size(40.dp))
                        else EventAvatar(event.lessonType, event.courseId ?: event.title,
                            Modifier.testTag("home-event-avatar-${event.id}").size(40.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(event.title, style = MaterialTheme.typography.titleMedium,
                                textDecoration = if (cancelled && settings.cancellationStyle == CancellationDisplayStyle.STRIKETHROUGH) TextDecoration.LineThrough else null)
                            Text(listOfNotNull(event.lessonType.takeUnless { it == LessonType.UNSPECIFIED }?.label?.let(::t), event.room,
                                status, t(uiMessage(UiKey.FORMAT_BY_VALUE, event.endTime.format(time)))).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall, color = muted)
                            if (event.isAbsent) Text(t(UiKey.TEXT_ABSENT), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                        }
                        if (CourseWork.hasTestOnLesson(event, events, assessmentDeadlines)) LessonTestBadge(compact = true)
                        if (active) {
                            val minutes = Duration.between(now, event.startTime).toMinutes()
                            Text(if (minutes > 0) t(uiMessage(UiKey.FORMAT_VALUE_MIN, minutes)) else t(UiKey.TEXT_NOW_FE18013D),
                                Modifier.testTag("home-active-${event.id}").glassSurface(RoundedCornerShape(50), accent.copy(alpha = .18f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall, color = accent)
                        } else Icon(Icons.Outlined.ChevronRight, null, Modifier.size(18.dp), tint = muted)
                    }
                }
                val following = events.getOrNull(index + 1)
                val minutes = following?.let { Duration.between(event.endTime, it.startTime).toMinutes() } ?: 0
                if (minutes > 0) Text(t(UiKey.TEXT_BREAK) + " · " + formatDurationMinutes(minutes),
                    Modifier.padding(start = timeWidth + 12.dp, top = 4.dp, bottom = 4.dp),
                    style = MaterialTheme.typography.bodySmall, color = muted)
                else Spacer(Modifier.height(9.dp))
            }
        }
        Spacer(Modifier.height(18.dp))
        HomeTasksSection(tasks, assessments, courses, today,
            savedFilter = settings.homeWorkFilter,
            onFilterChange = { filter -> scope.launch { settingsRepository.setHomeWorkFilter(filter) } },
            onTaskCompleted = { task, completed -> scope.launch { studyContentRepository.setTaskCompleted(task.id, completed) } },
            onOpenAssessment = onOpenAssessment)
        Spacer(Modifier.height(76.dp))
    }
}

@Composable
private fun HomeWeekStrip(selected: LocalDate, onSelect: (LocalDate) -> Unit) {
    val weekday = remember(TaktI18n.language) { DateTimeFormatter.ofPattern("EE", TaktI18n.locale) }
    val anchorWeek = remember { selected.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }
    val centerPage = Int.MAX_VALUE / 2
    val pagerState = rememberPagerState(initialPage = centerPage, pageCount = { Int.MAX_VALUE })
    var selectedWeekday by remember { mutableIntStateOf(selected.dayOfWeek.value) }

    LaunchedEffect(selected.dayOfWeek) { selectedWeekday = selected.dayOfWeek.value }
    LaunchedEffect(pagerState.settledPage, selectedWeekday) {
        val week = anchorWeek.plusWeeks((pagerState.settledPage - centerPage).toLong())
        val date = week.plusDays((selectedWeekday - 1).toLong())
        if (date != selected) onSelect(date)
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxWidth().testTag("home-week-strip").tourTarget("home-week"),
    ) { page ->
        val week = anchorWeek.plusWeeks((page - centerPage).toLong())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(7) { day ->
                val date = week.plusDays(day.toLong())
                val active = date == selected
                Column(Modifier.weight(1f).testTag("home-day-${date.toEpochDay()}")
                    .glassSurface(RoundedCornerShape(14.dp), if (active) MaterialTheme.colorScheme.primaryContainer else Color.Unspecified)
                    .selectable(active, role = Role.Tab) { onSelect(date) }.padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(date.format(weekday).uppercase(), fontSize = 10.sp, lineHeight = 13.sp,
                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text(date.dayOfMonth.toString(), modifier = if (active) Modifier.testTag("home-date-number") else Modifier, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold,
                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}
