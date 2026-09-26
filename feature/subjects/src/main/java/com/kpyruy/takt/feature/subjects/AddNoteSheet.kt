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
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.data.TaktDocumentStore
import com.kpyruy.takt.core.model.CourseNote
import com.kpyruy.takt.core.model.NoteAttachment
import java.util.UUID
import kotlinx.coroutines.launch

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
    var title by remember(initialNote?.id, initialTitle) {
        mutableStateOf(initialNote?.title ?: initialTitle)
    }
    var content by remember(initialNote?.id, initialContent) {
        mutableStateOf(initialNote?.content ?: initialContent)
    }
    var attachments by remember(initialNote?.id) { mutableStateOf(initialNote?.attachments.orEmpty()) }
    var attachmentError by remember { mutableStateOf<String?>(null) }
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

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (showHeading) {
            Text(
                if (initialNote == null) t("Нова нотатка") else t("Редагувати нотатку"),
                style = MaterialTheme.typography.headlineSmall,
            )
        }
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text(t("Заголовок")) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            label = { Text(t("Нотатка")) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 5,
        )
        attachments.forEach { attachment ->
            androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.AttachFile, contentDescription = null)
                Text(attachment.name, modifier = Modifier.weight(1f))
                IconButton(onClick = { attachments = attachments - attachment }) {
                    Icon(Icons.Default.Close, contentDescription = t("Прибрати вкладення"))
                }
            }
        }
        OutlinedButton(onClick = {
            if (documentStore.isConnected) filesPicker.launch(arrayOf("*/*")) else folderPicker.launch(null)
        }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.AttachFile, contentDescription = null)
            Text(t("Додати фото або файл"))
        }
        attachmentError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (copyingFiles) Text(t("Копіювання файлів…"), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(
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
