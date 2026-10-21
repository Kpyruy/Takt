package com.kpyruy.takt.feature.calendar

import com.kpyruy.takt.core.model.UiKey
import com.kpyruy.takt.core.ui.i18n.t

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.kpyruy.takt.core.ui.components.TaktFullSheet
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.*
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.kpyruy.takt.core.ui.i18n.TaktI18n
import com.kpyruy.takt.core.ui.components.GlassSurface

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
    TaktFullSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(event.lessonType.takeUnless { it == LessonType.UNSPECIFIED }?.label?.let(::t) ?: t(UiKey.TEXT_CLASS), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(event.title, Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
                IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, t(UiKey.TEXT_CLOSE_CLASS_ACTIONS)) }
            }
            Text(listOfNotNull(event.date.format(DateTimeFormatter.ofPattern("d MMMM", TaktI18n.locale)),
                "${event.startTime}–${event.endTime}", event.room).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            if (event.status != ScheduleEventStatus.CANCELLED || event.isAbsent) {
                val shape = RoundedCornerShape(14.dp)
                GlassSurface(Modifier.fillMaxWidth(), shape = shape,
                    tint = if (event.isAbsent) MaterialTheme.colorScheme.errorContainer else Color.Unspecified) {
                    Box(Modifier.background(if (event.isAbsent) MaterialTheme.colorScheme.errorContainer.copy(alpha = .64f)
                        else Color.Transparent, shape)) {
                        ActionRow(if (event.isAbsent) t(UiKey.TEXT_REMOVE_ABSENCE_MARK) else t(UiKey.TEXT_MARK_ABSENT),
                            Icons.Outlined.PersonOff, onToggleAbsence, selected = event.isAbsent,
                            contentColor = if (event.isAbsent) MaterialTheme.colorScheme.onErrorContainer else null)
                    }
                }
            }
            if (actions.canMoveOccurrence) ActionRow(t(UiKey.TEXT_MOVE_THIS_CLASS), Icons.Outlined.EventRepeat, onMoveOccurrence)
            if (actions.canCancelOccurrence) ActionRow(t(UiKey.TEXT_CANCEL_ONLY_THIS_CLASS), Icons.Outlined.EventBusy, onCancelOccurrence)
            if (actions.canRestoreOccurrence) ActionRow(t(UiKey.TEXT_RETURN_THE_ORIGINAL_CLASS), Icons.Outlined.Restore, onRestoreOccurrence)
            if (actions.canEditRecurringRule || actions.canEditOneOff) Spacer(Modifier.height(6.dp))
            if (actions.canEditRecurringRule) ActionRow(t(UiKey.TEXT_EDIT), Icons.Outlined.Edit, onEditRule)
            if (actions.canEditOneOff) ActionRow(t(UiKey.TEXT_EDIT_A_ONE_TIME_EVENT), Icons.Outlined.Edit, onEditOneOff)
            if (actions.canDeleteRecurringRule) ActionRow(t(UiKey.TEXT_DELETE_FROM_SCHEDULE), Icons.Outlined.DeleteOutline, onDeleteRule, destructive = true)
            if (actions.canDeleteOneOff) ActionRow(t(UiKey.TEXT_DELETE_A_ONE_TIME_EVENT), Icons.Outlined.DeleteOutline, onDeleteOneOff, destructive = true)
        }
    }
}

@Composable private fun ActionRow(label: String, icon: ImageVector, onClick: () -> Unit,
    destructive: Boolean = false, selected: Boolean? = null, contentColor: Color? = null) {
    val divider = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .30f)
    Surface(onClick = onClick, shape = RoundedCornerShape(12.dp), color = Color.Transparent,
        modifier = Modifier.semantics { selected?.let { this.selected = it } }) {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).drawBehind {
            val inset = 14.dp.toPx()
            drawLine(divider, Offset(inset, size.height), Offset(size.width - inset, size.height), 1.dp.toPx())
        }.padding(horizontal = 14.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            val tint = contentColor ?: if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            Icon(icon, null, Modifier.size(21.dp), tint = tint)
            Text(label, style = MaterialTheme.typography.bodyMedium, color = contentColor ?: if (destructive) tint else MaterialTheme.colorScheme.onSurface)
        }
    }
}
