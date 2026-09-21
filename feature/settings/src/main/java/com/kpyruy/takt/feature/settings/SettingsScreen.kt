package com.kpyruy.takt.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.data.AppSettingsRepository
import com.kpyruy.takt.core.data.BackupRepository
import com.kpyruy.takt.core.model.AppSettings
import com.kpyruy.takt.core.model.CancellationDisplayStyle
import com.kpyruy.takt.core.model.ParityOverride
import com.kpyruy.takt.core.ui.components.ScreenHeader
import com.kpyruy.takt.core.ui.components.SectionCard
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    settingsRepository: AppSettingsRepository,
    backupRepository: BackupRepository,
    onBack: () -> Unit,
) {
    val settings by settingsRepository.settings.collectAsState(initial = AppSettings())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var backupMessage by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val result = runCatching {
                    val json = backupRepository.exportJson()
                    withContext(Dispatchers.IO) {
                        val stream = context.contentResolver.openOutputStream(uri)
                            ?: error("Не вдалося відкрити файл")
                        stream.bufferedWriter().use { it.write(json) }
                    }
                }
                backupMessage = if (result.isSuccess) {
                    "Резервну копію збережено."
                } else {
                    "Помилка експорту: ${result.exceptionOrNull()?.message ?: "невідома помилка"}"
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val result = runCatching {
                    val raw = withContext(Dispatchers.IO) {
                        val stream = context.contentResolver.openInputStream(uri)
                            ?: error("Не вдалося відкрити файл")
                        stream.bufferedReader().use { it.readText() }
                    }
                    backupRepository.importJson(raw)
                }
                backupMessage = if (result.isSuccess) {
                    "Резервну копію відновлено."
                } else {
                    "Помилка імпорту: ${result.exceptionOrNull()?.message ?: "невідома помилка"}"
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
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
            listOf(
                CancellationDisplayStyle.STRIKETHROUGH to "Закреслити",
                CancellationDisplayStyle.HIDDEN to "Сховати",
                CancellationDisplayStyle.MARKED to "Позначити",
            ).forEach { (option, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            scope.launch { settingsRepository.setCancellationStyle(option) }
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = settings.cancellationStyle == option,
                        onClick = {
                            scope.launch { settingsRepository.setCancellationStyle(option) }
                        },
                    )
                    Text(label)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Показувати приховані")
                    Text(
                        "Працює, коли стиль скасованих пар — «Сховати».",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = settings.showHiddenLessons,
                    onCheckedChange = { show ->
                        scope.launch { settingsRepository.setShowHiddenLessons(show) }
                    },
                )
            }
        }

        SectionCard {
            Text("Парність тижня", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            listOf(
                ParityOverride.AUTO to "Автоматично",
                ParityOverride.EVEN to "Примусово парний",
                ParityOverride.ODD to "Примусово непарний",
            ).forEach { (option, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            scope.launch { settingsRepository.setParityOverride(option) }
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = settings.parityOverride == option,
                        onClick = {
                            scope.launch { settingsRepository.setParityOverride(option) }
                        },
                    )
                    Text(label)
                }
            }
            Text(
                "В автоматичному режимі Takt використовує ISO-номер календарного тижня.",
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
                "Для кожного предмета шкалу можна змінити окремо.",
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionCard {
            Text("Резервна копія", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Експорт містить розклад, оцінки, навчальний план, домашки, нотатки й налаштування.",
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = {
                    exportLauncher.launch("takt-backup-${LocalDate.now()}.json")
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Експортувати JSON")
            }
            OutlinedButton(
                onClick = {
                    importLauncher.launch(arrayOf("application/json", "text/plain"))
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Text("Відновити з JSON")
            }
            backupMessage?.let {
                Text(
                    it,
                    modifier = Modifier.padding(top = 10.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
