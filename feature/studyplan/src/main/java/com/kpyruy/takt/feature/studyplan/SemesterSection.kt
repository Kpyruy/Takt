package com.kpyruy.takt.feature.studyplan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.kpyruy.takt.core.model.Course
import com.kpyruy.takt.core.model.CourseStatus
import com.kpyruy.takt.core.ui.components.SectionCard
import com.kpyruy.takt.core.ui.components.StatusPill

@Composable
fun SemesterSection(
    semester: Int,
    courses: List<Course>,
) {
    val totalCredits = courses.sumOf { it.credits }
    val earnedCredits = courses.filter { it.status == CourseStatus.FULFILLED }.sumOf { it.credits }

    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("Семестр $semester", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "$earnedCredits / $totalCredits кредитів",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusPill(
                text = when {
                    courses.isNotEmpty() && courses.all { it.status == CourseStatus.FULFILLED } -> "Закрито"
                    courses.any { it.status == CourseStatus.ENROLLED } -> "Поточний"
                    else -> "План"
                }
            )
        }

        courses.forEachIndexed { index, course ->
            if (index > 0) HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(course.title, fontWeight = FontWeight.Medium)
                    Text(course.code, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("${course.credits} кр.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
