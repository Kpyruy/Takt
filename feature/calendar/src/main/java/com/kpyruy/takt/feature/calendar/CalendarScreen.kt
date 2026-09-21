package com.kpyruy.takt.feature.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.ui.components.SectionCard

@Composable
fun CalendarScreen() {
    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Календар", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        SectionCard {
            Text("Тижневий розклад", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Наступний модуль додасть день / тиждень / місяць, парність та винятки для окремих дат.")
        }
    }
}
