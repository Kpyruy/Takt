package com.kpyruy.takt.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.Course
import com.kpyruy.takt.core.model.activeCourseChoices

@Composable
fun CourseLinkSelector(courses: List<Course>, selectedId: String?, onChange: (Course?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val activeCourses = courses.activeCourseChoices()
    Column {
        Text("Предмет", style = MaterialTheme.typography.titleSmall)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(courses.firstOrNull { it.id == selectedId }?.title ?: "Без прив’язки до предмета")
            }
            DropdownMenu(expanded, onDismissRequest = { expanded = false }, modifier = Modifier.heightIn(max = 320.dp)) {
                DropdownMenuItem(text = { Text("Без прив’язки") }, onClick = { onChange(null); expanded = false })
                if (activeCourses.isEmpty()) {
                    Text(
                        "Немає активних предметів",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                activeCourses.forEach { course ->
                    DropdownMenuItem(text = { Text(course.title) }, leadingIcon = { CourseAvatar(course) },
                        onClick = { onChange(course); expanded = false })
                }
            }
        }
    }
}
