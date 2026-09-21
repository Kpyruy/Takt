package com.kpyruy.takt.feature.home

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
fun HomeScheduleCard(
    event: ResolvedScheduleEvent,
    cancellationStyle: CancellationDisplayStyle,
) {
    val cancelled = event.status == ScheduleEventStatus.CANCELLED
    val strike = cancelled && cancellationStyle == CancellationDisplayStyle.STRIKETHROUGH
    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val room = event.room
                Text(
                    text = "${event.startTime.format(timeFormatter)} – ${event.endTime.format(timeFormatter)}",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (strike) TextDecoration.LineThrough else null,
                )
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (strike) TextDecoration.LineThrough else null,
                )
                if (!room.isNullOrBlank()) {
                    Text(
                        text = room,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
