package com.kpyruy.takt.feature.settings

import com.kpyruy.takt.core.ui.components.tourTarget
import com.kpyruy.takt.core.ui.i18n.t

import androidx.compose.runtime.saveable.rememberSaveable
import com.kpyruy.takt.core.ui.components.rememberSaveAction
import com.kpyruy.takt.core.ui.components.SaveErrorDialog
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Notifications
import com.kpyruy.takt.core.ui.components.TaktIconButton
import com.kpyruy.takt.core.ui.components.TaktPrimaryButton as Button
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
import com.kpyruy.takt.core.data.AppReleaseRepository
import com.kpyruy.takt.core.data.AppSettingsRepository
import com.kpyruy.takt.core.data.BackupRepository
import com.kpyruy.takt.core.data.BackupPayloadCodec
import com.kpyruy.takt.core.data.BackupSection
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
    appReleaseRepository: AppReleaseRepository,
    onBack: () -> Unit,
    onStartTour: () -> Unit = {},
    guidedPage: String? = null,
) {
    val settings by settingsRepository.settings.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val saves = rememberSaveAction()
    SaveErrorDialog(saves)
    val context = LocalContext.current
    var backupMessage by remember { mutableStateOf<String?>(null) }
    var backupReview by remember { mutableStateOf<BackupReviewState?>(null) }
    var showAppearance by rememberSaveable { mutableStateOf(guidedPage == "appearance") }
    var showLanguage by rememberSaveable { mutableStateOf(false) }
    var showCalendar by rememberSaveable { mutableStateOf(false) }
    var showTasks by rememberSaveable { mutableStateOf(guidedPage == "filters") }
    var showNotifications by rememberSaveable { mutableStateOf(guidedPage == "reminders") }
    var showPeriods by rememberSaveable { mutableStateOf(guidedPage == "periods") }
    var showUniversityAccount by rememberSaveable { mutableStateOf(false) }
    var showUisAutoSync by rememberSaveable { mutableStateOf(false) }
    var showDocuments by rememberSaveable { mutableStateOf(guidedPage == "documents") }
    var showAppUpdates by rememberSaveable { mutableStateOf(false) }
    val releaseState by appReleaseRepository.state.collectAsStateWithLifecycle()
    val hasUniversityAccount by universityAccountRepository.hasAccount.collectAsStateWithLifecycle()
    val uisPreview by universityAccountRepository.syncPreview.collectAsStateWithLifecycle()
    val uisSession by universityAccountRepository.session.state.collectAsStateWithLifecycle()
    val uisImport by universityAccountRepository.importProgress.collectAsStateWithLifecycle()
    val courses by remember(studyPlanRepository) { studyPlanRepository.observeCourses() }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val documentStatus by documentStore.status.collectAsStateWithLifecycle()
    val unmigratedMaterials by documentStore.unmigratedMaterials.collectAsStateWithLifecycle()

    fun reviewDocuments(initialSelection: Set<BackupSection> = emptySet()) {
        scope.launch {
            runCatching {
                val sourceRaw = documentStore.readBackupForReview()
                val localRaw = backupRepository.exportJson()
                BackupPayloadCodec.decode(sourceRaw)
                backupReview = BackupReviewState(localRaw, sourceRaw, BackupReviewSource.DOCUMENTS, initialSelection)
            }.onFailure {
                backupMessage = t("Recovery Error: ${t(it.message.orEmpty())}")
                showPeriods = false
            }
        }
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
                            ?: error(t("Failed to open file"))
                        stream.bufferedWriter().use { it.write(json) }
                    }
                }
                backupMessage = if (result.isSuccess) {
                    t("Backup saved.")
                } else {
                    t("Export error: ") +
                        (result.exceptionOrNull()?.message ?: t("unknown error"))
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
                            ?: error(t("Failed to open file"))
                        stream.bufferedReader().use { it.readText() }
                    }
                    BackupPayloadCodec.decode(raw)
                    val localRaw = backupRepository.exportJson()
                    backupReview = BackupReviewState(localRaw, raw, BackupReviewSource.JSON_FILE)
                }
                backupMessage = if (result.isSuccess) {
                    null
                } else {
                    t("Import error: ") +
                        (result.exceptionOrNull()?.message ?: t("unknown error"))
                }
            }
        }
    }

    val folderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) scope.launch {
            val result = runCatching { documentStore.connect(uri) }
            backupMessage = when (result.getOrNull()) {
                DocumentSyncStatus.READY -> t("Documents/Takt is connected. Data is synchronized automatically.")
                DocumentSyncStatus.CONFLICT -> t("Different data found. Choose which version to keep.")
                else -> t("Connection error: ${t(result.exceptionOrNull()?.message ?: "Unknown error")}")
            }
        }
    }

    if (showAppUpdates) {
        AppUpdatesPage(appReleaseRepository, onBack = { showAppUpdates = false })
        return
    }
    if (showAppearance) {
        AppearanceSettingsPage(
            settings = settings,
            onBack = { showAppearance = false },
            onApply = { preview ->
                saves.launch {
                    settingsRepository.setAppearance(preview)
                    showAppearance = false
                }
            },
        )
        return
    }
    backupReview?.let { review ->
        BackupReviewPage(review = review, onBack = { backupReview = null }, onApply = { selected ->
            if (review.source == BackupReviewSource.DOCUMENTS) {
                documentStore.importSelectedFromDocuments(review.sourceRaw, review.localRaw, selected)
            } else if (documentStore.isConnected) {
                documentStore.importSelectedFromFile(review.sourceRaw, review.localRaw, selected)
            } else {
                backupRepository.importSelectedJson(review.sourceRaw, review.localRaw, selected)
            }
            backupMessage = if (documentStore.isConnected) t("Selected data applied and saved.")
                else t("Selected data applied.")
        })
        return
    }
    if (showLanguage) {
        LanguageSettingsPage(
            selected = settings.language,
            courseNameLanguage = settings.courseNameLanguage,
            ukrainianFallback = settings.ukrainianCourseNameFallback,
            onBack = { showLanguage = false },
            onSelected = { language ->
                saves.queue { settingsRepository.setLanguage(language); showLanguage = false }
            },
            onCourseNameSelected = { saves.queue { settingsRepository.setCourseNameLanguage(it) } },
            onUkrainianFallbackSelected = { saves.queue { settingsRepository.setUkrainianCourseNameFallback(it) } },
        )
        return
    }
    if (showCalendar) {
        CalendarSettingsPage(
            settings = settings,
            onBack = { showCalendar = false },
            onCancellationStyle = { saves.queue { settingsRepository.setCancellationStyle(it) } },
            onShowHiddenLessons = { saves.queue { settingsRepository.setShowHiddenLessons(it) } },
            onParityOverride = { saves.queue { settingsRepository.setParityOverride(it) } },
            onWeekLayout = { saves.queue { settingsRepository.setWeekLayout(it) } },
        )
        return
    }

    if (showNotifications) {
        NotificationsSettingsPage(settings.reminders,
            onChange = { saves.queue { settingsRepository.updateReminders(it) } },
            onBack = { showNotifications = false })
        return
    }
    if (showTasks) {
        TaskSettingsPage(
            filter = settings.homeWorkFilter,
            onBack = { showTasks = false },
            onChange = { saves.queue { settingsRepository.setHomeWorkFilter(it) } },
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
            onReviewDocuments = if (documentStore.isConnected) {
                { reviewDocuments(setOf(BackupSection.PERIODS)) }
            } else null,
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
    if (showUisAutoSync) {
        UisAutoSyncSettingsPage(settings.uisAutoSync,
            onChange = { saves.queue { settingsRepository.setUisAutoSync(it) } },
            onBack = { showUisAutoSync = false })
        return
    }

    if (showDocuments) {
        DocumentsSettingsPage(onBack = { showDocuments = false }) {
        SectionCard(modifier = Modifier.tourTarget("settings-documents")) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(9.dp).background(
                    if (documentStatus == DocumentSyncStatus.READY) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error, CircleShape))
                Text("Documents/Takt", style = MaterialTheme.typography.titleMedium)
            }
            Text(
                when (documentStatus) {
                    DocumentSyncStatus.READY -> t("Connected · synchronization is active")
                    DocumentSyncStatus.CONFLICT -> t("The local data and the copy in Documents/Takt are different")
                    DocumentSyncStatus.ERROR -> t("Synchronization error. Check folder access.")
                    DocumentSyncStatus.DISCONNECTED -> t("Not connected")
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (unmigratedMaterials > 0) {
                Text(
                    t("Could not copy ${unmigratedMaterials} older materials. Add them again while the originals are available."),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            OutlinedButton(onClick = { folderLauncher.launch(android.provider.DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", "primary:Documents")) }, modifier = Modifier.fillMaxWidth()) {
                Text(if (documentStatus == DocumentSyncStatus.DISCONNECTED) t("Connect Documents") else t("Change folder"))
            }
            if (documentStatus == DocumentSyncStatus.CONFLICT) {
                Button(onClick = { reviewDocuments() }, modifier = Modifier.fillMaxWidth()) {
                    Text(t("Review differences with Documents/Takt"))
                }
                OutlinedButton(onClick = {
                    scope.launch {
                        backupMessage = runCatching { documentStore.saveCurrentToDocuments() }
                            .fold({ t("Current data is recorded in Documents/Takt.") }, { t("Save error: ${t(it.message.orEmpty())}") })
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text(t("Replace the copy with phone data")) }
            }
        }
        SectionCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center) {
                Icon(Icons.Outlined.CloudUpload, null, Modifier.size(26.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text(t("Backup"), style = MaterialTheme.typography.titleLarge)
            }
            Button(
                onClick = {
                    exportLauncher.launch("takt-backup-" + LocalDate.now() + ".json")
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) {
                Icon(Icons.Outlined.FileUpload, null)
                Spacer(Modifier.width(10.dp))
                Text(t("Export JSON"), style = MaterialTheme.typography.titleMedium)
            }
            OutlinedButton(
                onClick = {
                    importLauncher.launch(arrayOf("application/json", "text/plain"))
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) {
                Icon(Icons.Outlined.Restore, null)
                Spacer(Modifier.width(10.dp))
                Text(t("Restore from JSON"), style = MaterialTheme.typography.titleMedium)
            }
            backupMessage?.let { message ->
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (message.startsWith(t("Error"))) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
            }
        }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader(
            title = t("Settings"),
            navigation = {
                TaktIconButton(Icons.AutoMirrored.Filled.ArrowBack, t("Back"), onBack)
            },
        )

        SettingsGroup(t("Help")) {
            SettingsNavigationTile(title = t("Tour of Takt"),
                icon = Icons.Outlined.School, onClick = onStartTour)
        }
        SettingsGroup(t("For yourself")) {
        SettingsNavigationTile(
            title = t("Notifications"),
            icon = Icons.Outlined.Notifications,
            onClick = { showNotifications = true },
        )
        SettingsNavigationTile(
            title = t("Appearance"),
            icon = Icons.Outlined.Palette,
            onClick = { showAppearance = true },
        )
        SettingsNavigationTile(
            title = t("Language"),
            icon = Icons.Outlined.Language,
            onClick = { showLanguage = true },
        )
        SettingsNavigationTile(
            title = t("Calendar and schedule"),
            icon = Icons.Outlined.CalendarMonth,
            onClick = { showCalendar = true },
        )
        SettingsNavigationTile(
            title = t("Study periods"),
            icon = Icons.Outlined.DateRange,
            onClick = { showPeriods = true },
        )

        SettingsNavigationTile(
            title = t("Tasks"),
            icon = Icons.Outlined.Checklist,
            onClick = { showTasks = true },
        )

        }
        SettingsGroup(t("Account")) {
        SettingsNavigationTile(
            title = t("University system"),
            subtitle = when {
                uisPreview != null -> t("UIS · changes to review")
                uisSession == com.kpyruy.takt.core.data.uis.UisResult.SECOND_FACTOR -> t("UIS · verification code needed")
                uisSession in setOf(com.kpyruy.takt.core.data.uis.UisResult.INVALID_CREDENTIALS,
                    com.kpyruy.takt.core.data.uis.UisResult.UNAVAILABLE,
                    com.kpyruy.takt.core.data.uis.UisResult.UNEXPECTED_RESPONSE) -> t("UIS · check failed")
                uisImport.error != null -> t("UIS · check failed")
                hasUniversityAccount -> t("UIS · credentials saved")
                else -> t("Local mode")
            },
            icon = Icons.Outlined.AccountCircle,
            onClick = { showUniversityAccount = true },
        )
        SettingsNavigationTile(
            title = t("Automatic UIS updates"),
            subtitle = t("Progress") + ": " + t(settings.uisAutoSync.progress.label()) + " · " +
                t("Timetable") + ": " + t(settings.uisAutoSync.timetable.label()),
            icon = Icons.Outlined.DateRange,
            onClick = { showUisAutoSync = true },
        )

        }
        SettingsSectionTitle(t("Assessment"))
        SectionCard {
            Text(t("Standard scale"), style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("A" to "92+", "B" to "83+", "C" to "74+", "D" to "65+", "E" to "56+").forEach { (grade, threshold) ->
                    Column(Modifier.weight(1f).padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(grade, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Text(threshold, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Text(t("FX · below 56%"), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        SettingsGroup(t("App")) {
        SettingsNavigationTile(
            title = t("App updates"),
            subtitle = if (releaseState.availableUpdate != null) t("Update available") else "Takt " + releaseState.installedVersion,
            icon = Icons.Outlined.SystemUpdate,
            onClick = { showAppUpdates = true },
        )

        }
        SettingsGroup(t("Data")) {
            SettingsNavigationTile("Documents/Takt", Icons.Outlined.Folder,
                onClick = { backupMessage = null; showDocuments = true },
                subtitle = t(if (documentStatus == DocumentSyncStatus.READY) "Connected" else "Not connected"))
        }
    }
}
