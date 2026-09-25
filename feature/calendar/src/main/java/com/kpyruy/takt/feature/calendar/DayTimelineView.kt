package com.kpyruy.takt.feature.calendar

import androidx.compose.foundation.layout.Arrangement

import androidx.compose.foundation.background
import com.kpyruy.takt.core.ui.components.lessonInteraction
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import com.kpyruy.takt.core.ui.components.CourseInlineIcon
import com.kpyruy.takt.core.ui.components.LessonTypeIcon
import com.kpyruy.takt.core.ui.components.LessonTestBadge
import com.kpyruy.takt.core.model.LessonType
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.CancellationDisplayStyle
import com.kpyruy.takt.core.model.DayTimelineInterval
import com.kpyruy.takt.core.model.DayTimelineLayout
import com.kpyruy.takt.core.model.ResolvedScheduleEvent
import com.kpyruy.takt.core.model.ScheduleEventStatus
import com.kpyruy.takt.core.ui.theme.taktSubjectColor
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.ceil

private val timelineTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val timelineHourHeight = 67.dp
private val timelineLabelWidth = 47.dp

@Composable
fun DayTimelineView(
    events: List<ResolvedScheduleEvent>,
    selectedDate: LocalDate,
    today: LocalDate,
    cancellationStyle: CancellationDisplayStyle,
    hasTest: (ResolvedScheduleEvent) -> Boolean = { false },
    onEventClick: (ResolvedScheduleEvent) -> Unit,
    onEventLongClick: (ResolvedScheduleEvent) -> Unit,
) {
    val ordered = events.sortedWith(compareBy<ResolvedScheduleEvent> { it.startTime }.thenBy { it.endTime })
    if (ordered.isEmpty()) return

    val rangeStartHour = minOf(9, ordered.minOf { it.startTime.hour })
    val latestMinute = ordered.maxOf { it.endTime.hour * 60 + it.endTime.minute }
    val rangeEndHour = maxOf(15, ceil(latestMinute / 60.0).toInt()).coerceAtMost(24)
    val rangeStart = LocalTime.of(rangeStartHour, 0)
    val rangeMinutes = (rangeEndHour - rangeStartHour) * 60L
    val totalHeight = timelineHourHeight * (rangeEndHour - rangeStartHour)
    val eventById = ordered.associateBy { it.layoutId }
    val placements = DayTimelineLayout.calculate(
        intervals = ordered.filter { it.endTime > it.startTime }.map { DayTimelineInterval(it.layoutId, it.startTime, it.endTime) },
        rangeStart = rangeStart,
    )
    val lifecycleOwner = LocalLifecycleOwner.current
    val now by produceState(initialValue = LocalTime.now(), selectedDate, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) { value = LocalTime.now(); delay(30_000L) }
        }
    }
    // A proportional grid must not let a large title paint over the following event.
    // Dense schedules and increased text sizes use the existing agenda interaction instead.
    val agenda = ordered.any { it.endTime <= it.startTime } || LocalDensity.current.fontScale > 1.15f ||
        placements.any { it.durationMinutes < 45 || it.laneCount > 2 }
    if (agenda) {
        Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
            Text("Події за часом", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            ordered.forEach { event ->
                TimelineEventBlock(event, cancellationStyle, hasTest(event), { onEventClick(event) },
                    onLongClick = { onEventLongClick(event) }, modifier = Modifier.fillMaxWidth(), dimmed = selectedDate == today && event.endTime < now, compact = false)
            }
        }
        return
    }

    BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 20.dp).height(totalHeight)) {
        Column(
            modifier = Modifier
                .offset(x = timelineLabelWidth)
                .width(maxWidth - timelineLabelWidth),
        ) {
            repeat(rangeEndHour - rangeStartHour) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(timelineHourHeight - 1.dp))
            }
        }

        repeat(rangeEndHour - rangeStartHour + 1) { index ->
            val hourMinute = (rangeStartHour + index) * 60
            if (selectedDate != today || kotlin.math.abs(now.hour * 60 + now.minute - hourMinute) > 12) Text(
                text = "%d:00".format(rangeStartHour + index),
                modifier = Modifier.offset(y = timelineHourHeight * index - 7.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        val eventAreaWidth = maxWidth - timelineLabelWidth
        placements.forEach { placement ->
            val event = eventById.getValue(placement.id)
            val laneWidth = eventAreaWidth / placement.laneCount.toFloat()
            TimelineEventBlock(
                event = event,
                cancellationStyle = cancellationStyle,
                hasTest = hasTest(event),
                onClick = { onEventClick(event) },
                onLongClick = { onEventLongClick(event) },
                modifier = Modifier
                    .offset(
                        x = timelineLabelWidth + laneWidth * placement.lane.toFloat(),
                        y = minutesToDp(placement.offsetMinutes),
                    )
                    .width(laneWidth)
                    .padding(end = if (placement.laneCount > 1) 3.dp else 0.dp)
                    .height(minutesToDp(placement.durationMinutes)),
                dimmed = selectedDate == today && event.endTime < now,
                short = placement.durationMinutes < 75,
            )
        }

        ordered.zipWithNext().forEach { (current, following) ->
            val gap = Duration.between(current.endTime, following.startTime).toMinutes()
            if (gap >= 35 && placements.all { it.laneCount == 1 }) {
                val offset = Duration.between(rangeStart, current.endTime).toMinutes() + gap / 2
                val currentOffset = Duration.between(rangeStart, now).toMinutes()
                if (selectedDate != today || kotlin.math.abs(currentOffset - offset) > 18) Text("Перерва · ${if (gap >= 60) "${gap / 60} год " else ""}${gap % 60} хв",
                    Modifier.offset(x = timelineLabelWidth + 13.dp, y = minutesToDp(offset) - 7.dp),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        val nowMinute = now.hour * 60 + now.minute
        val rangeEndMinute = rangeEndHour * 60
        if (selectedDate == today && nowMinute in (rangeStartHour * 60)..rangeEndMinute) {
            val nowOffset = Duration.between(rangeStart, now).toMinutes().coerceIn(0, rangeMinutes)
            Row(
                modifier = Modifier.fillMaxWidth().offset(y = minutesToDp(nowOffset) - 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    now.format(timelineTimeFormatter),
                    modifier = Modifier.width(timelineLabelWidth),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Box(
                    Modifier.width(6.dp).height(6.dp).background(
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(50),
                    ),
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun TimelineEventBlock(
    event: ResolvedScheduleEvent,
    cancellationStyle: CancellationDisplayStyle,
    hasTest: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier,
    dimmed: Boolean,
    compact: Boolean = true,
    short: Boolean = false,
) {
    val subjectColor = taktSubjectColor(event.courseId ?: event.title)
    val cancelled = event.status == ScheduleEventStatus.CANCELLED
    val status = if (event.isAbsent) "Пропущено" else when (event.status) {
        ScheduleEventStatus.CANCELLED -> "Скасовано"
        ScheduleEventStatus.MOVED -> "Перенесено"
        ScheduleEventStatus.ONE_OFF -> "Разова подія"
        ScheduleEventStatus.NORMAL -> null
    }
    val decoration = if (
        cancelled && cancellationStyle == CancellationDisplayStyle.STRIKETHROUGH
    ) TextDecoration.LineThrough else null

    Surface(
        modifier = modifier.alpha(if (dimmed || cancelled || event.isAbsent) 0.6f else 1f).semantics { contentDescription = "${event.title}, ${event.lessonType.label}, ${event.startTime}–${event.endTime}, ${event.room.orEmpty()}, ${status.orEmpty()}${if (hasTest) ", тест" else ""}" }.lessonInteraction(onClick, onLongClick),
        shape = RoundedCornerShape(7.dp),
        color = lerp(MaterialTheme.colorScheme.background, subjectColor, 0.13f),
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(3.dp).fillMaxHeight().background(subjectColor))
            Column(Modifier.padding(horizontal = 13.dp, vertical = if (short) 5.dp else 11.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CourseInlineIcon(event.courseId)
                    Text(
                        if (short && status != null) "$status · ${event.title}" else event.title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall,
                        textDecoration = decoration,
                        maxLines = if (!compact) Int.MAX_VALUE else if (short) 1 else 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (hasTest) LessonTestBadge(compact = true)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    LessonTypeIcon(event.lessonType)
                    Text(
                        buildString {
                            if (event.lessonType != LessonType.UNSPECIFIED) append(event.lessonType.shortLabel).append(" · ")
                            append(event.startTime.format(timelineTimeFormatter))
                            append("–")
                            append(event.endTime.format(timelineTimeFormatter))
                            event.room?.let { append(" · ").append(it) }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textDecoration = decoration,
                        maxLines = if (compact) 1 else Int.MAX_VALUE,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                status?.takeUnless { compact && short }?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = if (compact) 1 else Int.MAX_VALUE,
                        overflow = TextOverflow.Ellipsis,
                        color = if (cancelled || event.isAbsent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private val ResolvedScheduleEvent.layoutId: String
    get() = "$id|$startTime|$endTime|${exceptionId.orEmpty()}"

private fun minutesToDp(minutes: Long): Dp = (minutes / 60f * timelineHourHeight.value).dp
