package com.kpyruy.takt.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.CardAppearance
import com.kpyruy.takt.core.ui.theme.LocalTaktCardAppearance

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val appearance = LocalTaktCardAppearance.current
    val colors = when (appearance) {
        CardAppearance.ELEVATED -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        )
        CardAppearance.TONAL_FILLED -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
    val elevation = CardDefaults.cardElevation(
        defaultElevation = if (appearance == CardAppearance.ELEVATED) 1.dp else 0.dp,
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = colors,
        elevation = elevation,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}
