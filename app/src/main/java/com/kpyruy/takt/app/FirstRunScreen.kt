package com.kpyruy.takt.app

import com.kpyruy.takt.core.ui.i18n.t

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
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
import com.kpyruy.takt.core.model.AppThemeMode
import com.kpyruy.takt.core.model.ThemeFamily
import com.kpyruy.takt.core.ui.theme.TaktTheme
import com.kpyruy.takt.core.ui.i18n.TaktI18n
import com.kpyruy.takt.core.ui.theme.taktThemePreviewColors
import com.kpyruy.takt.feature.settings.AppearanceLivePreview
import com.kpyruy.takt.feature.settings.DeviceAuthenticationRequest
import com.kpyruy.takt.feature.settings.UniversityAccountForm
import kotlinx.coroutines.launch

private data class TourPage(val title: String, val body: String, val icon: ImageVector)

private fun localizedTourPages() = listOf(
    TourPage(t("Сьогодні"), t("Пари та задачі на вибраний день. Натисни пару, щоб відкрити предмет."), Icons.Outlined.Today),
    TourPage(t("Календар"), t("Дивись розклад за днем, тижнем або місяцем. Затисни «Тиждень» для вибору таймтейблу чи списку. Двічі натисни активний режим, щоб повернутися до сьогодні."), Icons.Outlined.CalendarMonth),
    TourPage(t("Предмети"), t("Спочатку створи предмет із власною назвою та кодом. До нього привʼязуються пари й задачі."), Icons.Outlined.MenuBook),
    TourPage(t("Прогрес"), t("Позначай активні й здані предмети, обирай поточний семестр та стеж за балами."), Icons.Outlined.School),
    TourPage(t("Налаштування"), t("Тут можна змінити вигляд, розклад і задачі та підключити резервну копію. Після додавання предметів обери поточний семестр у Прогресі, а в Налаштування → Періоди навчання вкажи дати занять та екзаменів."), Icons.Outlined.Settings),
)

