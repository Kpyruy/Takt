package com.kpyruy.takt.feature.subjects

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.ExamMaterial
import com.kpyruy.takt.core.ui.components.SectionCard
import java.util.UUID

@Composable
internal fun ExamMaterialsSection(
    courseId: String,
    materials: List<ExamMaterial>,
    onAdd: (ExamMaterial) -> Unit,
    onDelete: (ExamMaterial) -> Unit,
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var uriText by remember { mutableStateOf("") }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            val materialTitle = title.trim().ifBlank {
                uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "Матеріал"
            }
            onAdd(
                ExamMaterial(
                    id = UUID.randomUUID().toString(),
                    courseId = courseId,
                    title = materialTitle,
                    uri = uri.toString(),
                )
            )
            title = ""
            uriText = ""
        }
    }

    SectionCard {
        Text("Матеріали", style = MaterialTheme.typography.titleMedium)

        if (materials.isEmpty()) {
            Text(
                "Додай конспект, файл або посилання для підготовки.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            materials.forEachIndexed { index, material ->
                if (index > 0) HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(material.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            material.uri,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                        )
                    }
                    IconButton(onClick = { onDelete(material) }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Видалити матеріал")
                    }
                }
            }
        }

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Назва матеріалу") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = uriText,
            onValueChange = { uriText = it },
            label = { Text("Посилання") },
            placeholder = { Text("https://…") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(
                onClick = { filePicker.launch(arrayOf("*/*")) },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Default.AttachFile, contentDescription = null)
                Text("Файл")
            }
            Button(
                onClick = {
                    val trimmedUri = uriText.trim()
                    if (trimmedUri.isNotBlank()) {
                        onAdd(
                            ExamMaterial(
                                id = UUID.randomUUID().toString(),
                                courseId = courseId,
                                title = title.trim().ifBlank { "Посилання" },
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
                Text("Додати")
            }
        }
    }
}
