package com.kpyruy.takt.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.data.AppSettingsRepository
import com.kpyruy.takt.core.data.GradeRepository
import com.kpyruy.takt.core.data.ScheduleRepository
import com.kpyruy.takt.core.data.StudyContentRepository
import com.kpyruy.takt.core.data.StudyPlanRepository
import com.kpyruy.takt.core.model.AppSettings
import com.kpyruy.takt.core.model.CancellationDisplayStyle
import com.kpyruy.takt.core.model.CourseStatus
import com.kpyruy.takt.core.model.ScheduleEventStatus
import com.kpyruy.takt.core.model.ScheduleResolver
import com.kpyruy.takt.core.model.ScheduleTimeline
import com.kpyruy.takt.core.model.StudyTaskPlanner
import com.kpyruy.takt.core.model.WeekParity
import com.kpyruy.takt.core.ui.components.CompactSummaryItem
import com.kpyruy.takt.core.ui.components.CompactSummaryStrip
import com.kpyruy.takt.core.ui.components.ScreenHeader
import com.kpyruy.takt.core.ui.components.SectionCard
import com.kpyruy.takt.core.ui.components.TaktTimeline
import com.kpyruy.takt.core.ui.components.TaktTimelineItem
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
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
    onOpenCourses: () -> Unit,
    onQuickAction: (HomeQuickAction) -> Unit,
) {
    val allCourses by repository.observeCourses().collectAsState(initial = emptyList())
    val semesterCourses by repository.observeSemester(3).collectAsState(initial = emptyList())
    val rules by scheduleRepository.observeRules().collectAsState(initial = emptyList())
    val oneOffEvents by scheduleRepository.observeOneOffEvents().collectAsState(initial = emptyList())
    val exceptions by scheduleRepository.observeExceptions().collectAsState(initial = emptyList())
    val allTasks by studyContentRepository.observeAllTasks().collectAsState(initial = emptyList())
    val recentGrades by gradeRepository.observeRecentItems(4).collectAsState(initial = emptyList())
    val settings by settingsRepository.settings.collectAsState(initial = AppSettings())

    val today = LocalDate.now()
    val now = LocalTime.now()
    val week = today.get(WeekFields.ISO.weekOfWeekBasedYear())
    val parity = settings.effectiveParity(today)
    val earnedCredits = allCourses.filter { it.status == CourseStatus.FULFILLED }.sumOf { it.credits }
    val enrolledCount = semesterCourses.count { it.status == CourseStatus.ENROLLED }

    val resolvedTodayEvents = ScheduleResolver.eventsForDate(
        rules = rules,
        exceptions = exceptions,
        oneOffEvents = oneOffEvents,
        date = today,
        parityOverride = parity,
    )
    val todayEvents = settings.filterScheduleEvents(resolvedTodayEvents).sortedBy { it.startTime }
    val nextEvent = ScheduleTimeline.nextEvent(todayEvents, now)
    val upcomingTasks = StudyTaskPlanner.upcoming(allTasks, today, 4)
    val incompleteToday = allTasks.count { !it.completed && it.dueDate == today }
    val overdueCount = allTasks.count {
        !it.completed && it.dueDate?.isBefore(today) == true
    }
    val courseTitles = allCourses.associate { it.id to it.title }
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("uk"))
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    val gapByEventIndex = buildMap {
        todayEvents.zipWithNext().forEachIndexed { index, (current, next) ->
            val minutes = Duration.between(current.endTime, next.startTime).toMinutes()
            if (minutes > 0) put(index, "Перерва $minutes хв")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenHeader(
            title = "Takt",
            subtitle = today.format(dateFormatter).replaceFirstChar { it.uppercase() },
            action = {
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Налаштування")
                }
            },
        )

        Text(
            text = "$week тиждень · ${if (parity == WeekParity.EVEN) "Парний" else "Непарний"}",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
        )

        nextEvent?.let { event ->
            SectionCard {
                Text("Наступна пара", style = MaterialTheme.typography.labelLarge)
                Text(event.title, style = MaterialTheme.typography.titleLarge, maxLines = 2)
                Text(
                    listOfNotNull(
                        "${event.startTime.format(timeFormatter)}–${event.endTime.format(timeFormatter)}",
                        event.room,
                    ).joinToString(" · "),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                when {
                    now >= event.startTime && now < event.endTime -> {
                        Text("Зараз", color = MaterialTheme.colorScheme.primary)
                    }
                    event.startTime > now -> {
                        val minutesUntil = Duration.between(now, event.startTime).toMinutes()
                        Text(
                            "Через $minutesUntil хв",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }

        CompactSummaryStrip(
            items = listOf(
                CompactSummaryItem(incompleteToday.toString(), "завдань сьогодні"),
                CompactSummaryItem(overdueCount.toString(), "прострочено"),
            )
        )

        Text("Сьогодні", style = MaterialTheme.typography.titleLarge)
        if (todayEvents.isEmpty()) {
            SectionCard {
                Text("На сьогодні пар немає")
                Text(
                    "Можна використати день для домашок або підготовки.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            TaktTimeline(
                items = todayEvents.map { event ->
                    val cancelled = event.status == ScheduleEventStatus.CANCELLED
                    val supporting = buildList {
                        event.room?.let(::add)
                        when (event.status) {
                            ScheduleEventStatus.MOVED -> add("Перенесено")
                            ScheduleEventStatus.CANCELLED -> {
                                if (settings.cancellationStyle != CancellationDisplayStyle.STRIKETHROUGH) {
                                    add("Скасовано")
                                }
                            }
                            ScheduleEventStatus.ONE_OFF -> add("Разова подія")
                            ScheduleEventStatus.NORMAL -> Unit
                        }
                    }.joinToString(" · ").ifBlank { null }

                    TaktTimelineItem(
                        time = event.startTime.format(timeFormatter),
                        title = event.title,
                        supporting = supporting,
                        markerColor = MaterialTheme.colorScheme.primary,
                        emphasized = event.id == nextEvent?.id,
                        dimmed = event.endTime < now,
                        strikethrough = cancelled &&
                            settings.cancellationStyle == CancellationDisplayStyle.STRIKETHROUGH,
                    )
                },
                gapLabels = gapByEventIndex,
            )
        }

        SectionCard {
            Text("Прогрес навчання", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(2.dp))
            LinearProgressIndicator(
                progress = { (earnedCredits / 180f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "$earnedCredits із 180 кредитів закрито",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionCard(
            modifier = Modifier.clickable(onClick = onOpenCourses),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Курси", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "$enrolledCount активних · цей семестр",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(Icons.Default.ChevronRight, contentDescription = "Відкрити курси")
            }
        }

        SectionCard {
            Text("Найближчі дедлайни", style = MaterialTheme.typography.titleMedium)
            if (upcomingTasks.isEmpty()) {
                Text("Поки немає активних дедлайнів", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                upcomingTasks.forEachIndexed { index, task ->
                    if (index > 0) HorizontalDivider()
                    HomeTaskRow(
                        task = task,
                        courseTitle = courseTitles[task.courseId] ?: task.courseId,
                    )
                }
            }
        }

        SectionCard {
            Text("Останні оцінки", style = MaterialTheme.typography.titleMedium)
            if (recentGrades.isEmpty()) {
                Text("Після додавання балів вони з'являться тут", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                recentGrades.forEachIndexed { index, item ->
                    if (index > 0) HorizontalDivider()
                    HomeGradeRow(
                        item = item,
                        courseTitle = courseTitles[item.courseId] ?: item.courseId,
                    )
                }
            }
        }

        Text("Швидкі дії", style = MaterialTheme.typography.titleMedium)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { onQuickAction(HomeQuickAction.LESSON) },
                    modifier = Modifier.weight(1f),
                ) { Text("Пара") }
                OutlinedButton(
                    onClick = { onQuickAction(HomeQuickAction.TASK) },
                    modifier = Modifier.weight(1f),
                ) { Text("Завдання") }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { onQuickAction(HomeQuickAction.EXAM) },
                    modifier = Modifier.weight(1f),
                ) { Text("Екзамен") }
                OutlinedButton(
                    onClick = { onQuickAction(HomeQuickAction.NOTE) },
                    modifier = Modifier.weight(1f),
                ) { Text("Нотатка") }
            }
        }

        if (upcomingTasks.isEmpty() && todayEvents.isNotEmpty()) {
            Text(
                "На найближчі дні немає активних дедлайнів.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
