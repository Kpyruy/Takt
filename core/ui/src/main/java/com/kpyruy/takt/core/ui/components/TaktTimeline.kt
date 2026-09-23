package com.kpyruy.takt.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

data class TaktTimelineItem(
    val time: String,
    val title: String,
    val supporting: String?,
    val markerColor: Color,
    val emphasized: Boolean = false,
    val dimmed: Boolean = false,
    val strikethrough: Boolean = false,
)

@Composable
fun TaktTimeline(
    items: List<TaktTimelineItem>,
    gapLabels: Map<Int, String> = emptyMap(),
    modifier: Modifier = Modifier,
    onItemClick: ((Int) -> Unit)? = null,
    currentTimeBeforeIndex: Int? = null,
    currentTimeLabel: String? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        items.forEachIndexed { index, item ->
            if (currentTimeBeforeIndex == index && currentTimeLabel != null) {
                CurrentTimeMarker(currentTimeLabel)
            }

            val rowModifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .then(
                    if (onItemClick != null) {
                        Modifier.clickable { onItemClick(index) }
                    } else {
                        Modifier
                    }
                )
                .padding(vertical = 8.dp)

            Row(
                modifier = rowModifier,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = item.time,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(58.dp).padding(top = 2.dp),
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(if (item.emphasized) 10.dp else 8.dp)
                            .background(item.markerColor, CircleShape)
                    )
                    if (index != items.lastIndex) {
                        Spacer(
                            Modifier
                                .width(2.dp)
                                .height(42.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                    }
                }
                Column(
                    Modifier.weight(1f)
                        .clip(MaterialTheme.shapes.small)
                        .background(if (item.emphasized) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent)
                        .padding(horizontal = if (item.emphasized) 12.dp else 0.dp, vertical = if (item.emphasized) 10.dp else 0.dp)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (item.dimmed) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        textDecoration = if (item.strikethrough) TextDecoration.LineThrough else null,
                        maxLines = 2,
                    )
                    item.supporting?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                        )
                    }
                }
            }

            gapLabels[index]?.let { label ->
                Text(
                    text = label,
                    modifier = Modifier.padding(start = 84.dp, bottom = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (currentTimeBeforeIndex == items.size && currentTimeLabel != null) {
            CurrentTimeMarker(currentTimeLabel)
        }
    }
}

@Composable
private fun CurrentTimeMarker(label: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.width(58.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}
