package com.kpyruy.takt.feature.calendar

import com.kpyruy.takt.core.ui.i18n.t

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.LessonType
import com.kpyruy.takt.core.model.AppLanguage
import com.kpyruy.takt.core.model.ResolvedScheduleEvent
import com.kpyruy.takt.core.model.isVisuallyMuted
import com.kpyruy.takt.core.model.ScheduleEventStatus
import com.kpyruy.takt.core.ui.components.CourseInlineIcon
import com.kpyruy.takt.core.ui.components.LessonTestBadge
import com.kpyruy.takt.core.ui.components.LessonTypeIcon
import com.kpyruy.takt.core.ui.components.lessonInteraction
import com.kpyruy.takt.core.ui.theme.taktSubjectColor
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.kpyruy.takt.core.ui.i18n.TaktI18n

private val dayTitle get() = DateTimeFormatter.ofPattern("EEEE", TaktI18n.locale)
private val dayDate get() = DateTimeFormatter.ofPattern("d MMM", TaktI18n.locale)
private val time = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun WeekCompactList(
    dates: List<LocalDate>,
    clock: LocalDateTime,
    eventsForDate: (LocalDate) -> List<ResolvedScheduleEvent>,
    workCountForDate: (LocalDate) -> Int = { 0 },
    hasTest: (ResolvedScheduleEvent) -> Boolean = { false },
    onSelectDate: (LocalDate) -> Unit,
    onEventClick: (ResolvedScheduleEvent) -> Unit,
    onEventLongClick: (ResolvedScheduleEvent) -> Unit,
) {
    Column(Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        dates.forEach { date ->
            val events = eventsForDate(date)
            val workCount = workCountForDate(date)
            Column(Modifier.fillMaxWidth().testTag("week-list-day-${date.toEpochDay()}")) {
                Row(Modifier.fillMaxWidth().clickable { onSelectDate(date) }
                    .heightIn(min = 48.dp).padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(date.format(dayTitle).replaceFirstChar { it.uppercase() },
                        Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold)
                    Text(date.format(dayDate), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth().border(1.dp,
                        MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))) {
                    Column(Modifier.padding(horizontal = 13.dp, vertical = 9.dp)) {
                        Row(Modifier.fillMaxWidth().padding(bottom = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text(localizedCount(events.size, "пара", "пари", "пар", "class", "classes", "hodina", "hodiny", "hodín"),
                                Modifier.weight(1f), style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (workCount > 0) {
                                Icon(Icons.Outlined.Assignment, null, Modifier.size(15.dp),
                                    tint = MaterialTheme.colorScheme.tertiary)
                                Text(localizedCount(workCount, "робота", "роботи", "робіт", "task", "tasks", "úloha", "úlohy", "úloh"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary)
                            }
                        }
                        if (events.isEmpty()) {
                            Text(t("Пар немає"), Modifier.padding(vertical = 8.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else events.forEachIndexed { index, event ->
                            if (index > 0) HorizontalDivider(Modifier.padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.outlineVariant)
                            WeekLessonRow(event, clock, hasTest(event),
                                onClick = { onEventClick(event) },
                                onLongClick = { onEventLongClick(event) })
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}

private fun ukrainianCount(count: Int, one: String, few: String, many: String): String = when {
    count % 100 in 11..14 -> many
    count % 10 == 1 -> one
    count % 10 in 2..4 -> few
    else -> many
}

private fun localizedCount(
    count: Int,
    ukOne: String, ukFew: String, ukMany: String,
    enOne: String, enMany: String,
    skOne: String, skFew: String, skMany: String,
): String {
    val noun = when (TaktI18n.language) {
        AppLanguage.ENGLISH -> if (count == 1) enOne else enMany
        AppLanguage.SLOVAK -> when (count) { 1 -> skOne; in 2..4 -> skFew; else -> skMany }
        else -> ukrainianCount(count, ukOne, ukFew, ukMany)
    }
    return "$count $noun"
}

@Composable
private fun WeekLessonRow(
    event: ResolvedScheduleEvent,
    clock: LocalDateTime,
    hasTest: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val subjectColor = taktSubjectColor(event.courseId ?: event.title)
    val muted = event.isVisuallyMuted(clock)
    val accentColor = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else subjectColor
    val status = when {
        event.isAbsent -> t("Пропущено")
        event.status == ScheduleEventStatus.CANCELLED -> t("Скасовано")
        event.status == ScheduleEventStatus.MOVED -> t("Перенесено")
        else -> null
    }
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).testTag("week-lesson-${event.id}")
        .alpha(if (muted) 0.6f else 1f)
        .lessonInteraction(onClick, onLongClick)
        .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(event.startTime.format(time), Modifier.width(45.dp),
            style = MaterialTheme.typography.labelLarge, color = accentColor)
        Box(Modifier.width(3.dp).fillMaxHeight().background(accentColor, RoundedCornerShape(3.dp)))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CourseInlineIcon(event.courseId)
                Text(event.title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (hasTest) LessonTestBadge(compact = true)
            }
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                if (event.lessonType == LessonType.UNSPECIFIED) {
                    Icon(Icons.Outlined.Event, t("Подія"), Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                } else LessonTypeIcon(event.lessonType)
                Text(buildList {
                    event.lessonType.takeUnless { it == LessonType.UNSPECIFIED }?.let { add(t(it.label)) }
                    event.room?.takeIf { it.isNotBlank() }?.let(::add)
                    add(t("до ${event.endTime.format(time)}"))
                }.joinToString(" · "), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (status != null) Text(status, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error)
        }
    }
}
