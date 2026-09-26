package com.kpyruy.takt.app

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
    onDismiss: () -> Unit,
    onCreate: (CreateItemType) -> Unit,
    onQuickAdd: () -> Unit,
) {
    val haptics = rememberTaktHaptics()
    TaktFullSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Додати", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Оберіть тип елемента",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val actions: List<CreateItemType?> = CreateItemType.entries + null
            actions.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { type ->
                        Surface(
                            onClick = {
                                haptics.tick()
                                if (type == null) onQuickAdd() else onCreate(type)
                            },
                            modifier = Modifier.weight(1f).heightIn(min = 76.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(type.icon(), contentDescription = null,
                                    modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                                Text(type?.label ?: "Швидко", style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1)
                            }
                        }
                    }
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
