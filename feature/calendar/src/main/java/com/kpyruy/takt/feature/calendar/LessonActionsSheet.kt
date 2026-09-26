package com.kpyruy.takt.feature.calendar

import com.kpyruy.takt.core.ui.i18n.t

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
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.*
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.kpyruy.takt.core.ui.i18n.TaktI18n

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonActionsSheet(
    event: ResolvedScheduleEvent,
    isRecurringRule: Boolean,
    onDismiss: () -> Unit,
    onToggleAbsence: () -> Unit,
    onCancelOccurrence: () -> Unit,
    onMoveOccurrence: () -> Unit,
    onRestoreOccurrence: () -> Unit,
    onEditRule: () -> Unit,
    onDeleteRule: () -> Unit,
    onEditOneOff: () -> Unit,
    onDeleteOneOff: () -> Unit,
) {
    val actions = ScheduleEventActions.forEvent(event, isRecurringRule)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(event.lessonType.takeUnless { it == LessonType.UNSPECIFIED }?.label?.let(::t) ?: t("Пара"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(event.title, Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
                IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, t("Закрити дії пари")) }
            }
            Text(listOfNotNull(event.date.format(DateTimeFormatter.ofPattern("d MMMM", TaktI18n.locale)),
                "${event.startTime}–${event.endTime}", event.room).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            if (event.status != ScheduleEventStatus.CANCELLED || event.isAbsent) {
                Surface(shape = RoundedCornerShape(14.dp), color = if (event.isAbsent) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerLow) {
                    ActionRow(if (event.isAbsent) t("Зняти позначку пропуску") else t("Позначити пропуск"), Icons.Outlined.PersonOff, onToggleAbsence)
                }
            }
            if (actions.canMoveOccurrence) ActionRow(t("Перенести цю пару"), Icons.Outlined.EventRepeat, onMoveOccurrence)
            if (actions.canCancelOccurrence) ActionRow(t("Скасувати тільки цю пару"), Icons.Outlined.EventBusy, onCancelOccurrence)
            if (actions.canRestoreOccurrence) ActionRow(t("Повернути початкову пару"), Icons.Outlined.Restore, onRestoreOccurrence)
            if (actions.canEditRecurringRule || actions.canEditOneOff) HorizontalDivider(Modifier.padding(vertical = 6.dp))
            if (actions.canEditRecurringRule) ActionRow(t("Редагувати"), Icons.Outlined.Edit, onEditRule)
            if (actions.canEditOneOff) ActionRow(t("Редагувати разову подію"), Icons.Outlined.Edit, onEditOneOff)
            if (actions.canDeleteRecurringRule) ActionRow(t("Видалити з розкладу"), Icons.Outlined.DeleteOutline, onDeleteRule, destructive = true)
            if (actions.canDeleteOneOff) ActionRow(t("Видалити разову подію"), Icons.Outlined.DeleteOutline, onDeleteOneOff, destructive = true)
        }
    }
}

@Composable private fun ActionRow(label: String, icon: ImageVector, onClick: () -> Unit, destructive: Boolean = false) {
    Surface(onClick = onClick, shape = RoundedCornerShape(12.dp), color = androidx.compose.ui.graphics.Color.Transparent) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            val tint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            Icon(icon, null, Modifier.size(21.dp), tint = tint)
            Text(label, style = MaterialTheme.typography.bodyMedium, color = if (destructive) tint else MaterialTheme.colorScheme.onSurface)
        }
    }
}
