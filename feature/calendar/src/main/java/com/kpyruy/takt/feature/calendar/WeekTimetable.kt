package com.kpyruy.takt.feature.calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.*
import com.kpyruy.takt.core.ui.components.CourseInlineIcon
import com.kpyruy.takt.core.ui.components.LessonTestBadge
import com.kpyruy.takt.core.ui.components.LessonTypeIcon
import com.kpyruy.takt.core.ui.components.TaktSurface
import com.kpyruy.takt.core.ui.components.lessonInteraction
import com.kpyruy.takt.core.ui.i18n.TaktI18n
import com.kpyruy.takt.core.ui.i18n.t
import com.kpyruy.takt.core.ui.theme.taktSubjectColor
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val timetableDayFormatter get() = DateTimeFormatter.ofPattern("EEE d", TaktI18n.locale)
private val timetableTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

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
    initiallyVisibleDate: LocalDate = clock.toLocalDate(),
) {
    var zoom by rememberSaveable { mutableFloatStateOf(1f) }
    val vertical = rememberScrollState()
    val horizontal = rememberScrollState()
    val eventsByDate = dates.associateWith(eventsForDate)
    val allEvents = eventsByDate.values.flatten()
    val visibleDates = dates.filter {
        it.dayOfWeek.value <= 5 || eventsByDate.getValue(it).isNotEmpty() || assessmentCountForDate(it) > 0
    }
    val startHour = allEvents.minOfOrNull { it.startTime.hour } ?: 8
    val endHour = allEvents.maxOfOrNull { (it.endTime.hour * 60 + it.endTime.minute + 59) / 60 }
        ?.coerceAtLeast(startHour + 1) ?: 16
    val fontScale = LocalDensity.current.fontScale
    val hourHeight = (TimetableLayout.hourHeight(allEvents, fontScale) * zoom).dp
    val totalHeight = hourHeight * (endHour - startHour)
    val headerHeight = (48f * fontScale.coerceAtLeast(1f)).dp
    val lanesByDate = visibleDates.associateWith { TimetableLayout.lanes(eventsByDate.getValue(it)) }
    val minLaneWidth = (184f * fontScale.coerceAtLeast(1f) * zoom).dp
    val density = LocalDensity.current
    // The time axis stays fixed while day columns scroll horizontally. Both share one vertical scroll.
    BoxWithConstraints(Modifier.fillMaxWidth().clipToBounds().testTag("week-timetable")
        .semantics { stateDescription = "${(zoom * 100).toInt()}%" }
        .pointerInput(Unit) {
            // Leave one-finger scrolling to the scroll containers; consume only a pinch.
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                do {
                    val event = awaitPointerEvent()
                    if (event.changes.count { it.pressed } >= 2) {
                        val factor = event.calculateZoom()
                        if (factor.isFinite() && factor > 0f) zoom = (zoom * factor).coerceIn(.6f, 1.8f)
                        event.changes.forEach { if (it.pressed) it.consume() }
                    }
                } while (event.changes.any { it.pressed })
            }
        }) {
        val axisWidth = (48f * fontScale.coerceAtLeast(1f)).dp
        val availableDayWidth = ((maxWidth - axisWidth) / visibleDates.size.coerceAtLeast(1)).coerceAtLeast(minLaneWidth)
        val widths = visibleDates.map { date -> availableDayWidth * maxOf(1, lanesByDate.getValue(date).maxOfOrNull { it.laneCount } ?: 1) }
        LaunchedEffect(visibleDates, initiallyVisibleDate, maxWidth) {
            val index = visibleDates.indexOf(initiallyVisibleDate).coerceAtLeast(0)
            val precedingWidth = widths.take(index).sumOf { it.value.toDouble() }.toFloat()
            horizontal.scrollTo(with(density) { precedingWidth.dp.roundToPx() })
        }
        Row(Modifier.fillMaxWidth().verticalScroll(vertical).padding(bottom = 80.dp)) {
            Column(Modifier.width(axisWidth).testTag("week-timetable-axis")) {
                Spacer(Modifier.height(headerHeight))
                repeat(endHour - startHour) { index ->
                    Text("%02d:00".format(startHour + index),
                        Modifier.height(hourHeight).padding(top = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(Modifier.weight(1f).clipToBounds().horizontalScroll(horizontal).testTag("week-timetable-days")) {
                visibleDates.forEach { date ->
                    val lanes = lanesByDate.getValue(date)
                    val dayLaneCount = lanes.maxOfOrNull { it.laneCount } ?: 1
                    val columnWidth = maxOf(availableDayWidth, minLaneWidth * dayLaneCount)
                    Column(Modifier.width(columnWidth).testTag("week-timetable-day-${date.toEpochDay()}")) {
                        TaktSurface(
                            modifier = Modifier.fillMaxWidth().height(headerHeight).clickable { onSelectDate(date) },
                            shape = MaterialTheme.shapes.small,
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(date.format(timetableDayFormatter).replace(".", "").uppercase(TaktI18n.locale) +
                                    assessmentCountForDate(date).takeIf { it > 0 }?.let { " · $it" }.orEmpty(),
                                    style = MaterialTheme.typography.labelLarge, maxLines = 1)
                            }
                        }
                        Box(Modifier.width(columnWidth).height(totalHeight)) {
                            Column(Modifier.fillMaxHeight()) {
                                repeat(endHour - startHour) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                                    Spacer(Modifier.height(hourHeight - 1.dp))
                                }
                            }
                            lanes.forEach { lane ->
                                val event = lane.event
                                val width = columnWidth / lane.laneCount.toFloat()
                                val topMinutes = event.startTime.hour * 60 + event.startTime.minute - startHour * 60
                                val eventHeight = hourHeight * (TimetableLayout.durationMinutes(event) / 60f)
                                val subjectColor = taktSubjectColor(event.courseId ?: event.title)
                                val muted = event.isVisuallyMuted(clock)
                                val accentColor = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else subjectColor
                                val status = when {
                                    event.isAbsent -> t("Пропущено")
                                    event.status == ScheduleEventStatus.CANCELLED -> t("Скасовано")
                                    event.status == ScheduleEventStatus.MOVED -> t("Перенесено")
                                    event.status == ScheduleEventStatus.ONE_OFF -> t("Разова подія")
                                    else -> null
                                }
                                TaktSurface(
                                    modifier = Modifier.offset(x = width * lane.lane.toFloat(), y = hourHeight * (topMinutes / 60f))
                                        .width(width).height(eventHeight).padding(horizontal = 3.dp, vertical = 1.dp)
                                        .alpha(if (muted) 0.6f else 1f)
                                        .testTag("week-timetable-event-${event.id}")
                                        .semantics {
                                            contentDescription = listOfNotNull(event.title, t(event.lessonType.label),
                                                "${event.startTime}–${event.endTime}", event.room, status,
                                                if (hasTest(event)) t("тест") else null).joinToString(", ")
                                        }
                                        .lessonInteraction({ onEventClick(event) }, { onEventLongClick(event) }),
                                    shape = MaterialTheme.shapes.small,
                                    color = subjectColor.copy(alpha = if (muted) 0.04f else 0.1f),
                                ) {
                                    Column(Modifier.fillMaxSize().calendarEventAccent(accentColor)
                                        .padding(start = 10.dp, end = 8.dp, top = 7.dp, bottom = 7.dp),
                                        verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            CourseInlineIcon(event.courseId)
                                            Text(event.title, Modifier.weight(1f),
                                                style = MaterialTheme.typography.labelLarge,
                                                maxLines = 3, overflow = TextOverflow.Ellipsis)
                                            if (hasTest(event)) LessonTestBadge(compact = true)
                                        }
                                        Text("${event.startTime.format(timetableTimeFormatter)}–${event.endTime.format(timetableTimeFormatter)}",
                                            style = MaterialTheme.typography.labelMedium, maxLines = 1)
                                        Row(verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            LessonTypeIcon(event.lessonType)
                                            Text(t(event.lessonType.label), style = MaterialTheme.typography.labelSmall,
                                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        event.room?.let {
                                            Text(it, style = MaterialTheme.typography.labelSmall, maxLines = 1,
                                                overflow = TextOverflow.Ellipsis)
                                        }
                                        status?.let {
                                            Text(it, style = MaterialTheme.typography.labelSmall, maxLines = 1,
                                                overflow = TextOverflow.Ellipsis)
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
