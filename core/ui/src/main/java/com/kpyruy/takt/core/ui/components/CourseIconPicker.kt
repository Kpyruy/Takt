package com.kpyruy.takt.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseIconPicker(selectedKey: String?, onSelect: (String?) -> Unit, onDismiss: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("Усі") }
    val icons = CourseIcons.all.filter {
        (category == "Усі" || it.category == category) && (it.label.contains(query, true) || it.key.contains(query, true))
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth().fillMaxHeight(.88f).padding(horizontal = 12.dp),
            shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Іконка предмета", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, "Закрити іконки") }
            }
            Text("${CourseIcons.all.size} іконок · обери свою", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(vertical = 12.dp),
                placeholder = { Text("Пошук іконки") }, leadingIcon = { Icon(Icons.Outlined.Search, null) }, singleLine = true)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("Усі") + CourseIcons.all.map { it.category }.distinct()) { name ->
                    FilterChip(selected = name == category, onClick = { category = name }, label = { Text(name) })
                }
            }
            TextButton(onClick = { onSelect(null) }) { Text("Ініціали предмета") }
            if (icons.isEmpty()) Text("Іконок не знайдено", Modifier.padding(24.dp))
            LazyVerticalGrid(columns = GridCells.Adaptive(72.dp), modifier = Modifier.fillMaxWidth().weight(1f).testTag("course-icon-grid"),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(icons, key = { it.key }) { option ->
                    val chosen = option.key == selectedKey
                    Surface(onClick = { onSelect(option.key) },
                        modifier = Modifier.testTag("course-icon-${option.key}").semantics { selected = chosen; contentDescription = option.label },
                        shape = RoundedCornerShape(12.dp),
                        color = if (chosen) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, if (chosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
                        Column(Modifier.padding(8.dp).heightIn(min = 62.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(option.vector, null, Modifier.size(26.dp), tint = MaterialTheme.colorScheme.primary)
                            Text(option.label.substringBefore(' '), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        }
    }
}
