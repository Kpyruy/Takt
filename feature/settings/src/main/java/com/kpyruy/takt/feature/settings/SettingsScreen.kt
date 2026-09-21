package com.kpyruy.takt.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.ui.components.ScreenHeader
import com.kpyruy.takt.core.ui.components.SectionCard

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    var cancellationStyle by remember { mutableStateOf("Закреслити") }
    var showHidden by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenHeader(
            title = "Налаштування",
            action = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                }
            },
        )

        SectionCard {
            Text("Скасовані пари", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            listOf("Закреслити", "Сховати", "Позначити").forEach { option ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { cancellationStyle = option },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = cancellationStyle == option,
                        onClick = { cancellationStyle = option },
                    )
                    Text(option)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Показувати приховані")
                Switch(checked = showHidden, onCheckedChange = { showHidden = it })
            }
        }

        SectionCard {
            Text("Шкала оцінювання", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("A · 92–100%")
            Text("B · 83–91%")
            Text("C · 74–82%")
            Text("D · 65–73%")
            Text("E · 56–64%")
            Text("FX · 0–55%")
            Text(
                "Для кожного предмета шкалу можна буде змінити окремо.",
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionCard {
            Text("Парність тижня", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Автоматично за ISO-номером календарного тижня.")
            Text(
                "Ручне перевизначення додамо разом зі збереженням налаштувань.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
