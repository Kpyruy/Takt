package com.kpyruy.takt.feature.subjects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.data.TaktDocumentStore
import com.kpyruy.takt.core.model.CourseNote
import com.kpyruy.takt.core.ui.components.TaktFullSheet
import com.kpyruy.takt.core.ui.i18n.t

/** Read-only note surface; editing continues through the existing editor and its drafts. */
@Composable
internal fun CourseNoteReaderSheet(
    note: CourseNote,
    documentStore: TaktDocumentStore,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val openAttachment = rememberNoteAttachmentOpener(documentStore)
    TaktFullSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.9f).testTag("note-reader")) {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(t("Note"), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                NoteActionsMenu(onEdit = onEdit, onDelete = onDelete, modifier = Modifier.testTag("note-reader-actions"))
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("note-reader-close")) {
                    Icon(Icons.Default.Close, contentDescription = t("Close"))
                }
            }
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp).padding(top = 12.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SelectionContainer {
                    Text(note.title, Modifier.testTag("note-reader-title"), style = MaterialTheme.typography.headlineSmall)
                }
                SmallText(noteUpdatedDate(note.updatedAtEpochMillis))
                if (note.content.isNotBlank()) SelectionContainer {
                    Text(note.content, Modifier.fillMaxWidth().testTag("note-reader-content"),
                        style = MaterialTheme.typography.bodyLarge)
                }
                if (note.attachments.isNotEmpty()) {
                    Text(t("Attachments"), style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    note.attachments.forEachIndexed { index, attachment ->
                        NoteAttachmentRow(attachment, onClick = { openAttachment(attachment) },
                            modifier = Modifier.testTag("note-reader-attachment-$index"))
                    }
                }
            }
        }
    }
}
