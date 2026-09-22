package com.kpyruy.takt.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("Додати", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Оберіть тип елемента",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CreateItemType.entries.forEachIndexed { index, type ->
                if (index > 0) HorizontalDivider()
                Text(
                    text = type.label,
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .clickable {
                            haptics.tick()
                            onCreate(type)
                        }
                        .padding(vertical = 14.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            TextButton(
                onClick = {
                    haptics.tick()
                    onQuickAdd()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Швидко")
            }
        }
    }
}
