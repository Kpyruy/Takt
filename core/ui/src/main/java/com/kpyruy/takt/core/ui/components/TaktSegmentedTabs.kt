package com.kpyruy.takt.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.ui.motion.rememberTaktHaptics

/** Equal segments at normal text size; a scrollable rail when enlarged. */
@Composable
fun TaktSegmentedTabs(labels: List<String>, selectedIndex: Int, onSelected: (Int) -> Unit, modifier: Modifier = Modifier) {
    val haptics = rememberTaktHaptics()
    val enlarged = LocalDensity.current.fontScale > 1.15f
    Row(modifier.fillMaxWidth().clip(MaterialTheme.shapes.small)
        .background(MaterialTheme.colorScheme.outlineVariant)
        .then(if (enlarged) Modifier.horizontalScroll(rememberScrollState()) else Modifier)
        .selectableGroup().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        labels.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(modifier = (if (enlarged) Modifier.widthIn(min = 88.dp) else Modifier.weight(1f))
                .heightIn(min = 48.dp).selectable(selected, role = Role.Tab, onClick = {
                    if (!selected) { haptics.tick(); onSelected(index) }
                }), contentAlignment = Alignment.Center) {
                Box(Modifier.matchParentSize().padding(vertical = 4.dp).clip(MaterialTheme.shapes.extraSmall)
                    .background(if (selected) MaterialTheme.colorScheme.surface else Color.Transparent))
                Text(label, Modifier.padding(horizontal = 12.dp, vertical = 12.dp), style = MaterialTheme.typography.labelMedium,
                    color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
