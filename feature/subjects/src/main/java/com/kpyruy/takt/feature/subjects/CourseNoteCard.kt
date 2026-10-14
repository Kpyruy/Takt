package com.kpyruy.takt.feature.subjects

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.data.TaktDocumentStore
import com.kpyruy.takt.core.model.CourseNote
import com.kpyruy.takt.core.model.NoteAttachment
import com.kpyruy.takt.core.ui.components.TaktChoiceMenu
import com.kpyruy.takt.core.ui.components.TaktChoiceMenuItem
import com.kpyruy.takt.core.ui.components.glassSurface
import com.kpyruy.takt.core.ui.i18n.t
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun CourseNoteCard(
    note: CourseNote,
    documentStore: TaktDocumentStore,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var readerOpen by rememberSaveable(note.id) { mutableStateOf(false) }
    val openAttachment = rememberNoteAttachmentOpener(documentStore)
    val updatedDate = remember(note.updatedAtEpochMillis) { noteUpdatedDate(note.updatedAtEpochMillis) }
    Column(
        Modifier.fillMaxWidth().testTag("note-preview-${note.id}")
            .clickable(onClickLabel = t("Відкрити нотатку")) { readerOpen = true }.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(40.dp).testTag("note-preview-icon-${note.id}")
                    .glassSurface(RoundedCornerShape(13.dp), MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Description, null, Modifier.size(23.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Text(
                note.title, Modifier.weight(1f).testTag("note-preview-title-${note.id}"),
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                updatedDate, Modifier.testTag("note-preview-date-${note.id}"),
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            NoteActionsMenu(onEdit = onEdit, onDelete = onDelete)
        }
        if (note.content.isNotBlank()) Text(
            note.content.trim(), Modifier.fillMaxWidth().testTag("note-preview-content-${note.id}"),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3, overflow = TextOverflow.Ellipsis,
        )
        note.attachments.forEach { attachment ->
            NoteAttachmentRow(attachment, onClick = { openAttachment(attachment) })
        }
    }
    if (readerOpen) CourseNoteReaderSheet(
        note = note,
        documentStore = documentStore,
        onDismiss = { readerOpen = false },
        onEdit = { readerOpen = false; onEdit() },
        onDelete = { readerOpen = false; onDelete() },
    )
}

@Composable
internal fun NoteActionsMenu(onEdit: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    var menuOpen by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { menuOpen = true }, modifier = modifier) {
            Icon(Icons.Default.MoreVert, contentDescription = t("Дії нотатки"))
        }
        TaktChoiceMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            TaktChoiceMenuItem(text = { Text(t("Редагувати")) }, onClick = { menuOpen = false; onEdit() })
            TaktChoiceMenuItem(text = { Text(t("Видалити")) }, onClick = { menuOpen = false; onDelete() })
        }
    }
}

@Composable
internal fun NoteAttachmentRow(attachment: NoteAttachment, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().glassSurface(RoundedCornerShape(15.dp)).clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(if (attachment.isWebLink) Icons.Default.Link else Icons.Default.AttachFile, contentDescription = null,
            tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f)) {
            Text(attachment.name, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (attachment.isWebLink) {
                val host = remember(attachment.uri) { runCatching { java.net.URI(attachment.uri.trim()).host }.getOrNull() }
                if (host != null) SmallText(host)
            }
        }
        Icon(Icons.Default.OpenInNew, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

internal fun noteUpdatedDate(epochMillis: Long): String = Instant.ofEpochMilli(epochMillis)
    .atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
