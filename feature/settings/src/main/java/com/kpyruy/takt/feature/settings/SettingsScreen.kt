package com.kpyruy.takt.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kpyruy.takt.core.data.AppSettingsRepository
import com.kpyruy.takt.core.data.BackupRepository
import com.kpyruy.takt.core.data.DocumentSyncStatus
import com.kpyruy.takt.core.data.TaktDocumentStore
import com.kpyruy.takt.core.model.AppSettings
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
    documentStore: TaktDocumentStore,
    onBack: () -> Unit,
) {
    val settings by settingsRepository.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var backupMessage by remember { mutableStateOf<String?>(null) }
    var showAppearance by remember { mutableStateOf(false) }
    var showCalendar by remember { mutableStateOf(false) }
    var showTasks by remember { mutableStateOf(false) }
    val documentStatus by documentStore.status.collectAsStateWithLifecycle()
    val unmigratedMaterials by documentStore.unmigratedMaterials.collectAsStateWithLifecycle()

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

    if (showAppearance) {
        AppearanceSettingsPage(
            settings = settings,
            onBack = { showAppearance = false },
            onApply = { preview ->
                scope.launch {
                    settingsRepository.setAppearance(preview.themeMode, preview.themeFamily, preview.cardAppearance)
                    showAppearance = false
                }
            },
        )
        return
    }
    if (showCalendar) {
        CalendarSettingsPage(
            settings = settings,
            onBack = { showCalendar = false },
            onCancellationStyle = { scope.launch { settingsRepository.setCancellationStyle(it) } },
            onShowHiddenLessons = { scope.launch { settingsRepository.setShowHiddenLessons(it) } },
            onParityOverride = { scope.launch { settingsRepository.setParityOverride(it) } },
            onWeekLayout = { scope.launch { settingsRepository.setWeekLayout(it) } },
        )
        return
    }

    if (showTasks) {
        TaskSettingsPage(
            filter = settings.homeWorkFilter,
            onBack = { showTasks = false },
            onChange = { scope.launch { settingsRepository.setHomeWorkFilter(it) } },
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeader(
            title = "Налаштування",
            navigation = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                }
            },
        )

        SettingsSectionTitle("Під себе")
        SettingsNavigationTile(
            title = "Вигляд",
            icon = Icons.Outlined.Palette,
            onClick = { showAppearance = true },
        )
        SettingsNavigationTile(
            title = "Календар і розклад",
            icon = Icons.Outlined.CalendarMonth,
            onClick = { showCalendar = true },
        )

        SettingsNavigationTile(
            title = "Задачі",
            icon = Icons.Outlined.Checklist,
            onClick = { showTasks = true },
        )

        SettingsSectionTitle("Оцінювання")
        SectionCard {
            Text("Стандартна шкала", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("A" to "92+", "B" to "83+", "C" to "74+", "D" to "65+", "E" to "56+").forEach { (grade, threshold) ->
                    Column(Modifier.weight(1f).padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(grade, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Text(threshold, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Text("FX · нижче 56%", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        SettingsSectionTitle("Дані")
        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(9.dp).background(
                    if (documentStatus == DocumentSyncStatus.READY) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error, CircleShape))
                Text("Documents/Takt", style = MaterialTheme.typography.titleMedium)
            }
            Text(
                when (documentStatus) {
                    DocumentSyncStatus.READY -> "Підключено · синхронізація активна"
                    DocumentSyncStatus.CONFLICT -> "Локальні дані й копія в Documents/Takt відрізняються"
                    DocumentSyncStatus.ERROR -> "Помилка синхронізації. Перевірте доступ до папки."
                    DocumentSyncStatus.DISCONNECTED -> "Не підключено"
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
        }
        SectionCard {
            Text("Резервна копія", style = MaterialTheme.typography.titleMedium)
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
