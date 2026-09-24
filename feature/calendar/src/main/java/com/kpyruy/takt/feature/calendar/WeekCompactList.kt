package com.kpyruy.takt.feature.calendar

import com.kpyruy.takt.core.ui.components.lessonInteraction
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.ResolvedScheduleEvent
import com.kpyruy.takt.core.ui.theme.taktSubjectColor
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val weekDayTitleFormatter = DateTimeFormatter.ofPattern("EEEE, d MMM", Locale("uk"))
private val weekTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun WeekCompactList(
    dates: List<LocalDate>,
    eventsForDate: (LocalDate) -> List<ResolvedScheduleEvent>,
    assessmentCountForDate: (LocalDate) -> Int = { 0 },
    hasTest: (ResolvedScheduleEvent) -> Boolean = { false },
    onSelectDate: (LocalDate) -> Unit,
    onEventClick: (ResolvedScheduleEvent) -> Unit,
    onEventLongClick: (ResolvedScheduleEvent) -> Unit,
) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        dates.forEach { date ->
            val events = eventsForDate(date)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable { onSelectDate(date) },
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        date.format(weekDayTitleFormatter).replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleMedium,
                    )
                    val assessmentCount = assessmentCountForDate(date)
                    if (assessmentCount > 0) {
                        Text("Робіт з датою: $assessmentCount", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                    if (events.isEmpty()) {
                        Text(
                            "Немає занять",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        events.forEachIndexed { index, event ->
                            if (index > 0) HorizontalDivider(Modifier.padding(vertical = 8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .lessonInteraction({ onEventClick(event) }, { onEventLongClick(event) })
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text(
                                    event.startTime.format(weekTimeFormatter),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = taktSubjectColor(event.courseId ?: event.title),
                                )
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        com.kpyruy.takt.core.ui.components.CourseInlineIcon(event.courseId)
                                        Text(listOf(event.lessonType.shortLabel, event.title).filter { it.isNotBlank() }.joinToString(" · "),
                                            modifier = Modifier.weight(1f), maxLines = 2)
                                        if (hasTest(event)) com.kpyruy.takt.core.ui.components.LessonTestBadge(compact = true)
                                    }
                                    if (event.isAbsent) Text("Пропущено", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                    event.room?.let {
                                        Text(
                                            it,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}
