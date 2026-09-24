package com.kpyruy.takt.feature.subjects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.CourseNote
import com.kpyruy.takt.core.data.TaktDocumentStore
import com.kpyruy.takt.core.ui.components.SectionCard

@Composable
internal fun CourseNotesTab(
    notes: List<CourseNote>,
    documentStore: TaktDocumentStore,
    onEdit: (CourseNote) -> Unit,
    onDelete: (CourseNote) -> Unit,
    onAddNote: () -> Unit,
) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionCard {
            Text("Нотатки", style = MaterialTheme.typography.titleMedium)
            if (notes.isEmpty()) {
                Text("Поки немає нотаток.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                notes.forEachIndexed { index, note ->
                    if (index > 0) HorizontalDivider()
                    CourseNoteCard(
                        note = note,
                        documentStore = documentStore,
                        onEdit = { onEdit(note) },
                        onDelete = { onDelete(note) },
                    )
                }
            }
            OutlinedButton(onClick = onAddNote, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Додати нотатку")
            }
        }
    }
}
