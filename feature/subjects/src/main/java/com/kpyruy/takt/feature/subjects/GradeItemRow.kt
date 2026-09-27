package com.kpyruy.takt.feature.subjects

import com.kpyruy.takt.core.ui.i18n.t

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.GradeItem
import com.kpyruy.takt.core.model.GradeItemType
import java.time.format.DateTimeFormatter

@Composable
fun GradeItemRow(
    item: GradeItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 2)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)) {
                buildList {
                    add(t(item.type.label))
                    item.durationMinutes?.let { add(t("$it хв")) }
                    item.dueDate?.let {
                        add(t("до ") + it.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
                    }
                    if (item.requiredForExam) add(t("для допуску"))
                    item.minimumPointsForExam?.let { add(t("мін. ${it.displayNumber()} б.")) }
                    if (item.requiredForExam && item.completed && !item.meetsAdmissionRequirement) add(t("поріг не виконано"))
                }.forEach { detail ->
                    Text(detail, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(if (item.completed) t("завершено") else t("очікується"),
                style = MaterialTheme.typography.labelSmall,
                color = if (item.completed) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
            if (item.completed) {
                item.earnedPoints.displayNumber() + " / " + item.maxPoints.displayNumber()
            } else {
                t("до ") + item.maxPoints.displayNumber()
            },
            style = MaterialTheme.typography.labelLarge,
            )
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = t("Дії оцінювання"))
            }
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(t("Редагувати")) },
                onClick = {
                    menuOpen = false
                    onEdit()
                },
            )
            DropdownMenuItem(
                text = { Text(t("Видалити")) },
                onClick = {
                    menuOpen = false
                    onDelete()
                },
            )
        }
    }
}

internal fun Double.displayNumber(): String =
    if (this % 1.0 == 0.0) toInt().toString() else "%.1f".format(this)
