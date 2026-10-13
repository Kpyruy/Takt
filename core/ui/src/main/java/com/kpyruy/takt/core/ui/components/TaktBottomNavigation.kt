package com.kpyruy.takt.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.ui.i18n.t
import com.kpyruy.takt.core.ui.motion.TaktMotion

data class TaktNavItem(val label: String, val icon: ImageVector, val selected: Boolean, val onClick: () -> Unit)

@Composable
fun TaktBottomNavigation(items: List<TaktNavItem>, centerContent: (@Composable () -> Unit)? = null) {
    require(items.size == 4)
    Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(999.dp))
                .glassSurface(RoundedCornerShape(999.dp)).selectableGroup().padding(5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            items.forEachIndexed { index, item ->
                if (index == 2 && centerContent != null) {
                    Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) { centerContent() }
                }
                val color by animateColorAsState(
                    if (item.selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    TaktMotion.standard(), label = "navigation-content",
                )
                Column(
                    Modifier.weight(1f)
                        .then(if (item.selected) Modifier.glassSurface(RoundedCornerShape(999.dp), MaterialTheme.colorScheme.primaryContainer) else Modifier)
                        .clip(RoundedCornerShape(999.dp)).heightIn(min = 56.dp)
                        .selectable(item.selected, role = Role.Tab, onClick = item.onClick)
                        .padding(horizontal = 2.dp, vertical = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(item.icon, null, tint = color, modifier = Modifier.size(22.dp))
                    Text(item.label, color = color, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun TaktAddFab(onClick: () -> Unit) {
    Box(
        Modifier.size(52.dp).glassSurface(CircleShape, MaterialTheme.colorScheme.primaryContainer)
            .clickable(role = Role.Button, onClick = onClick).testTag("root-add"),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Default.Add, t("Додати"), Modifier.size(30.dp), tint = MaterialTheme.colorScheme.primary)
    }
}
