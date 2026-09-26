package com.kpyruy.takt.feature.calendar

import com.kpyruy.takt.core.ui.i18n.t

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.StudyTask
import com.kpyruy.takt.core.ui.components.SwipeCompletionBox

@Composable
fun CalendarDeadlineRow(
    task: StudyTask,
    courseTitle: String,
    onCompletedChange: (Boolean) -> Unit,
) {
    SwipeCompletionBox(
        completed = task.completed,
        onCompletedChange = onCompletedChange,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .padding(vertical = 4.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (task.completed) TextDecoration.LineThrough else null,
                    maxLines = 2,
                )
                Text(
                    courseTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (task.requiredForExam && task.completed && !task.meetsAdmissionRequirement) {
                    Text(t("Поріг для допуску не виконано"), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error)
                }
            }
            Text(t("Дедлайн"), color = MaterialTheme.colorScheme.primary)
        }
    }
}
