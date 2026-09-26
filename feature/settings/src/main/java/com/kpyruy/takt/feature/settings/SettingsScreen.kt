package com.kpyruy.takt.feature.settings

import com.kpyruy.takt.core.ui.i18n.t

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
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.AccountCircle
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
import com.kpyruy.takt.core.data.StudyPlanRepository
import com.kpyruy.takt.core.data.UniversityAccountRepository
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
    studyPlanRepository: StudyPlanRepository,
    universityAccountRepository: UniversityAccountRepository,
    authenticateDevice: DeviceAuthenticationRequest,
    onBack: () -> Unit,
) {
    val settings by settingsRepository.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var backupMessage by remember { mutableStateOf<String?>(null) }
    var showAppearance by remember { mutableStateOf(false) }
    var showLanguage by remember { mutableStateOf(false) }
    var showCalendar by remember { mutableStateOf(false) }
    var showTasks by remember { mutableStateOf(false) }
    var showPeriods by remember { mutableStateOf(false) }
    var showUniversityAccount by remember { mutableStateOf(false) }
    val hasUniversityAccount by universityAccountRepository.hasAccount.collectAsStateWithLifecycle()
    val courses by remember(studyPlanRepository) { studyPlanRepository.observeCourses() }
        .collectAsStateWithLifecycle(initialValue = emptyList())
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
                            ?: error(t("Не вдалося відкрити файл"))
                        stream.bufferedWriter().use { it.write(json) }
                    }
                }
                backupMessage = if (result.isSuccess) {
                    t("Резервну копію збережено.")
                } else {
                    t("Помилка експорту: ") +
                        (result.exceptionOrNull()?.message ?: t("невідома помилка"))
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
                            ?: error(t("Не вдалося відкрити файл"))
                        stream.bufferedReader().use { it.readText() }
                    }
                    backupRepository.importJson(raw)
                }
                backupMessage = if (result.isSuccess) {
                    t("Резервну копію відновлено.")
                } else {
                    t("Помилка імпорту: ") +
                        (result.exceptionOrNull()?.message ?: t("невідома помилка"))
                }
            }
        }
    }

    val folderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) scope.launch {
            val result = runCatching { documentStore.connect(uri) }
            backupMessage = when (result.getOrNull()) {
                DocumentSyncStatus.READY -> t("Documents/Takt підключено. Дані синхронізуються автоматично.")
                DocumentSyncStatus.CONFLICT -> t("Знайдено різні дані. Виберіть, яку версію залишити.")
                else -> t("Помилка підключення: ${result.exceptionOrNull()?.message ?: "невідома помилка"}")
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
    if (showLanguage) {
        LanguageSettingsPage(
            selected = settings.language,
            onBack = { showLanguage = false },
            onSelected = { language ->
                scope.launch { settingsRepository.setLanguage(language) }
                showLanguage = false
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
    if (showPeriods) {
        SemesterPeriodsPage(
            semesters = (courses.map { it.semester } + settings.semesterPeriods.keys).distinct().sorted(),
            currentSemester = settings.effectiveCurrentSemester(courses),
            settings = settings,
            onBack = { showPeriods = false },
            onSave = { periods -> settingsRepository.setSemesterPeriods(periods) },
        )
        return
    }
    if (showUniversityAccount) {
        UniversityAccountPage(
            repository = universityAccountRepository,
            authenticate = authenticateDevice,
            onBack = { showUniversityAccount = false },
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
            title = t("Налаштування"),
            navigation = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = t("Назад"))
                }
            },
        )

        SettingsSectionTitle(t("Під себе"))
        SettingsNavigationTile(
            title = t("Вигляд"),
            icon = Icons.Outlined.Palette,
            onClick = { showAppearance = true },
        )
        SettingsNavigationTile(
            title = t("Мова"),
            icon = Icons.Outlined.Language,
            onClick = { showLanguage = true },
        )
        SettingsNavigationTile(
            title = t("Календар і розклад"),
            icon = Icons.Outlined.CalendarMonth,
            onClick = { showCalendar = true },
        )
        SettingsNavigationTile(
            title = t("Періоди навчання"),
            icon = Icons.Outlined.DateRange,
            onClick = { showPeriods = true },
        )

        SettingsNavigationTile(
            title = t("Задачі"),
            icon = Icons.Outlined.Checklist,
            onClick = { showTasks = true },
        )

        SettingsSectionTitle(t("Акаунт"))
        SettingsNavigationTile(
            title = t("Університетська система"),
            subtitle = if (hasUniversityAccount) t("UIS · дані збережено") else t("Локальний режим"),
            icon = Icons.Outlined.AccountCircle,
            onClick = { showUniversityAccount = true },
        )

        SettingsSectionTitle(t("Оцінювання"))
        SectionCard {
            Text(t("Стандартна шкала"), style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("A" to "92+", "B" to "83+", "C" to "74+", "D" to "65+", "E" to "56+").forEach { (grade, threshold) ->
                    Column(Modifier.weight(1f).padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(grade, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Text(threshold, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Text(t("FX · нижче 56%"), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        SettingsSectionTitle(t("Дані"))
        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(9.dp).background(
                    if (documentStatus == DocumentSyncStatus.READY) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error, CircleShape))
                Text("Documents/Takt", style = MaterialTheme.typography.titleMedium)
            }
            Text(
                when (documentStatus) {
                    DocumentSyncStatus.READY -> t("Підключено · синхронізація активна")
                    DocumentSyncStatus.CONFLICT -> t("Локальні дані й копія в Documents/Takt відрізняються")
                    DocumentSyncStatus.ERROR -> t("Помилка синхронізації. Перевірте доступ до папки.")
                    DocumentSyncStatus.DISCONNECTED -> t("Не підключено")
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (unmigratedMaterials > 0) {
                Text(
                    t("Не вдалося скопіювати $unmigratedMaterials старих матеріалів. Додайте їх заново, поки оригінали доступні."),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            OutlinedButton(onClick = { folderLauncher.launch(null) }, modifier = Modifier.fillMaxWidth()) {
                Text(if (documentStatus == DocumentSyncStatus.DISCONNECTED) t("Підключити Documents") else t("Змінити папку"))
            }
            if (documentStatus == DocumentSyncStatus.CONFLICT) {
                Button(onClick = {
                    scope.launch {
                        backupMessage = runCatching { documentStore.restoreFromDocuments() }
                            .fold({ t("Дані відновлено з Documents/Takt.") }, { t("Помилка відновлення: ${it.message}") })
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text(t("Відновити з Documents/Takt")) }
                OutlinedButton(onClick = {
                    scope.launch {
                        backupMessage = runCatching { documentStore.saveCurrentToDocuments() }
                            .fold({ t("Поточні дані записано в Documents/Takt.") }, { t("Помилка збереження: ${it.message}") })
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text(t("Замінити копію даними телефона")) }
            }
        }
        SectionCard {
            Text(t("Резервна копія"), style = MaterialTheme.typography.titleMedium)
            Button(
                onClick = {
                    exportLauncher.launch("takt-backup-" + LocalDate.now() + ".json")
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(t("Експортувати JSON"))
            }
            OutlinedButton(
                onClick = {
                    importLauncher.launch(arrayOf("application/json", "text/plain"))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(t("Відновити з JSON"))
            }
            backupMessage?.let { message ->
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (message.startsWith(t("Помилка"))) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
            }
        }
    }
}
