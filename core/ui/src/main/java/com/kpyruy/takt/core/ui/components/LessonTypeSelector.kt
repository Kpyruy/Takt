package com.kpyruy.takt.core.ui.components

import com.kpyruy.takt.core.ui.i18n.t

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.LessonType

@Composable
fun LessonTypeSelector(value: LessonType, onChange: (LessonType) -> Unit) {
    Column {
        Text(t("Тип заняття"), style = MaterialTheme.typography.titleSmall)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(LessonType.entries) { type ->
                FilterChip(selected = type == value, onClick = { onChange(type) }, label = { Text(t(type.label)) },
                    leadingIcon = if (type == LessonType.UNSPECIFIED) null else ({ LessonTypeIcon(type) }))
            }
        }
    }
}

@Composable
fun LessonTypeIcon(type: LessonType) {
    val vector = when (type) {
        LessonType.UNSPECIFIED -> return
        LessonType.LECTURE -> Icons.Outlined.School
        LessonType.SEMINAR -> Icons.Outlined.Forum
        LessonType.PRACTICE -> Icons.Outlined.EditNote
        LessonType.LAB -> Icons.Outlined.Science
    }
    Icon(vector, t(type.label), Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
}
