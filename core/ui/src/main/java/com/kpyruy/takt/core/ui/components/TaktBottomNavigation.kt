package com.kpyruy.takt.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.ui.motion.TaktMotion

data class TaktNavItem(val label: String, val icon: ImageVector, val selected: Boolean, val onClick: () -> Unit)

@Composable
fun TaktBottomNavigation(items: List<TaktNavItem>, centerContent: (@Composable () -> Unit)? = null) {
    require(items.size == 4)
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.navigationBarsPadding()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier.fillMaxWidth().selectableGroup().padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                items.forEachIndexed { index, item ->
                    if (index == 2 && centerContent != null) {
                        Box(Modifier.size(60.dp), contentAlignment = Alignment.Center) { centerContent() }
                    }
                    val color by animateColorAsState(
                        targetValue = if (item.selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = TaktMotion.standard(),
                        label = "navigation-content",
                    )
                    val indicator by animateColorAsState(
                        targetValue = if (item.selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        animationSpec = TaktMotion.standard(),
                        label = "navigation-indicator",
                    )
                    Column(
                        modifier = Modifier.weight(1f).padding(horizontal = 5.dp).clip(RoundedCornerShape(12.dp))
                            .background(indicator).heightIn(min = 52.dp)
                            .selectable(selected = item.selected, role = Role.Tab, onClick = item.onClick)
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Icon(item.icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
                        Text(item.label, color = color, style = MaterialTheme.typography.labelSmall,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
fun TaktAddFab(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick, modifier = Modifier.size(56.dp).testTag("root-add"), shape = RoundedCornerShape(18.dp),
        containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp, pressedElevation = 7.dp),
    ) { Icon(Icons.Default.Add, contentDescription = "Додати") }
}
