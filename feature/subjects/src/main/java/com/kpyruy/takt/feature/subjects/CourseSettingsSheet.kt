package com.kpyruy.takt.feature.subjects

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.*
import com.kpyruy.takt.core.ui.components.CourseAvatar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun CourseSettingsSheet(
    course: Course,
    onDismiss: () -> Unit,
    onIcon: () -> Unit,
    onStatus: (CourseStatus) -> Unit,
    onGrading: (CourseGradingType) -> Unit,
    onResult: (PassFailResult?) -> Unit,
    onScale: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Налаштування", style = MaterialTheme.typography.headlineSmall)
                    Text(course.title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, "Закрити налаштування предмета") }
            }
            Surface(onClick = onIcon, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CourseAvatar(course)
                    Column(Modifier.weight(1f)) {
                        Text("Іконка предмета", style = MaterialTheme.typography.titleSmall)
                        Text("Обери свій символ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column {
                SettingsLabel("СТАТУС ПРЕДМЕТА")
                FlowRow(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CourseStatus.entries.forEach { status ->
                        val selected = course.status == status
                        FilterChip(selected = selected, onClick = { onStatus(status) },
                            modifier = Modifier.testTag("settings-status-${status.name}"),
                            leadingIcon = { Icon(if (selected) Icons.Outlined.CheckCircle else statusIcon(status), null, Modifier.size(17.dp)) },
                            label = { Text(statusText(status)) }, shape = RoundedCornerShape(12.dp))
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SettingsLabel("ОЦІНЮВАННЯ")
                Column(Modifier.selectableGroup()) {
                    CourseGradingType.entries.forEach { type ->
                        val selected = course.gradingType == type
                        Surface(shape = RoundedCornerShape(12.dp), color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface) {
                            Row(Modifier.fillMaxWidth().selectable(selected, role = Role.RadioButton, onClick = { onGrading(type) }).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(type.label(), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                RadioButton(selected = selected, onClick = null, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
            if (course.gradingType == CourseGradingType.PASS_FAIL) {
                Column {
                    SettingsLabel("РЕЗУЛЬТАТ")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(null, PassFailResult.PASSED, PassFailResult.FAILED).forEach { result ->
                            FilterChip(selected = course.passFailResult == result, onClick = { onResult(result) }, label = {
                                Text(when(result) { null -> "Не вказано"; PassFailResult.PASSED -> "Зараховано"; PassFailResult.FAILED -> "Не зараховано" })
                            })
                        }
                    }
                }
            } else {
                OutlinedButton(onClick = onScale, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Outlined.Tune, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Шкала оцінювання")
                }
            }
        }
    }
}

@Composable private fun SettingsLabel(text: String) {
    Text(text, Modifier.padding(bottom = 6.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
private fun statusText(status: CourseStatus) = when(status) {
    CourseStatus.FULFILLED -> "Здано"
    CourseStatus.ENROLLED -> "Активний"
    CourseStatus.PLANNED -> "Заплановано"
    CourseStatus.NOT_ENROLLED -> "Не записаний"
    CourseStatus.NOT_NEEDED -> "Не потрібний"
}
private fun statusIcon(status: CourseStatus): ImageVector = when(status) {
    CourseStatus.FULFILLED -> Icons.Outlined.CheckCircle
    CourseStatus.ENROLLED -> Icons.Outlined.Schedule
    CourseStatus.PLANNED -> Icons.Outlined.Event
    CourseStatus.NOT_ENROLLED -> Icons.Outlined.RadioButtonUnchecked
    CourseStatus.NOT_NEEDED -> Icons.Outlined.RemoveCircleOutline
}
