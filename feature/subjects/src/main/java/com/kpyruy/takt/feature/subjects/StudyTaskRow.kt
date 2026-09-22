package com.kpyruy.takt.feature.subjects

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.text.style.TextDecoration
import com.kpyruy.takt.core.model.StudyTask
import com.kpyruy.takt.core.ui.components.SwipeCompletionBox
import java.time.format.DateTimeFormatter

@Composable
fun StudyTaskRow(
    task: StudyTask,
    onCompletedChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val containerColor by animateColorAsState(
        targetValue = if (task.completed) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "task-completion-color",
    )

    SwipeCompletionBox(
        completed = task.completed,
        onCompletedChange = onCompletedChange,
    ) {
        Surface(
            color = containerColor,
            modifier = Modifier.animateContentSize(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = task.completed, onCheckedChange = onCompletedChange)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        task.title,
                        style = MaterialTheme.typography.titleMedium,
                        textDecoration = if (task.completed) TextDecoration.LineThrough else null,
                        maxLines = 2,
                    )
                    val supporting = buildList {
                        task.dueDate?.let {
                            add("до " + it.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
                        }
                        if (task.requiredForExam) add("потрібно для допуску")
                    }.joinToString(" · ")
                    if (supporting.isNotBlank()) {
                        Text(
                            supporting,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (task.requiredForExam && !task.completed) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                    task.description?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                        )
                    }
                }
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Дії завдання")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Видалити") },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}
