package com.kpyruy.takt.app

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
    TourPage(t("Сьогодні"), t("Пари та задачі на вибраний день. Натисни пару, щоб відкрити предмет."), Icons.Outlined.Today),
    TourPage(t("Календар"), t("Дивись розклад за днем, тижнем або місяцем. Затисни «Тиждень» для вибору таймтейблу чи списку. Двічі натисни активний режим, щоб повернутися до сьогодні."), Icons.Outlined.CalendarMonth),
    TourPage(t("Предмети"), t("Активні предмети, задачі й нотатки в одному місці."), Icons.Outlined.MenuBook),
    TourPage(t("Прогрес"), t("Позначай активні й здані предмети, обирай поточний семестр та стеж за балами."), Icons.Outlined.School),
    TourPage(t("Налаштування"), t("Тут можна змінити вигляд, розклад і задачі та підключити резервну копію. Після додавання предметів обери поточний семестр у Прогресі, а в Налаштування → Періоди навчання вкажи дати занять та екзаменів."), Icons.Outlined.Settings),
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
                            enabled = !finishing && (step != 1 || !useUIS || hasUniversityAccount),
                            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("onboarding-primary"),
                        ) {
                            Text(if (step < 2) t("Далі") else t("Почати екскурсію"))
                        }
                        if (step == 2) {
                            TextButton(onClick = { finishing = true; onFinish(draft, false) }, enabled = !finishing,
                                modifier = Modifier.fillMaxWidth()) {
                                Text(t("Почати без туру"))
                            }
                        }
                    }
            },
        ) { insets ->
            Column(Modifier.fillMaxSize().padding(insets).consumeWindowInsets(insets).verticalScroll(pageScroll)
                .padding(horizontal = 20.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text("TAKT  ·  ${step + 1} / 3", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                when (step) {
                    0 -> {
                        Text(t("Твій Takt"), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                        Text(t("Обери вигляд. Розклад почнеться з твоїх предметів."),
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(t("Мова"), style = MaterialTheme.typography.titleMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                AppLanguage.ENGLISH to "English",
                                AppLanguage.UKRAINIAN to t("Українська"),
                                AppLanguage.SLOVAK to "Slovenčina",
                            ).forEach { (language, label) ->
                                FilterChip(
                                    selected = draft.language == language,
                                    onClick = { draft = draft.copy(language = language) },
                                    label = { Text(label) },
                                    modifier = Modifier.testTag("onboarding-language-${language.name}"),
                                )
                            }
                        }
                        Text(t("Тема"), style = MaterialTheme.typography.titleMedium)
                        com.kpyruy.takt.feature.settings.AppearanceLightingChoices(
                            draft, { draft = it }, tagPrefix = "onboarding-theme")
                        Text(t("Колір"), style = MaterialTheme.typography.titleMedium)
                        com.kpyruy.takt.feature.settings.AppearanceThemeChoices(
                            draft, { draft = it }, tagPrefix = "onboarding-color")
                        TextButton(onClick = { folderPicker.launch(null) }) { Text(t("Відновити з Documents/Takt")) }
                        restoreMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                    1 -> {
                        Text(t("Як користуватися Takt?"), style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold)
                        Text(t("Працюй локально або підключи UIS STU."),
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = !useUIS, onClick = {
                                if (hasUniversityAccount) {
                                    authenticateDevice(t("Видалити дані UIS")) { result ->
                                        if (result == DeviceAuthenticationResult.SUCCESS) {
                                            scope.launch {
                                                runCatching { universityAccountRepository.remove() }
                                                    .onSuccess { useUIS = false; accountMessage = null }
                                                    .onFailure { accountMessage = t("Не вдалося видалити дані UIS") }
                                            }
                                        } else accountMessage = t("Підтвердження скасовано")
                                    }
                                } else useUIS = false
                            },
                                label = { Text(t("Локально")) }, modifier = Modifier.testTag("onboarding-local"))
                            FilterChip(selected = useUIS, onClick = { useUIS = true },
                                label = { Text("UIS") }, modifier = Modifier.testTag("onboarding-uis"))
                        }
                        if (useUIS) {
                            Text(t("Після збереження даних UIS Takt проситиме біометрію або PIN телефона при кожному запуску."),
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            UniversityAccountForm(universityAccountRepository, authenticateDevice)
                        }
                        accountMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                    2 -> {
                        Text(t("Показати, що де?"), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                        Text(t("Екскурсія відкриє екрани Takt і підсвітить головне. Її можна зупинити будь-коли."),
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Surface(shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.primaryContainer) {
                            Row(Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top) {
                                Icon(Icons.Outlined.DateRange, null, tint = MaterialTheme.colorScheme.primary)
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(t("Перед стартом розкладу"), style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold)
                                    Text(t("Додай предмети, обери поточний семестр у Прогресі та задай дати занять і екзаменів у Налаштування → Періоди навчання."),
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
