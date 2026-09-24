package com.kpyruy.takt.feature.subjects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.GradeItem
import com.kpyruy.takt.core.model.GradeItemType
import com.kpyruy.takt.core.ui.theme.taktSubjectColor
import java.time.format.DateTimeFormatter

@Composable
internal fun CourseWorkGradeRow(item: GradeItem, onEdit: () -> Unit, onDelete: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    val icon = when (item.type) {
        GradeItemType.TEST, GradeItemType.MIDTERM -> Icons.Outlined.Quiz
        GradeItemType.LAB -> Icons.Outlined.Science
        else -> Icons.Outlined.Assignment
    }
    val statusColor = if (item.completed && item.requiredForExam && !item.meetsAdmissionRequirement) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(onClick = onEdit, modifier = Modifier.fillMaxWidth().testTag("course-work-${item.id}")) {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, contentDescription = item.type.label, tint = taktSubjectColor(item.courseId))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                Text(buildList {
                    add(item.type.label)
                    item.dueDate?.let { add("до ${it.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}") }
                    if (item.requiredForExam) add("для допуску")
                    item.minimumPointsForExam?.let { add("мін. ${it.displayNumber()} б.") }
                }.joinToString(" · "), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    when {
                        item.completed && item.requiredForExam && !item.meetsAdmissionRequirement -> "Поріг не виконано"
                        item.completed -> "Виконано · ${item.earnedPoints.displayNumber()} / ${item.maxPoints.displayNumber()} б."
                        else -> "Очікується · до ${item.maxPoints.displayNumber()} б."
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                )
            }
            if (item.completed) Icon(Icons.Outlined.CheckCircle, contentDescription = "Виконано", tint = statusColor)
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Дії роботи")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text("Редагувати") }, onClick = { menuOpen = false; onEdit() })
                DropdownMenuItem(text = { Text("Видалити") }, onClick = { menuOpen = false; onDelete() })
            }
        }
    }
}
