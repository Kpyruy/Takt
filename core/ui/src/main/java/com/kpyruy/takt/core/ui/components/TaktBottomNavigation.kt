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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.graphicsLayer
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
    val switchTab = com.kpyruy.takt.core.ui.motion.rememberTabSwitchGuard()
    val selected = items.indexOfFirst { it.selected }.coerceAtLeast(0)
    val shape = RoundedCornerShape(999.dp)
    Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth().clip(shape)
            .glassSurface(shape, sampleContent = true).testTag("root-navigation")) {
            val centerWidth = if (centerContent != null) 56.dp else 0.dp
            val width = (maxWidth - 10.dp - centerWidth) / 4
            val target = width * selected + if (selected >= 2) centerWidth else 0.dp
            val offset = androidx.compose.animation.core.animateDpAsState(target,
                TaktMotion.menu(), label = "navigation-lens")
            Box(Modifier.padding(5.dp)) {
                // Read the moving position during placement, avoiding a rail recomposition
                // on every120/144Hz frame. This offset overload also preserves RTL placement.
                Box(Modifier.offset { androidx.compose.ui.unit.IntOffset(offset.value.roundToPx(), 0) }.width(width).height(60.dp)
                    .glassSurface(shape, MaterialTheme.colorScheme.primaryContainer)
                    .testTag("navigation-lens"))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    items.forEachIndexed { index, item ->
                        if (index == 2 && centerContent != null) Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) { centerContent() }
                        NavigationTab(item, width, shape) { if (!item.selected) switchTab(item.onClick) }
                    }
                }
            }
        }
    }
}

/** Tint changes recompose just this item; icon scale is read only by its graphics layer. */
@Composable
private fun NavigationTab(item: TaktNavItem, width: androidx.compose.ui.unit.Dp, shape: RoundedCornerShape, onClick: () -> Unit) {
    val source = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val color by animateColorAsState(if (item.selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        TaktMotion.standard(), label = "navigation-content")
    val pop = androidx.compose.animation.core.animateFloatAsState(if (item.selected) 1.08f else 1f,
        androidx.compose.animation.core.spring(dampingRatio = .9f, stiffness = 400f), label = "navigation-icon")
    Column(Modifier.width(width).height(60.dp).clip(shape).taktPressFeedback(source)
        .selectable(item.selected, interactionSource = source, indication = null, role = Role.Tab, onClick = onClick)
        .padding(horizontal = 2.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(item.icon, null, tint = color, modifier = Modifier.size(22.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value })
        Text(item.label, color = color, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 1)
    }
}

@Composable
fun TaktAddFab(onClick: () -> Unit) {
    val source = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Box(
        Modifier.size(52.dp).glassSurface(CircleShape, MaterialTheme.colorScheme.primaryContainer)
            .clip(CircleShape).taktPressFeedback(source).clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick).testTag("root-add").tourTarget("add-button"),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Default.Add, t("Add"), Modifier.size(30.dp), tint = MaterialTheme.colorScheme.primary)
    }
}
