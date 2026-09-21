package com.kpyruy.takt.feature.calendar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.kpyruy.takt.core.model.StudyTask

@Composable
fun CalendarDeadlineRow(
    task: StudyTask,
    courseTitle: String,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(task.title, fontWeight = FontWeight.SemiBold)
            Text(
                courseTitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text("Дедлайн", color = MaterialTheme.colorScheme.primary)
    }
}
