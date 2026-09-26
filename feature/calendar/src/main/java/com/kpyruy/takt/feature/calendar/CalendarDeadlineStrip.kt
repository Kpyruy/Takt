package com.kpyruy.takt.feature.calendar

import com.kpyruy.takt.core.ui.i18n.t

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.StudyTask

/** Tasks have date-only deadlines. A clock time must never be inferred. */
@Composable
internal fun CalendarDeadlineStrip(task: StudyTask, onCompletedChange: (Boolean) -> Unit) {
    val dark = MaterialTheme.colorScheme.surface.luminance() < .5f
    val ink = if (dark) Color(0xFFEFC78F) else Color(0xFF986126)
    Surface(shape = RoundedCornerShape(13.dp), color = if (dark) Color(0xFF392F26) else Color(0xFFFFF2DF),
        contentColor = ink) {
        Row(Modifier.fillMaxWidth().padding(start = 13.dp, end = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(task.title, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
            Text(if (task.requiredForExam && task.completed && !task.meetsAdmissionRequirement) t("поріг не виконано") else t("дедлайн"),
                style = MaterialTheme.typography.bodySmall)
            Checkbox(checked = task.completed, onCheckedChange = onCompletedChange,
                modifier = Modifier.semantics { contentDescription = t("Завершити: ${task.title}") })
        }
    }
}
