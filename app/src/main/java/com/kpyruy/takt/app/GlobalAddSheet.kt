package com.kpyruy.takt.app

import com.kpyruy.takt.core.ui.components.tourTarget
import com.kpyruy.takt.core.ui.i18n.t

import com.kpyruy.takt.core.ui.components.TaktFullSheet
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.ui.graphics.Color
import com.kpyruy.takt.core.ui.components.GlassSurface
import com.kpyruy.takt.core.ui.components.glassSurface
import com.kpyruy.takt.core.ui.components.TaktIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.ui.motion.rememberTaktHaptics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalAddSheet(
    allowCourseCreation: Boolean = true,
    onDismiss: () -> Unit,
    onCreate: (CreateItemType) -> Unit,
    onQuickAdd: () -> Unit,
) {
    TaktFullSheet(onDismissRequest = onDismiss) {
        GlobalAddContent(allowCourseCreation, onDismiss, onCreate, onQuickAdd)
    }
}

@Composable
internal fun GlobalAddContent(
    allowCourseCreation: Boolean,
    onDismiss: () -> Unit,
    onCreate: (CreateItemType) -> Unit,
    onQuickAdd: () -> Unit,
) {
    val haptics = rememberTaktHaptics()
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        // At enlarged text or narrow widths, keep whole words in one full-width tile.
        val columns = if (maxWidth < 360.dp || LocalDensity.current.fontScale > 1.15f) 1 else 2
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(t("Add"), Modifier.weight(1f), style = MaterialTheme.typography.headlineLarge)
                TaktIconButton(Icons.Outlined.Close, t("Close"), onDismiss)
            }
            val actions = CreateItemType.entries.filter { allowCourseCreation || it != CreateItemType.COURSE }
            Column(Modifier.fillMaxWidth().tourTarget("add-actions"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                actions.take(6).chunked(columns).forEach { pair -> AddActionRow(pair, onCreate) }
            }
            actions.drop(6).chunked(columns).forEach { pair -> AddActionRow(pair, onCreate) }
            GlassSurface(Modifier.fillMaxWidth().clickable(role = Role.Button) { haptics.tick(); onQuickAdd() }) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(42.dp).glassSurface(tint = MaterialTheme.colorScheme.primary.copy(alpha = .15f)),
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Bolt, null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Text(t("Quick add"), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}


@Composable
private fun AddActionRow(pair: List<CreateItemType>, onCreate: (CreateItemType) -> Unit) {
    val haptics = rememberTaktHaptics()
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        pair.forEach { type ->
            val tint = type.actionTint()
            GlassSurface(Modifier.weight(1f).testTag("global-add-${type.name}").clickable(role = Role.Button) {
                haptics.tick()
                onCreate(type)
            }) {
                Column(Modifier.fillMaxWidth().heightIn(min = 88.dp).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(32.dp).glassSurface(tint = tint.copy(alpha = .16f)),
                            contentAlignment = Alignment.Center) {
                            Icon(type.icon(), null, Modifier.size(20.dp), tint = tint)
                        }
                        Text(type.label, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                    }
                    // Description gets the entire tile width instead of a narrow icon-side column.
                    Text(type.actionHint(), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private fun CreateItemType?.icon(): ImageVector = when (this) {
    CreateItemType.COURSE -> Icons.Outlined.School
    CreateItemType.CLASS -> Icons.AutoMirrored.Outlined.MenuBook
    CreateItemType.TASK -> Icons.AutoMirrored.Outlined.Assignment
    CreateItemType.TEST -> Icons.Outlined.Quiz
    CreateItemType.EXAM -> Icons.Outlined.School
    CreateItemType.NOTE -> Icons.AutoMirrored.Outlined.Notes
    CreateItemType.EVENT -> Icons.Outlined.Event
    CreateItemType.REMINDER -> Icons.Outlined.Notifications
    null -> Icons.Outlined.Bolt
}

private fun CreateItemType.actionTint(): Color = when (this) {
    CreateItemType.COURSE, CreateItemType.NOTE -> Color(0xFF168265)
    CreateItemType.CLASS, CreateItemType.EVENT -> Color(0xFF2672E8)
    CreateItemType.TASK -> Color(0xFFC27A22)
    CreateItemType.TEST -> Color(0xFF7948C7)
    CreateItemType.EXAM -> Color(0xFFCA4747)
    CreateItemType.REMINDER -> Color(0xFFB5881F)
}

private fun CreateItemType.actionHint(): String = t(when (this) {
    CreateItemType.COURSE -> "Add course"
    CreateItemType.CLASS -> "Add class"
    CreateItemType.TASK -> "Add a task"
    CreateItemType.TEST -> "Add test"
    CreateItemType.EXAM -> "Add an exam"
    CreateItemType.NOTE -> "Add a note"
    CreateItemType.EVENT -> "Add event"
    CreateItemType.REMINDER -> "Add reminder"
})
