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
import com.kpyruy.takt.core.model.ResolvedScheduleEvent
import com.kpyruy.takt.core.model.ScheduleEventStatus
import com.kpyruy.takt.core.ui.components.SectionCard
import com.kpyruy.takt.core.ui.components.StatusPill
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun ScheduleEventCard(
    event: ResolvedScheduleEvent,
    onClick: () -> Unit,
) {
    val cancelled = event.status == ScheduleEventStatus.CANCELLED
    SectionCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val room = event.room
                Text(
                    "${event.startTime.format(timeFormatter)} – ${event.endTime.format(timeFormatter)}",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (cancelled) TextDecoration.LineThrough else null,
                )
                Text(
                    event.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (cancelled) TextDecoration.LineThrough else null,
                )
                if (!room.isNullOrBlank()) {
                    Text(room, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            when (event.status) {
                ScheduleEventStatus.ONE_OFF -> StatusPill("Блок")
                ScheduleEventStatus.MOVED -> StatusPill("Перенесено")
                ScheduleEventStatus.CANCELLED -> StatusPill("Скасовано")
                ScheduleEventStatus.NORMAL -> Unit
            }
        }
    }
}
