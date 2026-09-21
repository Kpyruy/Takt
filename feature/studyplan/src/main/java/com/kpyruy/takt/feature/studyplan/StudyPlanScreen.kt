package com.kpyruy.takt.feature.studyplan

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
import com.kpyruy.takt.core.model.CourseStatus
import com.kpyruy.takt.core.ui.components.SectionCard

@Composable
fun StudyPlanScreen(repository: StudyPlanRepository) {
    val courses by repository.observeCourses().collectAsState(initial = emptyList())
    val earned = courses.filter { it.status == CourseStatus.FULFILLED }.sumOf { it.credits }
    val semesters = courses.groupBy { it.semester }.toSortedMap()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        item {
            Text("Навчальний план", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        item {
            SectionCard(Modifier.padding(vertical = 12.dp)) {
                Text("Прогрес кредитів")
                Text("$earned / 180", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        }
        semesters.forEach { (semester, semesterCourses) ->
            item {
                Text(
                    "Семестр $semester",
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            items(semesterCourses, key = { it.id }) { course ->
                SectionCard(Modifier.padding(bottom = 10.dp)) {
                    Text(course.title, fontWeight = FontWeight.SemiBold)
                    Text("${course.credits} кредитів · ${course.status.name.replace('_', ' ')}")
                }
            }
        }
    }
}
