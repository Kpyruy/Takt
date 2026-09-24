package com.kpyruy.takt.feature.home

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
import com.kpyruy.takt.core.ui.components.LessonTypeIcon
import com.kpyruy.takt.core.ui.components.LessonTestBadge
import com.kpyruy.takt.core.model.LessonType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.kpyruy.takt.core.ui.components.TaktIconButton
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
    repository: StudyPlanRepository,
    scheduleRepository: ScheduleRepository,
    gradeRepository: GradeRepository,
    studyContentRepository: StudyContentRepository,
    settingsRepository: AppSettingsRepository,
    onOpenSettings: () -> Unit,
    onOpenCourse: (String) -> Unit,
    onOpenAssessment: (GradeItem) -> Unit,
    onEventLongClick: (ResolvedScheduleEvent) -> Unit,
) {
    val courses by remember(repository) { repository.observeCourses() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val absences by remember(scheduleRepository) { scheduleRepository.observeAbsences() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val rules by remember(scheduleRepository) { scheduleRepository.observeRules() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val oneOffEvents by remember(scheduleRepository) { scheduleRepository.observeOneOffEvents() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val exceptions by remember(scheduleRepository) { scheduleRepository.observeExceptions() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val tasks by remember(studyContentRepository) { studyContentRepository.observeAllTasks() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val assessments by remember(gradeRepository) { gradeRepository.observeAllItems() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val settings by settingsRepository.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
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
    val events = remember(rules, exceptions, oneOffEvents, selectedDate, settings, absences) {
        settings.filterScheduleEvents(ScheduleResolver.eventsForDate(
            rules, exceptions, oneOffEvents, selectedDate, settings.effectiveParity(selectedDate), absences,
        )).sortedBy { it.startTime }
    }
    val next = if (selectedDate == today) ScheduleTimeline.nextEvent(
        events.filter { it.status != ScheduleEventStatus.CANCELLED }, now,
    ) else null
    val deadlines = remember(tasks, selectedDate) {
        tasks.filter { it.dueDate == selectedDate && (!it.completed || (it.requiredForExam && !it.meetsAdmissionRequirement)) }
    }
    val assessmentDeadlines = remember(assessments, selectedDate) { assessments.filter { it.dueDate == selectedDate } }
    val untimed = remember(tasks) {
        tasks.filter { it.dueDate == null && (!it.completed || (it.requiredForExam && !it.meetsAdmissionRequirement)) }
    }
    val courseTitles = remember(courses) { courses.associate { it.id to it.title } }
    val uk = remember { Locale("uk") }
    val time = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val weekday = remember(uk) { DateTimeFormatter.ofPattern("EEEE", uk) }
    val month = remember(uk) { DateTimeFormatter.ofPattern("LLLL", uk) }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    val amber = if (MaterialTheme.colorScheme.surface.luminance() > .5f) Color(0xFF986126) else Color(0xFFEFC78F)

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(selectedDate.dayOfMonth.toString(), Modifier.testTag("home-date-number"), fontSize = 40.sp, lineHeight = 52.sp, fontWeight = FontWeight.SemiBold,
                letterSpacing = (-2).sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(selectedDate.format(weekday).replaceFirstChar { it.uppercase() },
                    fontSize = 21.sp, lineHeight = 27.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.65).sp)
                Text(selectedDate.format(month).replaceFirstChar { it.uppercase() } +
                    " · ${selectedDate.get(WeekFields.ISO.weekOfWeekBasedYear())} тиждень",
                    style = MaterialTheme.typography.bodySmall, color = muted)
            }
            TaktIconButton(Icons.Outlined.Settings, "Налаштування", onClick = onOpenSettings)
        }
        Spacer(Modifier.height(24.dp))
        key(today) {
            HomeWeekStrip(selected = selectedDate, onSelect = { selectedDate = it })
        }
        Spacer(Modifier.height(22.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Твій день", style = MaterialTheme.typography.labelLarge)
            Text("Пар: ${events.size} · Дедлайнів: ${deadlines.size + assessmentDeadlines.count { !it.completed }}", style = MaterialTheme.typography.bodySmall, color = muted)
        }
        Spacer(Modifier.height(23.dp))
        if (events.isEmpty() && deadlines.isEmpty() && assessmentDeadlines.isEmpty()) {
            Text("На цей день подій немає", style = MaterialTheme.typography.bodyMedium, color = muted)
            Spacer(Modifier.height(20.dp))
        }
        events.forEachIndexed { index, event ->
            val active = event.id == next?.id
            val cancelled = event.status == ScheduleEventStatus.CANCELLED
            val finished = selectedDate < today || (selectedDate == today && event.endTime < now)
            val subjectColor = taktSubjectColor(event.courseId ?: event.title)
            val contentAlpha = if ((finished || cancelled) && !event.isAbsent) 0.5f else 1f
            val status = when(event.status) {
                ScheduleEventStatus.CANCELLED -> "Скасовано"
                ScheduleEventStatus.MOVED -> "Перенесено"
                ScheduleEventStatus.ONE_OFF -> "Разова подія"
                ScheduleEventStatus.NORMAL -> null
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(event.startTime.format(time), Modifier.width(timeWidth).alpha(contentAlpha),
                    style = MaterialTheme.typography.bodySmall, color = if (active) accent else muted)
                Row(Modifier.weight(1f).height(IntrinsicSize.Min)
                    .clip(RoundedCornerShape(topEnd = 13.dp, bottomEnd = 13.dp))
                    .background(if (active) accent.copy(alpha = 0.09f) else Color.Transparent)
                    .lessonInteraction({ event.courseId?.let(onOpenCourse) ?: onEventLongClick(event) }, { onEventLongClick(event) })) {
                    Box(Modifier.width(2.dp).fillMaxHeight().background(subjectColor))
                    Column(Modifier.weight(1f).padding(13.dp).alpha(contentAlpha)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CourseInlineIcon(event.courseId)
                            Text(event.title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
                                textDecoration = if (cancelled && settings.cancellationStyle == CancellationDisplayStyle.STRIKETHROUGH) TextDecoration.LineThrough else null)
                            if (CourseWork.hasTestOnLesson(event, events, assessmentDeadlines)) {
                                LessonTestBadge(compact = true)
                            }
                            if (active) {
                                val minutes = Duration.between(now, event.startTime).toMinutes()
                                Text(if (minutes > 0) "$minutes хв" else "Зараз", style = MaterialTheme.typography.labelSmall, color = accent)
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            LessonTypeIcon(event.lessonType)
                            Text(listOfNotNull(event.lessonType.takeUnless { it == LessonType.UNSPECIFIED }?.label, event.room, status, if (!active) "до ${event.endTime.format(time)}" else null).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall, color = muted)
                        }
                        if (event.isAbsent) {
                            Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Icon(Icons.Outlined.PersonOff, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                                Text("Пропущено", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                        if (active) {
                            Spacer(Modifier.height(13.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("До ${event.endTime.format(time)}", style = MaterialTheme.typography.bodySmall)
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(17.dp))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(13.dp))
            val following = events.getOrNull(index + 1)
            if (active && following != null) {
                val minutes = Duration.between(event.endTime, following.startTime).toMinutes()
                if (minutes > 0) {
                    Text("Перерва · ${if (minutes >= 60) "${minutes / 60} год " else ""}${minutes % 60} хв",
                        Modifier.padding(start = 55.dp, bottom = 14.dp), style = MaterialTheme.typography.bodySmall, color = muted)
                }
            }
        }
        // StudyTask stores a date, not a time: never invent a timed deadline.
        deadlines.forEach { task ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Без\nчасу", Modifier.width(timeWidth).padding(top = 13.dp), style = MaterialTheme.typography.bodySmall, color = amber)
                Row(Modifier.weight(1f).height(IntrinsicSize.Min)) {
                    Box(Modifier.width(2.dp).fillMaxHeight().background(amber.copy(alpha = 0.7f)))
                    Column(Modifier.weight(1f).padding(start = 13.dp, top = 13.dp, bottom = 13.dp)) {
                        Text("ДЕДЛАЙН", style = MaterialTheme.typography.labelSmall, color = amber)
                        HomeTaskRow(task, courseTitles[task.courseId] ?: task.courseId) { completed ->
                            scope.launch { studyContentRepository.setTaskCompleted(task.id, completed) }
                        }
                    }
                }
            }
            Spacer(Modifier.height(13.dp))
        }
        assessmentDeadlines.forEach { item ->
            val subjectColor = taktSubjectColor(item.courseId)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Без\nчасу", Modifier.width(timeWidth).padding(top = 13.dp), style = MaterialTheme.typography.bodySmall, color = subjectColor)
                Row(Modifier.weight(1f).height(IntrinsicSize.Min)) {
                    Box(Modifier.width(2.dp).fillMaxHeight().background(subjectColor))
                    Column(Modifier.weight(1f).padding(start = 13.dp, top = 13.dp, bottom = 13.dp)) {
                        Text(item.type.label.uppercase(uk), style = MaterialTheme.typography.labelSmall, color = subjectColor)
                        HomeAssessmentRow(item, courseTitles[item.courseId] ?: item.courseId) {
                            onOpenAssessment(item)
                        }
                    }
                }
            }
            Spacer(Modifier.height(13.dp))
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Без дати", style = MaterialTheme.typography.bodySmall, color = muted)
            Text("Задач: ${untimed.size}", style = MaterialTheme.typography.bodySmall, color = muted)
        }
        untimed.forEach { task ->
            HomeTaskRow(task, courseTitles[task.courseId] ?: task.courseId) { completed ->
                scope.launch { studyContentRepository.setTaskCompleted(task.id, completed) }
            }
        }
        Spacer(Modifier.height(76.dp))
    }
}

@Composable
private fun HomeWeekStrip(selected: LocalDate, onSelect: (LocalDate) -> Unit) {
    val weekday = remember { DateTimeFormatter.ofPattern("EE", Locale("uk")) }
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
        modifier = Modifier.fillMaxWidth().testTag("home-week-strip"),
    ) { page ->
        val week = anchorWeek.plusWeeks((page - centerPage).toLong())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
            repeat(7) { day ->
                val date = week.plusDays(day.toLong())
                val active = date == selected
                Column(Modifier.weight(1f).testTag("home-day-${date.toEpochDay()}").clip(RoundedCornerShape(12.dp))
                    .background(if (active) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .selectable(active, role = Role.Tab) { onSelect(date) }.padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(date.format(weekday).uppercase(), fontSize = 10.sp, lineHeight = 13.sp,
                        color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text(date.dayOfMonth.toString(), fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold,
                        color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}
