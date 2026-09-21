package com.kpyruy.takt.feature.subjects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.data.StudyPlanRepository
import com.kpyruy.takt.core.model.CourseStatus
import com.kpyruy.takt.core.ui.components.ScreenHeader

@Composable
fun SubjectsScreen(repository: StudyPlanRepository) {
    val courses by repository.observeSemester(3).collectAsState(initial = emptyList())
    val active = courses.count { it.status == CourseStatus.ENROLLED }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        ScreenHeader(
            title = "Предмети",
            subtitle = "3 семестр · $active активних",
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(courses, key = { it.id }) { course ->
                SubjectCard(course)
            }
        }
    }
}
