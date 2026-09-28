package com.kpyruy.takt.feature.subjects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.School
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.GradeItem
import com.kpyruy.takt.core.model.GradeItemType
import com.kpyruy.takt.core.ui.i18n.t
import com.kpyruy.takt.core.ui.theme.taktSubjectColor
import java.time.format.DateTimeFormatter

@Composable
fun GradeItemRow(item: GradeItem, onEdit: () -> Unit, onDelete: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    val accent = taktSubjectColor(item.courseId)
    val icon = when (item.type) {
        GradeItemType.TEST, GradeItemType.MIDTERM -> Icons.Outlined.Quiz
        GradeItemType.LAB -> Icons.Outlined.Science
        GradeItemType.EXAM -> Icons.Outlined.School
        else -> Icons.Outlined.Assignment
    }
    Surface(onClick = onEdit, modifier = Modifier.fillMaxWidth().testTag("course-work-${item.id}"), shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(shape = RoundedCornerShape(12.dp), color = accent.copy(alpha = 0.16f)) {
                    Icon(icon, contentDescription = null, tint = accent,
                        modifier = Modifier.padding(10.dp).size(22.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold, maxLines = 2)
                    Text(t(item.type.label), style = MaterialTheme.typography.labelMedium, color = accent)
                }
                IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = t("Дії оцінювання"))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text(t("Редагувати")) }, onClick = { menuOpen = false; onEdit() })
                    DropdownMenuItem(text = { Text(t("Видалити")) }, onClick = { menuOpen = false; onDelete() })
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)) {
                item.dueDate?.let {
                    AssessmentDetailChip(item.type.datePrefix() + it.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
                }
                item.durationMinutes?.let { AssessmentDetailChip(t("$it хв")) }
                if (item.requiredForExam) AssessmentDetailChip(t("Для допуску"))
                item.minimumPointsForExam?.let { AssessmentDetailChip(t("Мінімум ") + it.displayNumber() + t(" б.")) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text(when {
                    item.completed && item.requiredForExam && !item.meetsAdmissionRequirement -> t("Поріг не виконано")
                    item.completed -> t("Результат записано")
                    else -> t("Очікується")
                }, style = MaterialTheme.typography.labelMedium,
                    color = if (item.completed && item.requiredForExam && !item.meetsAdmissionRequirement)
                        MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                Surface(shape = RoundedCornerShape(9.dp), color = accent.copy(alpha = 0.16f)) {
                    Text(if (item.completed) "${item.earnedPoints.displayNumber()} / ${item.maxPoints.displayNumber()} ${t("б.")}"
                        else "${t("Макс.")} ${item.maxPoints.displayNumber()} ${t("б.")}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge, color = accent)
                }
            }
        }
    }
}

@Composable
private fun AssessmentDetailChip(label: String) {
    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface) {
        Text(label, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

internal fun GradeItemType.datePrefix(): String =
    if (this == GradeItemType.TEST || this == GradeItemType.MIDTERM) t("На ") else t("До ")

internal fun Double.displayNumber(): String =
    if (this % 1.0 == 0.0) toInt().toString() else "%.1f".format(this)
