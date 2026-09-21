package com.kpyruy.takt.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.data.ScheduleRepository
import com.kpyruy.takt.core.data.StudyPlanRepository
import com.kpyruy.takt.core.model.CourseStatus
import com.kpyruy.takt.core.model.ScheduleResolver
import com.kpyruy.takt.core.model.WeekParity
import com.kpyruy.takt.core.ui.components.MetricCard
import com.kpyruy.takt.core.ui.components.ScreenHeader
import com.kpyruy.takt.core.ui.components.SectionCard
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

@Composable
fun HomeScreen(
    repository: StudyPlanRepository,
    scheduleRepository: ScheduleRepository,
    onOpenSettings: () -> Unit,
) {
    val allCourses by repository.observeCourses().collectAsState(initial = emptyList())
    val semesterCourses by repository.observeSemester(3).collectAsState(initial = emptyList())
    val rules by scheduleRepository.observeRules().collectAsState(initial = emptyList())
    val oneOffEvents by scheduleRepository.observeOneOffEvents().collectAsState(initial = emptyList())
    val today = LocalDate.now()
    val week = today.get(WeekFields.ISO.weekOfWeekBasedYear())
    val parity = WeekParity.fromIsoWeek(week)
    val earnedCredits = allCourses.filter { it.status == CourseStatus.FULFILLED }.sumOf { it.credits }
    val enrolledCount = semesterCourses.count { it.status == CourseStatus.ENROLLED }
    val todayEvents = ScheduleResolver.eventsForDate(
        rules = rules,
        exceptions = emptyList(),
        oneOffEvents = oneOffEvents,
        date = today,
    )
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("uk"))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenHeader(
            title = "Takt",
            subtitle = today.format(dateFormatter).replaceFirstChar { it.uppercase() },
            action = {
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Налаштування")
                }
            },
        )

        Text(
            text = "$week тиждень · ${if (parity == WeekParity.EVEN) "Парний тиждень" else "Непарний тиждень"}",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )

        Text(
            text = "Сьогодні",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        if (todayEvents.isEmpty()) {
            SectionCard {
                Text("На сьогодні пар немає")
                Text(
                    "Можна використати день для домашок або підготовки.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            todayEvents.forEach { HomeScheduleCard(it) }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MetricCard(
                label = "Кредити",
                value = "$earnedCredits / 180",
                supporting = "Навчальний план",
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                label = "Предмети",
                value = enrolledCount.toString(),
                supporting = "Активні зараз",
                modifier = Modifier.weight(1f),
            )
        }

        SectionCard {
            Text("Прогрес навчання", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { (earnedCredits / 180f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "$earnedCredits із 180 кредитів уже закрито",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionCard {
            Text("Найближчі дедлайни", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Поки немає активних завдань", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        SectionCard {
            Text("Останні оцінки", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Після додавання балів вони з'являться тут", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
