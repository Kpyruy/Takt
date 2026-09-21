package com.kpyruy.takt.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.data.StudyPlanRepository
import com.kpyruy.takt.core.model.CourseStatus
import com.kpyruy.takt.core.model.WeekParity
import com.kpyruy.takt.core.ui.components.SectionCard
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields

@Composable
fun HomeScreen(repository: StudyPlanRepository) {
    val courses by repository.observeSemester(3).collectAsState(initial = emptyList())
    val today = LocalDate.now()
    val week = today.get(WeekFields.ISO.weekOfWeekBasedYear())
    val parity = WeekParity.fromIsoWeek(week)
    val completedCredits = courses.filter { it.status == CourseStatus.FULFILLED }.sumOf { it.credits }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Takt", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "$week тиждень · ${if (parity == WeekParity.EVEN) "Парний тиждень" else "Непарний тиждень"}",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )

        SectionCard {
            Text("Сьогодні", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text("Розклад ще не заповнений. У календарі можна буде додавати пари натисканням на вільний час.")
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionCard(Modifier.weight(1f)) {
                Text("Предмети")
                Text("${courses.size}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            SectionCard(Modifier.weight(1f)) {
                Text("Кредити семестру")
                Text("$completedCredits", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        }

        SectionCard {
            Text("Найближчі дедлайни", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Поки немає активних завдань")
        }
    }
}
