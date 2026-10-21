package com.kpyruy.takt.feature.home

import com.kpyruy.takt.core.model.uiMessage
import com.kpyruy.takt.core.model.UiKey
import com.kpyruy.takt.core.ui.i18n.t

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.ResolvedScheduleEvent
import com.kpyruy.takt.core.model.ScheduleEventStatus
import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
internal fun NextClassCard(event: ResolvedScheduleEvent, now: LocalTime, onClick: () -> Unit) {
    val current = now >= event.startTime && now < event.endTime
    val time = DateTimeFormatter.ofPattern("HH:mm")
    Card(
        onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text(if (current) t(UiKey.TEXT_NOW_IN_PROGRESS) else t(UiKey.TEXT_FOLLOWING_CLASS), style = MaterialTheme.typography.labelMedium)
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
            Text(event.title, style = MaterialTheme.typography.headlineSmall)
            Text(listOfNotNull(event.room?.let { t(uiMessage(UiKey.FORMAT_ROOM_VALUE, it)) },
                when (event.status) {
                    ScheduleEventStatus.MOVED -> t(UiKey.TEXT_RESCHEDULED)
                    ScheduleEventStatus.ONE_OFF -> t(UiKey.TEXT_ONE_TIME_EVENT)
                    else -> null
                }).joinToString(" · ").ifBlank { t(UiKey.TEXT_ROOM_NOT_SPECIFIED_YET) },
                style = MaterialTheme.typography.bodySmall)
            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.24f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text("${event.startTime.format(time)}–${event.endTime.format(time)}",
                    modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Text(if (current) t(UiKey.TEXT_NOW_FE18013D) else t(uiMessage(UiKey.FORMAT_IN_VALUE_MIN, Duration.between(now, event.startTime).toMinutes())),
                    style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
