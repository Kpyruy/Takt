package com.kpyruy.takt.feature.calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.ResolvedScheduleEvent
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val TIMETABLE_START_HOUR = 7
private const val TIMETABLE_END_HOUR = 21
private val timetableHourHeight = 60.dp
private val timetableColumnWidth = 132.dp
private val timetableDayFormatter = DateTimeFormatter.ofPattern("EEE d", Locale("uk"))
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

private fun eventTop(event: ResolvedScheduleEvent): Dp {
    val start = minuteOfDay(event.startTime.hour, event.startTime.minute)
    val base = TIMETABLE_START_HOUR * 60
    return ((start - base).coerceAtLeast(0) / 60f * timetableHourHeight.value).dp
}

private fun eventHeight(event: ResolvedScheduleEvent): Dp {
    val duration = java.time.Duration.between(event.startTime, event.endTime).toMinutes()
    return (duration.coerceAtLeast(20) / 60f * timetableHourHeight.value).dp
}

@Composable
fun WeekTimetable(
    dates: List<LocalDate>,
    eventsForDate: (LocalDate) -> List<ResolvedScheduleEvent>,
    onEventClick: (ResolvedScheduleEvent) -> Unit,
) {
    val horizontal = rememberScrollState()
    val vertical = rememberScrollState()
    val totalHeight = timetableHourHeight * (TIMETABLE_END_HOUR - TIMETABLE_START_HOUR)

    Row(
        modifier = Modifier
            .horizontalScroll(horizontal)
            .verticalScroll(vertical),
    ) {
        Column(
            modifier = Modifier.width(48.dp),
        ) {
            Spacer(Modifier.height(48.dp))
            repeat(TIMETABLE_END_HOUR - TIMETABLE_START_HOUR) { index ->
                Text(
                    text = "%02d:00".format(TIMETABLE_START_HOUR + index),
                    modifier = Modifier.height(timetableHourHeight).padding(top = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        dates.forEach { date ->
            Column(
                modifier = Modifier.width(timetableColumnWidth),
            ) {
                Surface(
                    modifier = Modifier.width(timetableColumnWidth).height(48.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Text(
                        text = date.format(timetableDayFormatter).replace(".", "").uppercase(),
                        modifier = Modifier.padding(10.dp),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }

                Box(
                    modifier = Modifier
                        .width(timetableColumnWidth)
                        .height(totalHeight),
                ) {
                    Column(Modifier.fillMaxHeight()) {
                        repeat(TIMETABLE_END_HOUR - TIMETABLE_START_HOUR) {
                            HorizontalDivider()
                            Spacer(Modifier.height(timetableHourHeight - 1.dp))
                        }
                    }

                    layoutIntoLanes(eventsForDate(date)).forEach { laneEvent ->
                        val width = timetableColumnWidth / laneEvent.laneCount.toFloat()
                        Surface(
                            modifier = Modifier
                                .offset(
                                    x = width * laneEvent.lane.toFloat(),
                                    y = eventTop(laneEvent.event),
                                )
                                .width(width)
                                .height(eventHeight(laneEvent.event))
                                .padding(horizontal = 2.dp, vertical = 1.dp)
                                .clickable { onEventClick(laneEvent.event) },
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Column(Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    laneEvent.event.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    laneEvent.event.startTime.format(timetableTimeFormatter),
                                    style = MaterialTheme.typography.labelSmall,
                                )
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
