package com.kpyruy.takt.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.Course
import com.kpyruy.takt.core.model.CourseStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectCourseSheet(
    courses: List<Course>,
    onDismiss: () -> Unit,
    onSelected: (Course) -> Unit,
) {
    val ordered = courses.sortedWith(
        compareBy<Course> { it.status != CourseStatus.ENROLLED }
            .thenBy { it.semester }
            .thenBy { it.title }
    )

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Text("Оберіть предмет", style = MaterialTheme.typography.headlineSmall)
            ordered.forEachIndexed { index, course ->
                if (index > 0) HorizontalDivider()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .clickable { onSelected(course) }
                        .padding(vertical = 12.dp),
                ) {
                    Text(course.title, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                    Text(
                        "${course.code} · ${course.credits} кр. · семестр ${course.semester}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
