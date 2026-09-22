package com.kpyruy.takt.feature.subjects

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.kpyruy.takt.core.model.GradeItem
import com.kpyruy.takt.core.model.GradeItemType
import java.time.format.DateTimeFormatter

@Composable
fun GradeItemRow(
    item: GradeItem,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 2)
            Text(
                buildList {
                    add(item.type.label())
                    item.dueDate?.let {
                        add("до " + it.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
                    }
                    if (item.requiredForExam) add("для допуску")
                    add(if (item.completed) "завершено" else "очікується")
                }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            if (item.completed) {
                item.earnedPoints.compact() + " / " + item.maxPoints.compact()
            } else {
                "до " + item.maxPoints.compact()
            },
            style = MaterialTheme.typography.labelLarge,
        )
        IconButton(onClick = { menuOpen = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Дії оцінювання")
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

internal fun GradeItemType.label(): String = when (this) {
    GradeItemType.TEST -> "Тест"
    GradeItemType.MIDTERM -> "Модуль / проміжний тест"
    GradeItemType.LAB -> "Лабораторна"
    GradeItemType.SEMINAR -> "Семінар"
    GradeItemType.HOMEWORK -> "Домашня робота"
    GradeItemType.PROJECT -> "Проєкт"
    GradeItemType.ORAL -> "Усна відповідь"
    GradeItemType.EXAM -> "Екзамен"
    GradeItemType.OTHER -> "Інше"
}

internal fun Double.compact(): String =
    if (this % 1.0 == 0.0) toInt().toString() else "%.1f".format(this)