@Composable
internal fun FirstRunScreen(
    settings: AppSettings,
    universityAccountRepository: UniversityAccountRepository,
    authenticateDevice: DeviceAuthenticationRequest,
    onFinish: (AppSettings) -> Unit,
    onRestore: suspend (Uri) -> String?,
) {
    var draft by remember { mutableStateOf(settings) }
    SideEffect { TaktI18n.use(draft.language) }
    val tourPages = remember(TaktI18n.language) { localizedTourPages() }
    var step by rememberSaveable { mutableIntStateOf(0) }
    var tourIndex by rememberSaveable { mutableIntStateOf(0) }
    var restoreMessage by remember { mutableStateOf<String?>(null) }
    val hasUniversityAccount by universityAccountRepository.hasAccount.collectAsStateWithLifecycle()
    var useUIS by rememberSaveable { mutableStateOf(universityAccountRepository.hasAccount.value) }
    var accountMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) scope.launch { restoreMessage = onRestore(uri) }
    }
    BackHandler(step != 0) {
        if (step == 3 && tourIndex > 0) tourIndex-- else step--
    }

    TaktTheme(draft) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                Surface(shadowElevation = 10.dp) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Button(
                            onClick = {
                                when (step) {
                                    0 -> step = 1
                                    1 -> step = 2
                                    2 -> { step = 3; tourIndex = 0 }
                                    else -> if (tourIndex < tourPages.lastIndex) tourIndex++ else onFinish(draft)
                                }
                            },
                            enabled = step != 1 || !useUIS || hasUniversityAccount,
                            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("onboarding-primary"),
                        ) {
                            Text(when (step) { 0, 1 -> t("Далі"); 2 -> t("Показати короткий тур");
                                else -> if (tourIndex == tourPages.lastIndex) t("Почати") else t("Далі") })
                        }
                        if (step == 2 || step == 3) {
                            TextButton(onClick = { onFinish(draft) }, modifier = Modifier.fillMaxWidth()) {
                                Text(if (step == 2) t("Почати без туру") else t("Пропустити тур"))
                            }
                        }
                    }
                }
            },
        ) { insets ->
            Column(Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text("TAKT  ·  ${step + 1} / 4", style = MaterialTheme.typography.labelLarge,
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
                        AppearanceLivePreview(draft)
                        Text(t("Тема"), style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(AppThemeMode.SYSTEM to t("Як телефон"), AppThemeMode.LIGHT to t("Світла"),
                                AppThemeMode.DARK to t("Темна")).forEach { (mode, label) ->
                                FilterChip(selected = draft.themeMode == mode, onClick = { draft = draft.copy(themeMode = mode) },
                                    label = { Text(label) }, modifier = Modifier.weight(1f).testTag("onboarding-theme-${mode.name}"))
                            }
                        }
                        Text(t("Колір"), style = MaterialTheme.typography.titleMedium)
                        val dark = when (draft.themeMode) {
                            AppThemeMode.SYSTEM -> isSystemInDarkTheme()
                            AppThemeMode.LIGHT -> false
                            AppThemeMode.DARK -> true
                        }
                        listOf(ThemeFamily.BLUE to t("Синя"), ThemeFamily.GREEN to t("Зелена"),
                            ThemeFamily.PURPLE to t("Фіолетова"), ThemeFamily.WARM to t("Тепла"),
                            ThemeFamily.MONOCHROME to t("Монохром")).forEach { (family, label) ->
                            val selected = draft.themeFamily == family
                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable { draft = draft.copy(themeFamily = family) }
                                    .testTag("onboarding-color-${family.name}"),
                                shape = MaterialTheme.shapes.medium,
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                            ) {
                                Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                        taktThemePreviewColors(family, dark).take(3).forEach { color ->
                                            Box(Modifier.size(19.dp).background(color, CircleShape))
                                        }
                                    }
                                    Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                                    if (selected) Icon(Icons.Outlined.Check, null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                        TextButton(onClick = { folderPicker.launch(null) }) { Text(t("Відновити з Documents/Takt")) }
                        restoreMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                    1 -> {
                        Text(t("Як користуватися Takt?"), style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold)
                        Text(t("Працюй повністю локально або збережи дані UIS для майбутнього підключення."),
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = !useUIS, onClick = {
                                if (hasUniversityAccount) {
                                    authenticateDevice(t("Видалити дані UIS")) { result ->
                                        if (result == DeviceAuthenticationResult.SUCCESS) {
                                            runCatching { universityAccountRepository.remove() }
                                                .onSuccess { useUIS = false; accountMessage = null }
                                                .onFailure { accountMessage = t("Не вдалося видалити дані UIS") }
                                        } else accountMessage = t("Підтвердження скасовано")
                                    }
                                } else useUIS = false
                            },
                                label = { Text(t("Локально")) }, modifier = Modifier.testTag("onboarding-local"))
                            FilterChip(selected = useUIS, onClick = { useUIS = true },
                                label = { Text("UIS") }, modifier = Modifier.testTag("onboarding-uis"))
                        }
                        if (useUIS) {
                            Text(t("Вхід у UIS поки не виконується. Після збереження даних Takt проситиме захист телефона при кожному запуску."),
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            UniversityAccountForm(universityAccountRepository, authenticateDevice)
                        }
                        accountMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                    2 -> {
                        Text(t("Показати, що де?"), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                        Text(t("Короткий тур покаже головні розділи й налаштування. Його можна пропустити."),
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
                    else -> {
                        Text(t("Швидкий тур"), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                        LinearProgressIndicator(progress = { (tourIndex + 1) / tourPages.size.toFloat() },
                            modifier = Modifier.fillMaxWidth())
                        TourCard(tourPages[tourIndex])
                        Text(t("${tourIndex + 1} із ${tourPages.size}"), color = MaterialTheme.colorScheme.onSurfaceVariant)
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

@Composable
private fun TourCard(page: TourPage) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(page.icon, null, modifier = Modifier.size(34.dp), tint = MaterialTheme.colorScheme.primary)
            Text(page.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(page.body, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
