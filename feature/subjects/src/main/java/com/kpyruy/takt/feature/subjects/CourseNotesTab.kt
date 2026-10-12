package com.kpyruy.takt.feature.subjects

import com.kpyruy.takt.core.ui.i18n.t

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.kpyruy.takt.core.ui.components.GlassSurface
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment

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
        if (notes.isEmpty()) SectionCard {
            Text(t("Поки немає нотаток."), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        notes.forEach { note ->
            SectionCard {
                CourseNoteCard(
                    note = note,
                    documentStore = documentStore,
                    onEdit = { onEdit(note) },
                    onDelete = { onDelete(note) },
                )
            }
        }
        GlassSurface(Modifier.fillMaxWidth().clickable(onClick = onAddNote), shape = RoundedCornerShape(20.dp)) {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center) {
                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(t("Додати нотатку"), color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
