package com.kpyruy.takt.feature.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.kpyruy.takt.core.model.GradeItem

@Composable
fun HomeGradeRow(
    item: GradeItem,
    courseTitle: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.title, fontWeight = FontWeight.SemiBold)
            Text(
                courseTitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = "${item.earnedPoints.compact()} / ${item.maxPoints.compact()}",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun Double.compact(): String =
    if (this % 1.0 == 0.0) toInt().toString() else "%.1f".format(this)
