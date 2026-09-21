package com.kpyruy.takt.feature.settings

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
fun SettingsScreen() {
    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Налаштування", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        SectionCard {
            Text("Відображення скасованих пар", fontWeight = FontWeight.SemiBold)
            Text("Закреслити · Сховати · Позначити")
        }
        SectionCard {
            Text("Шкала оцінювання", fontWeight = FontWeight.SemiBold)
            Text("A 92–100 · B 83–91 · C 74–82 · D 65–73 · E 56–64 · FX 0–55")
        }
    }
}
