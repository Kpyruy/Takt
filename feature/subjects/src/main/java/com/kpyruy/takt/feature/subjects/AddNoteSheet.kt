package com.kpyruy.takt.feature.subjects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.CourseNote
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNoteSheet(
    courseId: String,
    initialNote: CourseNote? = null,
    onDismiss: () -> Unit,
    onSave: (CourseNote) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        AddNoteForm(
            courseId = courseId,
            initialNote = initialNote,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            onSave = onSave,
        )
    }
}

@Composable
fun AddNoteForm(
    courseId: String,
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

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (showHeading) {
            Text(
                if (initialNote == null) "Нова нотатка" else "Редагувати нотатку",
                style = MaterialTheme.typography.headlineSmall,
            )
        }
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Заголовок") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            label = { Text("Нотатка") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 5,
        )
        Button(
            onClick = {
                onSave(
                    CourseNote(
                        id = initialNote?.id ?: UUID.randomUUID().toString(),
                        courseId = courseId,
                        title = title.trim(),
                        content = content.trim(),
                        updatedAtEpochMillis = System.currentTimeMillis(),
                    )
                )
            },
            enabled = title.isNotBlank() && content.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (initialNote == null) "Зберегти" else "Оновити")
        }
    }
}
