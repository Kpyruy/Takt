package com.kpyruy.takt.core.ui.components

import com.kpyruy.takt.core.ui.i18n.t

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.Course
import com.kpyruy.takt.core.model.activeCourseChoices

@Composable
fun CourseLinkSelector(courses: List<Course>, selectedId: String?, onChange: (Course?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val activeCourses = courses.activeCourseChoices()
    val selectedCourse = courses.firstOrNull { it.id == selectedId }
    val shape = RoundedCornerShape(20.dp)
    val interaction = remember { MutableInteractionSource() }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(t("Предмет"), style = MaterialTheme.typography.titleSmall)
        Box(Modifier.fillMaxWidth()) {
            TaktSurface(modifier = Modifier.fillMaxWidth().taktPressFeedback(interaction).clip(shape)
                .clickable(interactionSource = interaction, indication = null, role = Role.Button,
                    onClick = { expanded = true }).testTag("course-link-trigger"), shape = shape) {
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    selectedCourse?.let { CourseAvatar(it) }
                    Text(selectedCourse?.title ?: t("Без прив’язки до предмета"), Modifier.weight(1f))
                    Icon(Icons.Outlined.ExpandMore, contentDescription = null)
                }
            }
            TaktChoiceMenu(expanded, onDismissRequest = { expanded = false }) {
                TaktChoiceMenuItem(text = { Text(t("Без прив’язки")) },
                    trailingIcon = { if (selectedId == null) Icon(Icons.Outlined.Check, null) },
                    onClick = { onChange(null); expanded = false })
                if (activeCourses.isEmpty()) {
                    Text(
                        t("Немає активних предметів"),
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                activeCourses.forEach { course ->
                    TaktChoiceMenuItem(text = { Text(course.title) }, leadingIcon = { CourseAvatar(course) },
                        trailingIcon = { if (course.id == selectedId) Icon(Icons.Outlined.Check, null) },
                        onClick = { onChange(course); expanded = false })
                }
            }
        }
    }
}
