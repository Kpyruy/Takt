package com.kpyruy.takt.feature.home

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.StudyTask
import com.kpyruy.takt.core.ui.components.SwipeCompletionBox
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HomeTaskRow(task: StudyTask, courseTitle: String, onCompletedChange: (Boolean) -> Unit) {
    val largeText = LocalDensity.current.fontScale > 1.15f
    val dueLabel: @Composable () -> Unit = {
        task.dueDate?.let { date ->
            val today = LocalDate.now()
            Text(when (date) { today -> "Сьогодні"; today.plusDays(1) -> "Завтра"; else -> date.format(DateTimeFormatter.ofPattern("dd.MM")) },
                color = if (date <= today) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall)
        }
    }
    SwipeCompletionBox(completed = task.completed, onCompletedChange = onCompletedChange) {
        Row(Modifier.fillMaxWidth().animateContentSize().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Checkbox(checked = task.completed, onCheckedChange = onCompletedChange,
                modifier = Modifier.semantics { contentDescription = task.title })
            Column(Modifier.weight(1f)) {
                Text(task.title, style = MaterialTheme.typography.titleSmall,
                    textDecoration = if (task.completed) TextDecoration.LineThrough else null)
                val minimum = task.minimumPointsForExam?.let { " · мін. ${it.pointText()} б." }.orEmpty()
                Text(courseTitle + if (task.requiredForExam) " · Для допуску$minimum" else "",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                task.maxPoints?.let { maximum ->
                    Text("${task.earnedPoints?.pointText() ?: "—"} / ${maximum.pointText()} б.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (task.requiredForExam && task.completed && !task.meetsAdmissionRequirement) {
                    Text("Поріг для допуску не виконано", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error)
                }
                if (largeText) dueLabel()
            }
            if (!largeText) dueLabel()
        }
    }
}
