package com.kpyruy.takt.feature.calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.kpyruy.takt.core.model.CancellationDisplayStyle
import com.kpyruy.takt.core.model.ResolvedScheduleEvent
import com.kpyruy.takt.core.model.ScheduleEventStatus
import com.kpyruy.takt.core.ui.components.SectionCard
import com.kpyruy.takt.core.ui.components.StatusPill
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun ScheduleEventCard(
    event: ResolvedScheduleEvent,
    cancellationStyle: CancellationDisplayStyle,
    onClick: () -> Unit,
) {
    val cancelled = event.status == ScheduleEventStatus.CANCELLED
    val strike = cancelled && cancellationStyle == CancellationDisplayStyle.STRIKETHROUGH
    SectionCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            com.kpyruy.takt.core.ui.components.CourseInlineIcon(event.courseId)
            Column(modifier = Modifier.weight(1f)) {
                val room = event.room
                Text(
                    "${event.startTime.format(timeFormatter)} – ${event.endTime.format(timeFormatter)}",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (strike) TextDecoration.LineThrough else null,
                )
                Text(
                    event.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (strike) TextDecoration.LineThrough else null,
                )
                if (event.lessonType != com.kpyruy.takt.core.model.LessonType.UNSPECIFIED) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        com.kpyruy.takt.core.ui.components.LessonTypeIcon(event.lessonType)
                        Text(" ${event.lessonType.label}", style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (!room.isNullOrBlank()) {
                    Text(room, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            when (event.status) {
                ScheduleEventStatus.ONE_OFF -> StatusPill("Блок")
                ScheduleEventStatus.MOVED -> StatusPill("Перенесено")
                ScheduleEventStatus.CANCELLED -> {
                    if (cancellationStyle != CancellationDisplayStyle.STRIKETHROUGH) {
                        StatusPill("Скасовано")
                    }
                }
                ScheduleEventStatus.NORMAL -> Unit
            }
        }
    }
}
