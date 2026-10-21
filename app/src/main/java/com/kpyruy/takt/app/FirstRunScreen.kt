package com.kpyruy.takt.app

import com.kpyruy.takt.core.model.uiMessage
import com.kpyruy.takt.core.model.UiKey
import com.kpyruy.takt.core.ui.i18n.t

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import com.kpyruy.takt.core.ui.components.TaktPrimaryButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import com.kpyruy.takt.core.ui.components.TaktSurface as Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.draw.clipToBounds
import com.kpyruy.takt.core.ui.components.TaktSheetMaterialShape
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.AppSettings
import com.kpyruy.takt.core.model.DeviceAuthenticationResult
import com.kpyruy.takt.core.data.UniversityAccountRepository
import com.kpyruy.takt.core.model.AppLanguage
import com.kpyruy.takt.core.ui.theme.TaktTheme
import com.kpyruy.takt.core.ui.components.TaktGlassBackground
import com.kpyruy.takt.core.ui.components.GlassSurface
import com.kpyruy.takt.core.ui.components.glassSurface
import androidx.compose.ui.graphics.Color
import com.kpyruy.takt.core.ui.i18n.TaktI18n
import com.kpyruy.takt.feature.settings.DeviceAuthenticationRequest
import com.kpyruy.takt.feature.settings.UniversityAccountForm
import kotlinx.coroutines.launch

private data class TourPage(val title: String, val body: String, val icon: ImageVector)

private fun localizedTourPages() = listOf(
    TourPage(t(UiKey.TEXT_TODAY), t(UiKey.TEXT_CLASSES_AND_TASKS_FOR_THE_SELECTED_DAY_TAP_A_CLASS_TO_OPEN_ITS_SUBJECT), Icons.Outlined.Today),
    TourPage(t(UiKey.TEXT_CALENDAR), t(UiKey.TEXT_VIEW_YOUR_SCHEDULE_BY_DAY_WEEK_OR_MONTH_LONG_PRESS_WEEK_TO_SWI_1FF5CEB8), Icons.Outlined.CalendarMonth),
    TourPage(t(UiKey.TEXT_SUBJECTS), t(UiKey.TEXT_ACTIVE_SUBJECTS_TASKS_AND_NOTES_IN_ONE_PLACE), Icons.Outlined.MenuBook),
    TourPage(t(UiKey.TEXT_PROGRESS), t(UiKey.TEXT_MARK_ACTIVE_AND_PASSED_SUBJECTS_CHOOSE_THE_CURRENT_SEMESTER_AN_DBEE6EB5), Icons.Outlined.School),
    TourPage(t(UiKey.TEXT_SETTINGS), t(UiKey.TEXT_HERE_YOU_CAN_CHANGE_THE_VIEW_SCHEDULE_AND_TASKS_AND_CONNECT_A_4A6A5418), Icons.Outlined.Settings),
)

