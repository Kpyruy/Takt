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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** A compact visual rail with full-height accessible hit targets. */
@Composable
fun TaktUnderlineTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxWidth()) {
        Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant))
        Row(Modifier.horizontalScroll(rememberScrollState()).selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            labels.forEachIndexed { index, label ->
                val selected = selectedIndex == index
                Column(Modifier.width(IntrinsicSize.Max).heightIn(min = 48.dp)
                    .selectable(selected, role = Role.Tab, onClick = { onSelected(index) }),
                    verticalArrangement = Arrangement.SpaceBetween) {
                    Text(label, modifier = Modifier.padding(top = 13.dp, bottom = 12.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    Box(Modifier.fillMaxWidth().height(2.dp)
                        .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent))
                }
            }
        }
    }
}
