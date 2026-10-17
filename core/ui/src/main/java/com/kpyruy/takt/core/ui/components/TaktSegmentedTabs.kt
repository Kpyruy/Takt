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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.rememberTextMeasurer
import com.kpyruy.takt.core.ui.motion.rememberTaktHaptics

/** Equal segments at normal text size; a scrollable rail when enlarged. */
@Composable
fun TaktSegmentedTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    railCornerRadius: Dp = 16.dp,
    tabTag: ((Int) -> String)? = null,
) {
    if (labels.isEmpty()) return
    val haptics = rememberTaktHaptics()
    val switchTab = com.kpyruy.takt.core.ui.motion.rememberTabSwitchGuard()
    val density = LocalDensity.current
    val style = MaterialTheme.typography.labelMedium
    val measurer = rememberTextMeasurer()
    val labelPadding = if (labels.size >= 4) 4.dp else 12.dp
    val labelWidth = remember(labels, style, density) {
        with(density) { labels.maxOf { measurer.measure(it, style).size.width }.toDp() }
    }
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(railCornerRadius)
    BoxWithConstraints(modifier.fillMaxWidth().clip(shape).glassSurface(shape)) {
        val available = (maxWidth - 8.dp - 3.dp * (labels.size - 1)) / labels.size
        val scrollable = density.fontScale > 1.15f || labelWidth + labelPadding * 2 > available
        val width = if (scrollable) (labelWidth + labelPadding * 2).coerceAtLeast(88.dp) else available
        val selected = selectedIndex.coerceIn(labels.indices)
        val offset = androidx.compose.animation.core.animateDpAsState((width + 3.dp) * selected,
            com.kpyruy.takt.core.ui.motion.TaktMotion.menu(), label = "segment-lens")
        Box(Modifier.fillMaxWidth().then(if (scrollable) Modifier.horizontalScroll(rememberScrollState()) else Modifier).padding(horizontal = 4.dp)) {
            Box(Modifier.offset { androidx.compose.ui.unit.IntOffset(offset.value.roundToPx(), 0) }.width(width).height(48.dp).padding(vertical = 4.dp)
                .glassSurface(androidx.compose.foundation.shape.RoundedCornerShape(10.dp), MaterialTheme.colorScheme.primaryContainer))
            Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                labels.forEachIndexed { index, label ->
                    val source = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    val bringIntoView = remember { BringIntoViewRequester() }
                    LaunchedEffect(index == selected, scrollable) { if (index == selected && scrollable) bringIntoView.bringIntoView() }
                    Box(Modifier.then(tabTag?.let { Modifier.testTag(it(index)) } ?: Modifier).width(width).height(48.dp).bringIntoViewRequester(bringIntoView)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp)).taktPressFeedback(source)
                        .selectable(index == selected, interactionSource = source, indication = null, role = Role.Tab, onClick = {
                            if (index != selected) switchTab { haptics.tick(); onSelected(index) }
                        }), contentAlignment = Alignment.Center) {
                        Text(label, Modifier.padding(horizontal = labelPadding, vertical = 12.dp), style = style,
                            maxLines = 1, softWrap = false,
                            color = if (index == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
