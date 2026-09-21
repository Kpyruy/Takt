package com.kpyruy.takt.feature.subjects

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.data.StudyPlanRepository
import com.kpyruy.takt.core.ui.components.SectionCard

@Composable
fun SubjectsScreen(repository: StudyPlanRepository) {
    val courses by repository.observeSemester(3).collectAsState(initial = emptyList())

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text("Предмети", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("3 семестр", color = MaterialTheme.colorScheme.primary)
        LazyColumn(modifier = Modifier.padding(top = 14.dp)) {
            items(courses, key = { it.id }) { course ->
                SectionCard(Modifier.padding(bottom = 10.dp)) {
                    Text(course.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("${course.code} · ${course.credits} кредитів")
                    Text(course.status.name.replace('_', ' '), color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
