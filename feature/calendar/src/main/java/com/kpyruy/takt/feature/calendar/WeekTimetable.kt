package com.kpyruy.takt.feature.calendar

import com.kpyruy.takt.core.ui.i18n.t

import androidx.compose.ui.Alignment

import com.kpyruy.takt.core.ui.components.lessonInteraction
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import com.kpyruy.takt.core.ui.components.TaktSurface as Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.ResolvedScheduleEvent
import com.kpyruy.takt.core.model.isVisuallyMuted
import com.kpyruy.takt.core.ui.theme.taktSubjectColor
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.kpyruy.takt.core.ui.i18n.TaktI18n

private val timetableHourHeight = 60.dp
private val timetableDayFormatter get() = DateTimeFormatter.ofPattern("EEE d", TaktI18n.locale)
private val timetableTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private data class LaneEvent(
    val event: ResolvedScheduleEvent,
    val lane: Int,
    val laneCount: Int,
)

private fun layoutIntoLanes(events: List<ResolvedScheduleEvent>): List<LaneEvent> {
    val ordered = events.sortedBy { it.startTime }
    val laneEnds = mutableListOf<java.time.LocalTime>()
    val assignments = mutableListOf<Pair<ResolvedScheduleEvent, Int>>()

    ordered.forEach { event ->
        val freeLane = laneEnds.indexOfFirst { it <= event.startTime }
        val lane = if (freeLane >= 0) {
            laneEnds[freeLane] = event.endTime
            freeLane
        } else {
            laneEnds += event.endTime
            laneEnds.lastIndex
        }
        assignments += event to lane
    }

    val count = laneEnds.size.coerceAtLeast(1)
    return assignments.map { (event, lane) -> LaneEvent(event, lane, count) }
}

private fun minuteOfDay(hour: Int, minute: Int): Int = hour * 60 + minute

private fun eventTop(event: ResolvedScheduleEvent, startHour: Int): Dp {
    val start = minuteOfDay(event.startTime.hour, event.startTime.minute)
    val base = startHour * 60
    return ((start - base).coerceAtLeast(0) / 60f * timetableHourHeight.value).dp
}

private fun eventHeight(event: ResolvedScheduleEvent): Dp {
    val duration = java.time.Duration.between(event.startTime, event.endTime).toMinutes()
    return (duration.coerceAtLeast(20) / 60f * timetableHourHeight.value).dp
}

@Composable
fun WeekTimetable(
    dates: List<LocalDate>,
    clock: LocalDateTime,
    eventsForDate: (LocalDate) -> List<ResolvedScheduleEvent>,
    assessmentCountForDate: (LocalDate) -> Int = { 0 },
    hasTest: (ResolvedScheduleEvent) -> Boolean = { false },
    onEventClick: (ResolvedScheduleEvent) -> Unit,
    onEventLongClick: (ResolvedScheduleEvent) -> Unit,
    onSelectDate: (LocalDate) -> Unit = {},
) {
    val vertical = rememberScrollState()
    val allEvents = dates.flatMap(eventsForDate)
    val visibleDates = dates.filter { it.dayOfWeek.value <= 5 || eventsForDate(it).isNotEmpty() || assessmentCountForDate(it) > 0 }
    val startHour = allEvents.minOfOrNull { it.startTime.hour } ?: 8
    val endHour = allEvents.maxOfOrNull { (it.endTime.hour * 60 + it.endTime.minute + 59) / 60 }?.coerceAtLeast(startHour + 1) ?: 16
    val totalHeight = timetableHourHeight * (endHour - startHour)
    val fontScale = LocalDensity.current.fontScale
    val lanesByDate = visibleDates.associateWith { layoutIntoLanes(eventsForDate(it)) }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val timetableColumnWidth = (maxWidth - 48.dp) / visibleDates.size.coerceAtLeast(1)
        val crowded = fontScale > 1.15f || lanesByDate.values.any { lanes ->
            val laneCount = lanes.maxOfOrNull { it.laneCount } ?: 1
            laneCount > 1 || timetableColumnWidth / laneCount.toFloat() < 48.dp ||
                lanes.any { java.time.Duration.between(it.event.startTime, it.event.endTime).toMinutes() < 45 }
        }
        if (crowded) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    t("Для читабельності тиждень показано списком."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                WeekCompactList(
                    dates = visibleDates,
                    clock = clock,
                    eventsForDate = eventsForDate,
                    workCountForDate = assessmentCountForDate,
                    hasTest = hasTest,
                    onSelectDate = onSelectDate,
                    onEventClick = onEventClick,
                    onEventLongClick = onEventLongClick,
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .verticalScroll(vertical)
                    .padding(bottom = 80.dp),
            ) {
                Column(
                    modifier = Modifier.width(48.dp),
                ) {
                    Spacer(Modifier.height(48.dp))
                    repeat(endHour - startHour) { index ->
                        Text(
                            text = "%02d:00".format(startHour + index),
                            modifier = Modifier.height(timetableHourHeight).padding(top = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                visibleDates.forEach { date ->
                    Column(
                        modifier = Modifier.width(timetableColumnWidth),
                    ) {
                        Surface(
                            modifier = Modifier.width(timetableColumnWidth).height(48.dp)
                                .clickable { onSelectDate(date) },
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Text(
                                text = date.format(timetableDayFormatter).replace(".", "").uppercase() +
                                    assessmentCountForDate(date).takeIf { it > 0 }?.let { " · $it" }.orEmpty(),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(timetableColumnWidth)
                                .height(totalHeight),
                        ) {
                            Column(Modifier.fillMaxHeight()) {
                                repeat(endHour - startHour) {
                                    HorizontalDivider()
                                    Spacer(Modifier.height(timetableHourHeight - 1.dp))
                                }
                            }

                            lanesByDate.getValue(date).forEach { laneEvent ->
                                val width = timetableColumnWidth / laneEvent.laneCount.toFloat()
                                val subjectColor = taktSubjectColor(
                                    laneEvent.event.courseId ?: laneEvent.event.title
                                )
                                val muted = laneEvent.event.isVisuallyMuted(clock)
                                Surface(
                                    modifier = Modifier
                                        .offset(
                                            x = width * laneEvent.lane.toFloat(),
                                            y = eventTop(laneEvent.event, startHour),
                                        )
                                        .width(width)
                                        .height(eventHeight(laneEvent.event))
                                        .padding(horizontal = 2.dp, vertical = 1.dp)
                                        .alpha(if (muted) 0.6f else 1f)
                                        .lessonInteraction({ onEventClick(laneEvent.event) }, { onEventLongClick(laneEvent.event) }),
                                    shape = MaterialTheme.shapes.extraSmall,
                                    color = if (muted) MaterialTheme.colorScheme.surfaceContainerLow
                                        else subjectColor.copy(alpha = 0.18f),
                                ) {
                                    Column(Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                listOf(if (laneEvent.event.isAbsent) t("Пропущено") else "", t(laneEvent.event.lessonType.shortLabel), laneEvent.event.title).filter { it.isNotBlank() }.joinToString(" · "),
                                                modifier = Modifier.weight(1f),
                                                style = MaterialTheme.typography.labelMedium,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            if (hasTest(laneEvent.event)) {
                                                com.kpyruy.takt.core.ui.components.LessonTestBadge(compact = true)
                                            }
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            com.kpyruy.takt.core.ui.components.CourseInlineIcon(laneEvent.event.courseId)
                                            Text(laneEvent.event.startTime.format(timetableTimeFormatter),
                                                style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        laneEvent.event.room?.let {
                                            Text(
                                                it,
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
