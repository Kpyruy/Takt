package com.kpyruy.takt.feature.settings

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.data.AppSettingsRepository
import com.kpyruy.takt.core.data.BackupRepository
import com.kpyruy.takt.core.data.TaktDocumentStore
import com.kpyruy.takt.core.data.DocumentSyncStatus
import com.kpyruy.takt.core.model.AppSettings
import com.kpyruy.takt.core.model.AppThemeMode
import com.kpyruy.takt.core.model.CardAppearance
import com.kpyruy.takt.core.model.CancellationDisplayStyle
import com.kpyruy.takt.core.model.ParityOverride
import com.kpyruy.takt.core.model.ThemeFamily
import com.kpyruy.takt.core.model.WeekLayout
import com.kpyruy.takt.core.ui.components.ScreenHeader
import com.kpyruy.takt.core.ui.components.SectionCard
import com.kpyruy.takt.core.ui.theme.taktThemePreviewColors
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    settingsRepository: AppSettingsRepository,
    backupRepository: BackupRepository,
    documentStore: TaktDocumentStore,
    onBack: () -> Unit,
) {
    val settings by settingsRepository.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var backupMessage by remember { mutableStateOf<String?>(null) }
    val documentStatus by documentStore.status.collectAsStateWithLifecycle()
    val unmigratedMaterials by documentStore.unmigratedMaterials.collectAsStateWithLifecycle()
    val darkPreview = when (settings.themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

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
                    "Помилка експорту: " +
                        (result.exceptionOrNull()?.message ?: "невідома помилка")
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
                    "Помилка імпорту: " +
                        (result.exceptionOrNull()?.message ?: "невідома помилка")
                }
            }
        }
    }

    val folderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) scope.launch {
            val result = runCatching { documentStore.connect(uri) }
            backupMessage = when (result.getOrNull()) {
                DocumentSyncStatus.READY -> "Documents/Takt підключено. Дані синхронізуються автоматично."
                DocumentSyncStatus.CONFLICT -> "Знайдено різні дані. Виберіть, яку версію залишити."
                else -> "Помилка підключення: ${result.exceptionOrNull()?.message ?: "невідома помилка"}"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader(
            title = "Налаштування",
            navigation = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                }
            },
        )

        SettingsSectionTitle("Вигляд")
        SectionCard {
            Text("Режим теми", style = MaterialTheme.typography.titleMedium)
            listOf(
                AppThemeMode.SYSTEM to "Як у системі",
                AppThemeMode.LIGHT to "Світла",
                AppThemeMode.DARK to "Темна",
            ).forEach { (option, label) ->
                PreferenceRadioRow(
                    label = label,
                    selected = settings.themeMode == option,
                    onClick = { scope.launch { settingsRepository.setThemeMode(option) } },
                )
            }

            Text("Кольорова тема", style = MaterialTheme.typography.titleMedium)
            listOf(
                ThemeFamily.BLUE to "Синя",
                ThemeFamily.GREEN to "Зелена",
                ThemeFamily.PURPLE to "Фіолетова",
                ThemeFamily.WARM to "Тепла",
                ThemeFamily.MONOCHROME to "Монохром",
            ).forEach { (option, label) ->
                ThemeRadioRow(
                    family = option,
                    label = label,
                    darkPreview = darkPreview,
                    selected = settings.themeFamily == option,
                    onClick = { scope.launch { settingsRepository.setThemeFamily(option) } },
                )
            }

            Text("Картки", style = MaterialTheme.typography.titleMedium)
            listOf(
                CardAppearance.ELEVATED to "Підняті",
                CardAppearance.TONAL_FILLED to "Заливка",
            ).forEach { (option, label) ->
                PreferenceRadioRow(
                    label = label,
                    selected = settings.cardAppearance == option,
                    onClick = { scope.launch { settingsRepository.setCardAppearance(option) } },
                )
            }
        }

        SettingsSectionTitle("Календар")
        SectionCard {
            Text("Скасовані пари", style = MaterialTheme.typography.titleMedium)
            listOf(
                CancellationDisplayStyle.STRIKETHROUGH to "Закреслити",
                CancellationDisplayStyle.HIDDEN to "Сховати",
                CancellationDisplayStyle.MARKED to "Позначити",
            ).forEach { (option, label) ->
                PreferenceRadioRow(
                    label = label,
                    selected = settings.cancellationStyle == option,
                    onClick = { scope.launch { settingsRepository.setCancellationStyle(option) } },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
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

            Text("Парність тижня", style = MaterialTheme.typography.titleMedium)
            listOf(
                ParityOverride.AUTO to "Автоматично",
                ParityOverride.EVEN to "Примусово парний",
                ParityOverride.ODD to "Примусово непарний",
            ).forEach { (option, label) ->
                PreferenceRadioRow(
                    label = label,
                    selected = settings.parityOverride == option,
                    onClick = { scope.launch { settingsRepository.setParityOverride(option) } },
                )
            }
            Text(
                "Автоматичний режим використовує ISO-номер календарного тижня.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text("Вигляд тижня", style = MaterialTheme.typography.titleMedium)
            listOf(
                WeekLayout.TIMETABLE to "Таймтейбл",
                WeekLayout.COMPACT_LIST to "Компактний список",
            ).forEach { (option, label) ->
                PreferenceRadioRow(
                    label = label,
                    selected = settings.weekLayout == option,
                    onClick = { scope.launch { settingsRepository.setWeekLayout(option) } },
                )
            }
        }

        SettingsSectionTitle("Оцінювання")
        SectionCard {
            Text("Стандартна шкала", style = MaterialTheme.typography.titleMedium)
            Text("A · 92–100%")
            Text("B · 83–91%")
            Text("C · 74–82%")
            Text("D · 65–73%")
            Text("E · 56–64%")
            Text("FX · 0–55%")
            Text(
                "Для кожного предмета шкалу можна змінити окремо.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SettingsSectionTitle("Дані")
        SectionCard {
            Text("Documents/Takt", style = MaterialTheme.typography.titleMedium)
            Text(
                when (documentStatus) {
                    DocumentSyncStatus.READY -> "Підключено · зміни зберігаються автоматично в data.json"
                    DocumentSyncStatus.CONFLICT -> "Локальні дані й копія в Documents/Takt відрізняються"
                    DocumentSyncStatus.ERROR -> "Помилка синхронізації. Перевірте доступ до папки."
                    DocumentSyncStatus.DISCONNECTED -> "Підключіть папку Documents. Takt створить у ній Takt і збереже дані та матеріали."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (unmigratedMaterials > 0) {
                Text(
                    "Не вдалося скопіювати $unmigratedMaterials старих матеріалів. Додайте їх заново, поки оригінали доступні.",
                    color = MaterialTheme.colorScheme.error,
                )
            }
            OutlinedButton(onClick = { folderLauncher.launch(null) }, modifier = Modifier.fillMaxWidth()) {
                Text(if (documentStatus == DocumentSyncStatus.DISCONNECTED) "Підключити Documents" else "Змінити папку")
            }
            if (documentStatus == DocumentSyncStatus.CONFLICT) {
                Button(onClick = {
                    scope.launch {
                        backupMessage = runCatching { documentStore.restoreFromDocuments() }
                            .fold({ "Дані відновлено з Documents/Takt." }, { "Помилка відновлення: ${it.message}" })
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text("Відновити з Documents/Takt") }
                OutlinedButton(onClick = {
                    scope.launch {
                        backupMessage = runCatching { documentStore.saveCurrentToDocuments() }
                            .fold({ "Поточні дані записано в Documents/Takt." }, { "Помилка збереження: ${it.message}" })
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text("Залишити дані на телефоні") }
            }
            Text("Резервна копія", style = MaterialTheme.typography.titleMedium)
            Text(
                "Експорт містить розклад, оцінювання, екзамени, матеріали, навчальний план, завдання, нотатки й налаштування.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = {
                    exportLauncher.launch("takt-backup-" + LocalDate.now() + ".json")
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Експортувати JSON")
            }
            OutlinedButton(
                onClick = {
                    importLauncher.launch(arrayOf("application/json", "text/plain"))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Відновити з JSON")
            }
            backupMessage?.let { message ->
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (message.startsWith("Помилка")) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
            }
        }
    }
}

@Composable
private fun PreferenceRadioRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label)
    }
}

@Composable
private fun ThemeRadioRow(
    family: ThemeFamily,
    label: String,
    darkPreview: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column(Modifier.weight(1f)) {
            Text(label)
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                taktThemePreviewColors(family, dark = darkPreview).forEach { color ->
                    Box(
                        Modifier
                            .size(12.dp)
                            .background(color, CircleShape)
                    )
                }
            }
        }
    }
}
