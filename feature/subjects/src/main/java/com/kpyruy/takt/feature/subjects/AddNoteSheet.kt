package com.kpyruy.takt.feature.subjects

import com.kpyruy.takt.core.ui.i18n.t

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.kpyruy.takt.core.ui.components.TaktFullSheet
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.kpyruy.takt.core.ui.components.TaktPrimaryButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import com.kpyruy.takt.core.ui.components.TaktTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.data.TaktDocumentStore
import com.kpyruy.takt.core.model.CourseNote
import com.kpyruy.takt.core.model.NoteAttachment
import java.util.UUID
import kotlinx.coroutines.launch
import androidx.compose.ui.window.Dialog
import com.kpyruy.takt.core.ui.components.GlassSurface
import com.kpyruy.takt.core.ui.components.glassSurface
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.filled.Description
import androidx.compose.ui.Alignment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNoteSheet(
    courseId: String,
    courseCode: String,
    documentStore: TaktDocumentStore,
    initialNote: CourseNote? = null,
    onDismiss: () -> Unit,
    onSave: (CourseNote) -> Unit,
) {
    TaktFullSheet(onDismissRequest = onDismiss) {
        AddNoteForm(
            courseId = courseId,
            courseCode = courseCode,
            documentStore = documentStore,
            initialNote = initialNote,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            onSave = onSave,
        )
    }
}

