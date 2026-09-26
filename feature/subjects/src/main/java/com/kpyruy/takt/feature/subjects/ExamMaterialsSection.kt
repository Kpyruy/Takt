package com.kpyruy.takt.feature.subjects

import com.kpyruy.takt.core.ui.i18n.t

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.ExamMaterial
import com.kpyruy.takt.core.data.TaktDocumentStore
import java.util.UUID
import kotlinx.coroutines.launch

@Composable
internal fun ExamMaterialsSection(
    courseId: String,
    courseCode: String,
    documentStore: TaktDocumentStore,
    materials: List<ExamMaterial>,
    onAdd: (ExamMaterial) -> Unit,
    onDelete: (ExamMaterial) -> Unit,
) {
    val context = LocalContext.current
    var adding by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var uriText by remember { mutableStateOf("") }
    var fileError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) scope.launch {
            val materialTitle = title.trim().ifBlank {
                documentStore.sourceDisplayName(uri)
            }
            runCatching { documentStore.copyMaterial(courseCode, uri) }
                .onSuccess { stored ->
                    onAdd(ExamMaterial(UUID.randomUUID().toString(), courseId, materialTitle, stored))
                    title = ""
                    uriText = ""
                    fileError = null
                }
                .onFailure { fileError = t("Не вдалося додати файл: ${it.message}") }
        }
    }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) scope.launch {
            runCatching { documentStore.connect(uri) }
                .onSuccess { filePicker.launch(arrayOf("*/*")) }
                .onFailure { fileError = t("Не вдалося підключити папку: ${it.message}") }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(t("Підготовка"), style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = { adding = !adding }) { Text(if (adding) t("Скасувати") else t("Додати матеріал")) }
        }

        if (materials.isEmpty()) {
            Text(
                t("Додай конспект, файл або посилання для підготовки."),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            materials.forEachIndexed { index, material ->
                if (index > 0) HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.AttachFile, null, Modifier.padding(end = 12.dp).size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f).clickable {
                        scope.launch {
                            val resolved = documentStore.resolve(material.uri)
                            if (resolved == null) Toast.makeText(context, t("Файл недоступний"), Toast.LENGTH_SHORT).show()
                            else runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, resolved).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                            }.onFailure { Toast.makeText(context, t("Немає застосунку для відкриття матеріалу"), Toast.LENGTH_SHORT).show() }
                        }
                    }) {
                        Text(material.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (material.uri.startsWith("takt://")) documentStore.displayName(material.uri) else material.uri,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                        )
                    }
                    IconButton(onClick = { onDelete(material) }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = t("Видалити матеріал"))
                    }
                }
            }
        }

        if (adding) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(t("Назва матеріалу")) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = uriText,
                onValueChange = { uriText = it },
                label = { Text(t("Посилання")) },
                placeholder = { Text("https://…") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        if (documentStore.isConnected) filePicker.launch(arrayOf("*/*")) else folderPicker.launch(null)
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.AttachFile, contentDescription = null)
                    Text(t("Файл"))
                }
                Button(
                    onClick = {
                        val trimmedUri = uriText.trim()
                        if (trimmedUri.isNotBlank()) {
                            onAdd(
                                ExamMaterial(
                                    id = UUID.randomUUID().toString(),
                                    courseId = courseId,
                                    title = title.trim().ifBlank { t("Посилання") },
                                    uri = trimmedUri,
                                )
                            )
                            title = ""
                            uriText = ""
                        }
                    },
                    enabled = uriText.isNotBlank(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(t("Додати"))
                }
            }
            fileError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}
