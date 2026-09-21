package com.kpyruy.takt.feature.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.data.ScheduleRepository
import com.kpyruy.takt.core.model.ScheduleResolver
import com.kpyruy.takt.core.model.WeekParity
import com.kpyruy.takt.core.ui.components.ScreenHeader
import com.kpyruy.takt.core.ui.components.SectionCard
import com.kpyruy.takt.core.ui.components.StatusPill
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun CalendarScreen(scheduleRepository: ScheduleRepository) {
    val rules by scheduleRepository.observeRules().collectAsState(initial = emptyList())
    val oneOffEvents by scheduleRepository.observeOneOffEvents().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val today = LocalDate.now()
    var selectedDate by remember { mutableStateOf(today) }
    var weekStart by remember {
        mutableStateOf(today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))
    }
    var showAddSheet by remember { mutableStateOf(false) }

    val dates = (0L..6L).map { weekStart.plusDays(it) }
    val week = selectedDate.get(WeekFields.ISO.weekOfWeekBasedYear())
    val parity = WeekParity.fromIsoWeek(week)
    val events = ScheduleResolver.eventsForDate(
        rules = rules,
        exceptions = emptyList(),
        oneOffEvents = oneOffEvents,
        date = selectedDate,
    )
    val monthFormatter = DateTimeFormatter.ofPattern("d MMMM", Locale("uk"))

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            ScreenHeader(
                title = "Календар",
                subtitle = "${weekStart.format(monthFormatter)} – ${weekStart.plusDays(6).format(monthFormatter)}",
            )

            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = {
                        weekStart = weekStart.minusWeeks(1)
                        selectedDate = weekStart
                    }
                ) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Попередній тиждень")
                }
                StatusPill(
                    text = "$week · ${if (parity == WeekParity.EVEN) "Парний" else "Непарний"}",
                )
                IconButton(
                    onClick = {
                        weekStart = weekStart.plusWeeks(1)
                        selectedDate = weekStart
                    }
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Наступний тиждень")
                }
            }

            WeekDaySelector(
                dates = dates,
                selectedDate = selectedDate,
                onSelect = { selectedDate = it },
            )

            Text(
                text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("uk")))
                    .replaceFirstChar { it.uppercase() },
                modifier = Modifier.padding(top = 20.dp, bottom = 10.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            if (events.isEmpty()) {
                SectionCard {
                    Text("На цей день занять немає")
                    Text(
                        "Натисни +, щоб додати пару.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(bottom = 80.dp),
                ) {
                    items(events, key = { "${it.id}-${it.date}" }) {
                        ScheduleEventCard(it)
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddSheet = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = "Додати пару")
        }
    }

    if (showAddSheet) {
        AddLessonSheet(
            initialDay = selectedDate.dayOfWeek,
            onDismiss = { showAddSheet = false },
            onSave = { rule ->
                scope.launch {
                    scheduleRepository.upsertRule(rule)
                    showAddSheet = false
                }
            },
        )
    }
}