@Composable
internal fun FirstRunScreen(
    settings: AppSettings,
    universityAccountRepository: UniversityAccountRepository,
    authenticateDevice: DeviceAuthenticationRequest,
    onFinish: (AppSettings, Boolean) -> Unit,
    onRestore: suspend (Uri) -> String?,
    onSystemBarBackground: (Color) -> Unit = {},
) {
    var draft by remember { mutableStateOf(settings) }
    SideEffect { TaktI18n.use(draft.language) }
    val tourPages = remember(TaktI18n.language) { localizedTourPages() }
    var step by rememberSaveable { mutableIntStateOf(-1) }
    var finishing by remember { mutableStateOf(false) }
    var restoreMessage by remember { mutableStateOf<String?>(null) }
    val hasUniversityAccount by universityAccountRepository.hasAccount.collectAsStateWithLifecycle()
    val initialImportComplete by universityAccountRepository.initialImportComplete.collectAsStateWithLifecycle()
    val importProgress by universityAccountRepository.importProgress.collectAsStateWithLifecycle()
    var useUIS by rememberSaveable { mutableStateOf(universityAccountRepository.hasAccount.value) }
    var accountMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(step) {
        if (step != 1) { focusManager.clearFocus(force = true); keyboard?.hide() }
    }
    val pageScroll = key(step) { rememberScrollState() }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) scope.launch { restoreMessage = onRestore(uri) }
    }
    BackHandler(step > -1) {
        step--
    }

    TaktTheme(draft) {
        val barBackground = MaterialTheme.colorScheme.background
        SideEffect { onSystemBarBackground(barBackground) }
        if (step == -1) {
            TaktWelcomeScreen(
                onConnect = { useUIS = true; step = 0 },
                onLocal = { useUIS = false; step = 0 },
            )
            return@TaktTheme
        }
        TaktGlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                    Column(Modifier.fillMaxWidth().clipToBounds().glassSurface(TaktSheetMaterialShape, extendBackdrop = step == 2)
                        .testTag("onboarding-footer").navigationBarsPadding()
                        .padding(horizontal = 20.dp).padding(top = if (step == 2) 16.dp else 12.dp,
                            bottom = if (step == 2) 28.dp else 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        TaktPrimaryButton(
                            onClick = {
                                focusManager.clearFocus(force = true)
                                keyboard?.hide()
                                when (step) {
                                    0 -> step = 1
                                    1 -> step = 2
                                    else -> { finishing = true; onFinish(draft, true) }
                                }
                            },
                            enabled = !finishing && (step != 1 || !useUIS ||
                                hasUniversityAccount && initialImportComplete && !importProgress.running),
                            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("onboarding-primary"),
                        ) {
                            Text(if (step < 2) t(UiKey.TEXT_NEXT) else t(UiKey.TEXT_START_THE_TOUR))
                        }
                        if (step == 2) {
                            TextButton(onClick = { finishing = true; onFinish(draft, false) }, enabled = !finishing,
                                modifier = Modifier.fillMaxWidth()) {
                                Text(t(UiKey.TEXT_START_WITHOUT_A_TOUR))
                            }
                        }
                    }
            },
        ) { insets ->
            Column(Modifier.fillMaxSize().padding(insets).consumeWindowInsets(insets).verticalScroll(pageScroll)
                .padding(horizontal = 20.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text(t(uiMessage(UiKey.FORMAT_ONBOARDING_STEP, step + 1)), style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                when (step) {
                    0 -> {
                        Text(t(UiKey.TEXT_YOUR_TAKT), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                        Text(t(UiKey.TEXT_CHOOSE_AN_APPEARANCE_YOUR_SCHEDULE_STARTS_WITH_YOUR_SUBJECTS),
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(t(UiKey.TEXT_LANGUAGE), style = MaterialTheme.typography.titleMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                AppLanguage.ENGLISH to UiKey.TEXT_ENGLISH,
                                AppLanguage.UKRAINIAN to t(UiKey.TEXT_UKRAINIAN),
                                AppLanguage.SLOVAK to UiKey.TEXT_SLOVEN_INA,
                            ).forEach { (language, label) ->
                                FilterChip(
                                    selected = draft.language == language,
                                    onClick = { draft = draft.copy(language = language) },
                                    label = { Text(t(label)) },
                                    modifier = Modifier.testTag("onboarding-language-${language.name}"),
                                )
                            }
                        }
                        Text(t(UiKey.TEXT_TOPIC), style = MaterialTheme.typography.titleMedium)
                        com.kpyruy.takt.feature.settings.AppearanceLightingChoices(
                            draft, { draft = it }, tagPrefix = "onboarding-theme")
                        Text(t(UiKey.TEXT_COLOR), style = MaterialTheme.typography.titleMedium)
                        com.kpyruy.takt.feature.settings.AppearanceThemeChoices(
                            draft, { draft = it }, tagPrefix = "onboarding-color")
                        TextButton(onClick = { folderPicker.launch(null) }) { Text(t(UiKey.TEXT_RESTORE_FROM_DOCUMENTS_TAKT)) }
                        restoreMessage?.let { Text(t(it), color = MaterialTheme.colorScheme.error) }
                    }
                    1 -> {
                        Text(t(UiKey.TEXT_HOW_WILL_YOU_USE_TAKT), style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold)
                        Text(t(UiKey.TEXT_WORK_LOCALLY_OR_CONNECT_TO_STU_UIS),
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = !useUIS, onClick = {
                                if (hasUniversityAccount) {
                                    authenticateDevice(t(UiKey.TEXT_REMOVE_UIS_CREDENTIALS)) { result ->
                                        if (result == DeviceAuthenticationResult.SUCCESS) {
                                            scope.launch {
                                                runCatching { universityAccountRepository.remove() }
                                                    .onSuccess { useUIS = false; accountMessage = null }
                                                    .onFailure { accountMessage = uiMessage(UiKey.TEXT_COULD_NOT_REMOVE_UIS_CREDENTIALS) }
                                            }
                                        } else accountMessage = uiMessage(UiKey.TEXT_VERIFICATION_CANCELLED)
                                    }
                                } else useUIS = false
                            },
                                label = { Text(t(UiKey.TEXT_LOCALLY)) }, modifier = Modifier.testTag("onboarding-local"))
                            FilterChip(selected = useUIS, onClick = { useUIS = true },
                                label = { Text(t(UiKey.TEXT_UIS)) }, modifier = Modifier.testTag("onboarding-uis"))
                        }
                        if (useUIS) {
                            Text(t(UiKey.TEXT_AFTER_SAVING_UIS_CREDENTIALS_TAKT_REQUIRES_PHONE_BIOMETRICS_OR_D1338C30),
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            UniversityAccountForm(universityAccountRepository, authenticateDevice)
                        }
                        accountMessage?.let { Text(t(it), color = MaterialTheme.colorScheme.error) }
                    }
                    2 -> {
                        Text(t(UiKey.TEXT_SHOW_ME_AROUND), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                        Text(t(UiKey.TEXT_THE_TOUR_OPENS_TAKT_S_SCREENS_AND_HIGHLIGHTS_WHAT_MATTERS_YOU_3A0E1416),
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Surface(shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.primaryContainer) {
                            Row(Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top) {
                                Icon(Icons.Outlined.DateRange, null, tint = MaterialTheme.colorScheme.primary)
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(t(UiKey.TEXT_BEFORE_THE_START_OF_THE_SCHEDULE), style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold)
                                    Text(t(UiKey.TEXT_ADD_SUBJECTS_SELECT_THE_CURRENT_SEMESTER_IN_PROGRESS_AND_SET_T_073AC112),
                                        style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        tourPages.chunked(2).forEach { pair ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                pair.forEach { page -> TourTeaser(page, Modifier.weight(1f)) }
                            }
                        }
                    }

                }
            }
        }
    }
    }
}

@Composable
private fun TourTeaser(page: TourPage, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(page.icon, null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
            Text(page.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}
