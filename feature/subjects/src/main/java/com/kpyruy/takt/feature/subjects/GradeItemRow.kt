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
import java.time.LocalDate
import androidx.compose.ui.graphics.compositeOver
import com.kpyruy.takt.core.ui.theme.readableAccentColor
import com.kpyruy.takt.core.ui.components.GlassSurface
import com.kpyruy.takt.core.ui.components.glassSurface
import androidx.compose.foundation.clickable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline

@Composable
fun GradeItemRow(item: GradeItem, onEdit: () -> Unit, onDelete: () -> Unit, today: LocalDate = LocalDate.now()) {
    var menuOpen by remember { mutableStateOf(false) }
    val accent = taktSubjectColor(item.courseId)
    val overdue = item.isOverdue(today)
    val background = MaterialTheme.colorScheme.surfaceContainerHigh
    val accentText = readableAccentColor(accent, background)
    val icon = when (item.type) {
        GradeItemType.TEST, GradeItemType.MIDTERM -> Icons.Outlined.Quiz
        GradeItemType.LAB -> Icons.Outlined.Science
        GradeItemType.EXAM -> Icons.Outlined.School
        else -> Icons.Outlined.Assignment
    }
    GlassSurface(modifier = Modifier.fillMaxWidth().testTag("course-work-${item.id}").clickable(onClick = onEdit),
        shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(shape = RoundedCornerShape(12.dp), color = accent.copy(alpha = 0.16f)) {
                    Icon(icon, contentDescription = null, tint = accentText,
                        modifier = Modifier.padding(10.dp).size(22.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold, maxLines = 2, color = MaterialTheme.colorScheme.onSurface)
                    Text(t(item.type.label), style = MaterialTheme.typography.labelMedium, color = accentText)
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
                    AssessmentDetailChip(item.type.datePrefix() + it.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")), Icons.Outlined.CalendarMonth)
                }
                item.durationMinutes?.let { AssessmentDetailChip(t("$it хв"), Icons.Outlined.Schedule) }
                AssessmentDetailChip(if (item.contributesToGrade) t("Входить у підсумок") else t("Не входить у підсумок"), Icons.Outlined.Description)
                if (item.requiredForExam) AssessmentDetailChip(t("Для допуску"), Icons.Outlined.Lock)
                item.minimumPointsForExam?.let { AssessmentDetailChip(t("Мінімум ") + it.displayNumber() + t(" б."), Icons.Outlined.BarChart) }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Icon(if (item.completed) Icons.Outlined.CheckCircle else if (overdue) Icons.Outlined.ErrorOutline else Icons.Outlined.Schedule,
                    null, Modifier.size(18.dp), tint = if (overdue) MaterialTheme.colorScheme.error else accentText)
                Text(when {
                    item.isAwaitingResult -> t("Очікую оцінку")
                    overdue -> overdueLabel(item.dueDate!!, today)
                    item.completed && item.requiredForExam && !item.meetsAdmissionRequirement -> t("Поріг не виконано")
                    item.completed -> t("Результат записано")
                    else -> t("Очікується")
                }, modifier = Modifier.weight(1f).padding(start = 7.dp), style = MaterialTheme.typography.labelMedium,
                    color = if (overdue || (item.completed && item.requiredForExam && !item.meetsAdmissionRequirement))
                        MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                Text(if (item.completed) "${item.earnedPoints.displayNumber()} / ${item.maxPoints.displayNumber()} ${t("б.")}"
                        else "${t("Макс.")} ${item.maxPoints.displayNumber()} ${t("б.")}",
                        style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
internal fun AssessmentDetailChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(Modifier.glassSurface(RoundedCornerShape(50)).padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Icon(icon, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

internal fun GradeItemType.datePrefix(): String =
    if (this == GradeItemType.TEST || this == GradeItemType.MIDTERM) t("На ") else t("До ")

internal fun Double.displayNumber(): String =
    if (this % 1.0 == 0.0) toInt().toString() else "%.1f".format(this)