@Composable
fun AddNoteForm(
    courseId: String,
    courseCode: String,
    documentStore: TaktDocumentStore,
    initialNote: CourseNote? = null,
    initialTitle: String = "",
    initialContent: String = "",
    modifier: Modifier = Modifier,
    showHeading: Boolean = true,
    onSave: (CourseNote) -> Unit,
) {
    var title by rememberSaveable(initialNote?.id, initialTitle) {
        mutableStateOf(initialNote?.title ?: initialTitle)
    }
    var content by rememberSaveable(initialNote?.id, initialContent) {
        mutableStateOf(initialNote?.content ?: initialContent)
    }
    var attachments by rememberSaveable(initialNote?.id, stateSaver = noteAttachmentListSaver) { mutableStateOf(initialNote?.attachments.orEmpty()) }
    var attachmentError by rememberSaveable { mutableStateOf<String?>(null) }
    var showLinkDialog by rememberSaveable { mutableStateOf(false) }
    var linkUrl by rememberSaveable { mutableStateOf("") }
    var linkTitle by rememberSaveable { mutableStateOf("") }
    var linkError by rememberSaveable { mutableStateOf(false) }
    var copyingFiles by remember { mutableStateOf(false) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val filesPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            copyingFiles = true
            scope.launch {
                try {
                    uris.forEach { uri ->
                        runCatching {
                            val name = documentStore.sourceDisplayName(uri)
                            val mime = documentStore.sourceMimeType(uri)
                            NoteAttachment(name, documentStore.copyMaterial(courseCode, uri), mime)
                        }.onSuccess { attachment ->
                            attachments = attachments + attachment
                            attachmentError = null
                        }.onFailure { attachmentError = t("Не вдалося додати файл: ${it.message}") }
                    }
                } finally {
                    copyingFiles = false
                }
            }
        }
    }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) scope.launch {
            runCatching { documentStore.connect(uri) }
                .onSuccess { filesPicker.launch(arrayOf("*/*")) }
                .onFailure { attachmentError = t("Не вдалося підключити папку: ${it.message}") }
        }
    }

    if (showLinkDialog) Dialog(onDismissRequest = { showLinkDialog = false }) {
        GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(25.dp)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(t("Додати посилання"), style = MaterialTheme.typography.headlineSmall)
                TaktTextField(linkUrl, { linkUrl = it; linkError = false },
                    label = { Text(t("Адреса посилання")) }, singleLine = true,
                    isError = linkError, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    leadingIcon = { Icon(Icons.Default.Link, null) },
                    trailingIcon = { if (linkUrl.isNotBlank()) IconButton(onClick = { linkUrl = ""; linkError = false }) {
                        Icon(Icons.Default.Close, t("Очистити"))
                    } })
                TaktTextField(linkTitle, { linkTitle = it },
                    label = { Text(t("Назва (необов’язково)")) }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Description, null) },
                    trailingIcon = { if (linkTitle.isNotBlank()) IconButton(onClick = { linkTitle = "" }) {
                        Icon(Icons.Default.Close, t("Очистити"))
                    } })
                if (linkError) Text(t("Вкажи коректне посилання https:// або http://"),
                    color = MaterialTheme.colorScheme.error)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    GlassSurface(Modifier.weight(1f), shape = RoundedCornerShape(50)) {
                        TextButton(onClick = { showLinkDialog = false }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(t("Скасувати")) }
                    }
                    TaktPrimaryButton(onClick = {
                        runCatching { NoteAttachment.webLink(linkUrl, linkTitle) }
                            .onSuccess { attachment ->
                                attachments = attachments + attachment
                                linkUrl = ""; linkTitle = ""; linkError = false; showLinkDialog = false
                            }.onFailure { linkError = true }
                    }, modifier = Modifier.weight(1f)) { Text(t("Додати")) }
                }
            }
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (showHeading) {
            Text(
                if (initialNote == null) t("Додати нотатку") else t("Редагувати нотатку"),
                style = MaterialTheme.typography.headlineMedium,
            )
        }
        TaktTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text(t("Заголовок")) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        TaktTextField(
            value = content,
            onValueChange = { content = it },
            label = { Text(t("Нотатка")) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 5,
        )
        Text(t("Вкладення"), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        attachments.forEach { attachment ->
            Row(modifier = Modifier.fillMaxWidth().glassSurface(RoundedCornerShape(17.dp)).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(if (attachment.isWebLink) Icons.Default.Link else Icons.Default.AttachFile, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(9.dp))
                Text(attachment.name, modifier = Modifier.weight(1f))
                IconButton(onClick = { attachments = attachments - attachment }) {
                    Icon(Icons.Default.Close, contentDescription = t("Прибрати вкладення"))
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            GlassSurface(Modifier.weight(1f), shape = RoundedCornerShape(18.dp)) {
                TextButton(onClick = {
                    if (documentStore.isConnected) filesPicker.launch(arrayOf("*/*")) else folderPicker.launch(null)
                }, modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp)) {
                    Icon(Icons.Default.AttachFile, contentDescription = null)
                    Text(t("Додати файл"))
                }
            }
            GlassSurface(Modifier.weight(1f), shape = RoundedCornerShape(18.dp)) {
                TextButton(onClick = { showLinkDialog = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp)) {
                    Icon(Icons.Default.Link, contentDescription = null)
                    Text(t("Додати посилання"))
                }
            }
        }
        attachmentError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (copyingFiles) Text(t("Копіювання файлів…"), color = MaterialTheme.colorScheme.onSurfaceVariant)
        TaktPrimaryButton(
            onClick = {
                onSave(
                    CourseNote(
                        id = initialNote?.id ?: UUID.randomUUID().toString(),
                        courseId = courseId,
                        title = title.trim(),
                        content = content.trim(),
                        updatedAtEpochMillis = System.currentTimeMillis(),
                        attachments = attachments,
                    )
                )
            },
            enabled = !copyingFiles && title.isNotBlank() && (content.isNotBlank() || attachments.isNotEmpty()),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (initialNote == null) t("Зберегти") else t("Оновити"))
        }
    }
}

// Save completed attachment references only; file copying itself remains a live operation.
private val noteAttachmentListSaver = listSaver<List<NoteAttachment>, String>(
    save = { attachments -> attachments.flatMap { listOf(it.name, it.uri, it.mimeType) } },
    restore = { values -> values.chunked(3).map { NoteAttachment(it[0], it[1], it[2]) } },
)
