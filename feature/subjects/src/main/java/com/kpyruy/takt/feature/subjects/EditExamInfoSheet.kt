package com.kpyruy.takt.feature.subjects

import com.kpyruy.takt.core.ui.i18n.t

import com.kpyruy.takt.core.ui.components.TaktFullSheet
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.kpyruy.takt.core.ui.components.TaktPrimaryButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.Alignment
import com.kpyruy.takt.core.ui.components.GlassSurface
import androidx.compose.material3.MaterialTheme
import com.kpyruy.takt.core.ui.components.TaktTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import com.kpyruy.takt.core.ui.components.TaktSegmentedTabs
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.ExamInfo
import com.kpyruy.takt.core.model.GradeItem
import com.kpyruy.takt.core.model.GradeItemType
import com.kpyruy.takt.core.ui.components.TaktDatePickerField
import com.kpyruy.takt.core.ui.components.TaktTimePickerField
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditExamInfoSheet(
    courseId: String,
    initial: ExamInfo?,
    gradeItems: List<GradeItem>,
    onDismiss: () -> Unit,
    onSave: (ExamInfo) -> Unit,
) {
    val examItems = gradeItems.filter { it.type == GradeItemType.EXAM }
    var date by remember(initial) { mutableStateOf(initial?.date) }
    var startTime by remember(initial) { mutableStateOf(initial?.startTime ?: LocalTime.of(9, 0)) }
    var endTime by remember(initial) { mutableStateOf(initial?.endTime ?: LocalTime.of(10, 30)) }
    var room by remember(initial) { mutableStateOf(initial?.room.orEmpty()) }
    var attempt by remember(initial) { mutableStateOf(initial?.attemptNumber ?: 1) }
    var notes by remember(initial) { mutableStateOf(initial?.notes.orEmpty()) }
    var linkedGradeItemId by remember(initial) { mutableStateOf(initial?.gradeItemId) }
    var error by remember { mutableStateOf<String?>(null) }

    TaktFullSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(t("Exam"), style = MaterialTheme.typography.headlineSmall)

            TaktDatePickerField(
                label = t("Date"),
                value = date,
                onValueChange = {
                    date = it
                    error = null
                },
                modifier = Modifier.fillMaxWidth(),
            )
            if (date != null) {
                TextButton(onClick = { date = null }) {
                    Text(t("The date is not yet known"))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TaktTimePickerField(
                        label = t("Start time"),
                        value = startTime,
                        onValueChange = {
                            startTime = it
                            error = null
                        },
                        modifier = Modifier.weight(1f),
                    )
                    TaktTimePickerField(
                        label = t("End"),
                        value = endTime,
                        onValueChange = {
                            endTime = it
                            error = null
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            TaktTextField(
                value = room,
                onValueChange = { room = it },
                label = { Text(t("Room")) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = { Icon(Icons.Outlined.LocationOn, null) },
            )

            Text(t("Attempt · maximum 3"), style = MaterialTheme.typography.labelLarge)
            TaktSegmentedTabs(listOf("1/3", "2/3", "3/3"), attempt - 1, { attempt = it + 1 })

            if (examItems.isNotEmpty()) {
                Text(t("Related result"), style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        ExamChoice(
                            selected = linkedGradeItemId == null,
                            onClick = { linkedGradeItemId = null },
                            label = { Text(t("Not selected")) },
                        )
                    }
                    items(examItems, key = { it.id }) { item ->
                        ExamChoice(
                            selected = linkedGradeItemId == item.id,
                            onClick = { linkedGradeItemId = item.id },
                            label = { Text(item.title, maxLines = 1) },
                        )
                    }
                }
            }

            TaktTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(t("Notes")) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            TaktPrimaryButton(
                onClick = {
                    if (date != null && endTime <= startTime) {
                        error = t("The end must be later than the beginning.")
                        return@TaktPrimaryButton
                    }
                    onSave(
                        ExamInfo(
                            courseId = courseId,
                            gradeItemId = linkedGradeItemId,
                            date = date,
                            startTime = if (date == null) null else startTime,
                            endTime = if (date == null) null else endTime,
                            room = room.trim().ifBlank { null },
                            attemptNumber = attempt,
                            maxAttempts = 3,
                            notes = notes.trim(),
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(t("Save"))
            }
        }
    }
}

@Composable
private fun ExamChoice(selected: Boolean, onClick: () -> Unit, label: @Composable () -> Unit) {
    GlassSurface(Modifier.heightIn(min = 48.dp).selectable(selected, role = Role.RadioButton, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        tint = if (selected) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Unspecified) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            label()
        }
    }
}
