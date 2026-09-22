package com.kpyruy.takt.feature.studyplan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.data.StudyPlanRepository
import com.kpyruy.takt.core.model.CourseStatus
import com.kpyruy.takt.core.ui.components.ScreenHeader
import com.kpyruy.takt.core.ui.components.SectionCard

@Composable
fun StudyPlanScreen(
    repository: StudyPlanRepository,
    onCourseClick: (String) -> Unit,
) {
    val courses by repository.observeCourses().collectAsState(initial = emptyList())
    val earned = courses.filter { it.status == CourseStatus.FULFILLED }.sumOf { it.credits }
    val fulfilledCourses = courses.count { it.status == CourseStatus.FULFILLED }
    val totalCourses = courses.size
    val semesters = courses.groupBy { it.semester }.toSortedMap()
    var expandedSemesters by rememberSaveable { mutableStateOf(emptySet<Int>()) }

    LaunchedEffect(semesters.keys, expandedSemesters.isEmpty()) {
        if (expandedSemesters.isEmpty() && semesters.isNotEmpty()) {
            val currentSemester = courses
                .firstOrNull { it.status == CourseStatus.ENROLLED }
                ?.semester
                ?: semesters.keys.first()
            expandedSemesters = setOf(currentSemester)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        ScreenHeader(
            title = "Навчальний план",
            subtitle = "Повна програма · 6 семестрів",
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                SectionCard {
                    Text("Загальний прогрес", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(earned.toString() + " / 180", style = MaterialTheme.typography.headlineSmall)
                    LinearProgressIndicator(
                        progress = { (earned / 180f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        fulfilledCourses.toString() + " / " + totalCourses + " предметів",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            semesters.forEach { (semester, semesterCourses) ->
                item(key = "semester-" + semester) {
                    SemesterSection(
                        semester = semester,
                        courses = semesterCourses,
                        expanded = semester in expandedSemesters,
                        onExpandedChange = { expanded ->
                            expandedSemesters = if (expanded) {
                                expandedSemesters + semester
                            } else {
                                expandedSemesters - semester
                            }
                        },
                        onCourseClick = onCourseClick,
                    )
                }
            }
        }
    }
}
