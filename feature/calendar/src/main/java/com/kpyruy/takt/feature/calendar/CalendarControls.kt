package com.kpyruy.takt.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.ui.motion.rememberTaktHaptics

/** Calendar-only controls: full touch targets with a quieter, narrower visual rail. */
@Composable
internal fun CalendarTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberTaktHaptics()
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.outlineVariant)
            .selectableGroup()
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        labels.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.outlineVariant)
                    .selectable(selected = selected, role = Role.Tab) {
                        if (!selected) {
                            haptics.tick()
                            onSelected(index)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
    }
}

@Composable
internal fun CalendarPeriodNavigation(
    onPrevious: () -> Unit,
    onToday: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.height(48.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Попередній період")
        }
        Box(
            modifier = Modifier
                .width(68.dp)
                .height(48.dp)
                .clickable(role = Role.Button, onClick = onToday),
            contentAlignment = Alignment.Center,
        ) {
            Text("Сьогодні", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        IconButton(onClick = onNext, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Наступний період")
        }
    }
}
