package com.kpyruy.takt.feature.calendar

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.kpyruy.takt.core.model.CancellationDisplayStyle
import com.kpyruy.takt.core.model.ResolvedScheduleEvent
import com.kpyruy.takt.core.model.ScheduleEventStatus
import com.kpyruy.takt.core.model.ScheduleTimeline
import com.kpyruy.takt.core.ui.components.TaktTimeline
import com.kpyruy.takt.core.ui.components.TaktTimelineItem
import com.kpyruy.takt.core.ui.theme.taktSubjectColor
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val timelineTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun DayTimelineView(
    events: List<ResolvedScheduleEvent>,
    selectedDate: LocalDate,
    today: LocalDate,
    cancellationStyle: CancellationDisplayStyle,
    onEventClick: (ResolvedScheduleEvent) -> Unit,
) {
    val ordered = events.sortedBy { it.startTime }
    val now = LocalTime.now()
    val next = if (selectedDate == today) ScheduleTimeline.nextEvent(ordered, now) else null
    val gaps = buildMap {
        ordered.zipWithNext().forEachIndexed { index, (current, following) ->
            val minutes = Duration.between(current.endTime, following.startTime).toMinutes()
            if (minutes > 0) put(index, "Вільно $minutes хв")
        }
    }

    val currentMarkerIndex = if (selectedDate == today) {
        ordered.indexOfFirst { it.endTime >= now }.let { if (it >= 0) it else ordered.size }
    } else {
        null
    }

    TaktTimeline(
        items = ordered.map { event ->
            val cancelled = event.status == ScheduleEventStatus.CANCELLED
            val supporting = buildList {
                event.room?.let(::add)
                when (event.status) {
                    ScheduleEventStatus.MOVED -> add("Перенесено")
                    ScheduleEventStatus.CANCELLED -> {
                        if (cancellationStyle != CancellationDisplayStyle.STRIKETHROUGH) add("Скасовано")
                    }
                    ScheduleEventStatus.ONE_OFF -> add("Разова подія")
                    ScheduleEventStatus.NORMAL -> Unit
                }
            }.joinToString(" · ").ifBlank { null }

            TaktTimelineItem(
                time = event.startTime.format(timelineTimeFormatter) + "\n" +
                    event.endTime.format(timelineTimeFormatter),
                title = event.title,
                supporting = supporting,
                markerColor = taktSubjectColor(event.courseId ?: event.title),
                emphasized = event.id == next?.id,
                dimmed = selectedDate == today && event.endTime < now,
                strikethrough = cancelled &&
                    cancellationStyle == CancellationDisplayStyle.STRIKETHROUGH,
            )
        },
        gapLabels = gaps,
        onItemClick = { index -> onEventClick(ordered[index]) },
        currentTimeBeforeIndex = currentMarkerIndex,
        currentTimeLabel = if (selectedDate == today) {
            "Зараз\n" + now.format(timelineTimeFormatter)
        } else {
            null
        },
    )
}
