package com.kpyruy.takt.feature.subjects

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.AttachFile
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.kpyruy.takt.core.data.TaktDocumentStore
import com.kpyruy.takt.core.model.CourseNote
import kotlinx.coroutines.launch

@Composable
fun CourseNoteCard(
    note: CourseNote,
    documentStore: TaktDocumentStore,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(note.title, style = MaterialTheme.typography.titleMedium, maxLines = 2)
            Text(note.content, color = MaterialTheme.colorScheme.onSurfaceVariant)
            note.attachments.forEach { attachment ->
                Row(modifier = Modifier.fillMaxWidth().clickable {
                    scope.launch {
                        val uri = documentStore.resolve(attachment.uri)
                        if (uri == null) Toast.makeText(context, "Файл недоступний", Toast.LENGTH_SHORT).show()
                        else runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, attachment.mimeType)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            })
                        }.onFailure { Toast.makeText(context, "Немає застосунку для відкриття файла", Toast.LENGTH_SHORT).show() }
                    }
                }) {
                    Icon(Icons.Default.AttachFile, contentDescription = null)
                    Text(attachment.name, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        IconButton(onClick = { menuOpen = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Дії нотатки")
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text("Редагувати") },
                onClick = {
                    menuOpen = false
                    onEdit()
                },
            )
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
