package com.kpyruy.takt.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Source-compatible compact tabs using the shared glass selection rail. */
@Composable
fun TaktUnderlineTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    TaktSegmentedTabs(labels, selectedIndex, onSelected, modifier)
}
