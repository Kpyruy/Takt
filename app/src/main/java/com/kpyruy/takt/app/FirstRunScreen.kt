package com.kpyruy.takt.app

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
import androidx.compose.runtime.getValue
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
import com.kpyruy.takt.core.model.AppThemeMode
import com.kpyruy.takt.core.model.ThemeFamily
import com.kpyruy.takt.core.ui.theme.TaktTheme
import com.kpyruy.takt.core.ui.theme.taktThemePreviewColors
import com.kpyruy.takt.feature.settings.AppearanceLivePreview
import kotlinx.coroutines.launch

private data class TourPage(val title: String, val body: String, val icon: ImageVector)

private val tourPages = listOf(
    TourPage("Сьогодні", "Пари та задачі на вибраний день. Натисни пару, щоб відкрити предмет.", Icons.Outlined.Today),
    TourPage("Календар", "Дивись розклад за днем, тижнем або місяцем. Затисни «Тиждень» для вибору таймтейблу чи списку. Двічі натисни активний режим, щоб повернутися до сьогодні.", Icons.Outlined.CalendarMonth),
    TourPage("Предмети", "Спочатку створи предмет із власною назвою та кодом. До нього привʼязуються пари й задачі.", Icons.Outlined.MenuBook),
    TourPage("Прогрес", "Позначай активні й здані предмети, обирай поточний семестр та стеж за балами.", Icons.Outlined.School),
    TourPage("Налаштування", "Тут можна змінити вигляд, розклад і задачі та підключити резервну копію. Після додавання предметів обери поточний семестр у Прогресі, а в Налаштування → Періоди навчання вкажи дати занять та екзаменів.", Icons.Outlined.Settings),
)

@Composable
internal fun FirstRunScreen(
    settings: AppSettings,
    onFinish: (AppSettings) -> Unit,
    onRestore: suspend (Uri) -> String?,
) {
    var draft by remember { mutableStateOf(settings) }
    var step by rememberSaveable { mutableIntStateOf(0) }
    var tourIndex by rememberSaveable { mutableIntStateOf(0) }
    var restoreMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) scope.launch { restoreMessage = onRestore(uri) }
    }
    BackHandler(step != 0) {
        if (step == 2 && tourIndex > 0) tourIndex-- else step = if (step == 2) 1 else 0
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
                                    1 -> { step = 2; tourIndex = 0 }
                                    else -> if (tourIndex < tourPages.lastIndex) tourIndex++ else onFinish(draft)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("onboarding-primary"),
                        ) {
                            Text(when (step) { 0 -> "Далі"; 1 -> "Показати короткий тур";
                                else -> if (tourIndex == tourPages.lastIndex) "Почати" else "Далі" })
                        }
                        if (step == 1 || step == 2) {
                            TextButton(onClick = { onFinish(draft) }, modifier = Modifier.fillMaxWidth()) {
                                Text(if (step == 1) "Почати без туру" else "Пропустити тур")
                            }
                        }
                    }
                }
            },
        ) { insets ->
            Column(Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text("TAKT  ·  ${step + 1} / 3", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                when (step) {
                    0 -> {
                        Text("Твій Takt", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                        Text("Обери вигляд. Розклад почнеться з твоїх предметів.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        AppearanceLivePreview(draft)
                        Text("Тема", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(AppThemeMode.SYSTEM to "Як телефон", AppThemeMode.LIGHT to "Світла",
                                AppThemeMode.DARK to "Темна").forEach { (mode, label) ->
                                FilterChip(selected = draft.themeMode == mode, onClick = { draft = draft.copy(themeMode = mode) },
                                    label = { Text(label) }, modifier = Modifier.weight(1f).testTag("onboarding-theme-${mode.name}"))
                            }
                        }
                        Text("Колір", style = MaterialTheme.typography.titleMedium)
                        val dark = when (draft.themeMode) {
                            AppThemeMode.SYSTEM -> isSystemInDarkTheme()
                            AppThemeMode.LIGHT -> false
                            AppThemeMode.DARK -> true
                        }
                        listOf(ThemeFamily.BLUE to "Синя", ThemeFamily.GREEN to "Зелена",
                            ThemeFamily.PURPLE to "Фіолетова", ThemeFamily.WARM to "Тепла",
                            ThemeFamily.MONOCHROME to "Монохром").forEach { (family, label) ->
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
                        TextButton(onClick = { folderPicker.launch(null) }) { Text("Відновити з Documents/Takt") }
                        restoreMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                    1 -> {
                        Text("Показати, що де?", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                        Text("Короткий тур покаже головні розділи й налаштування. Його можна пропустити.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Surface(shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.primaryContainer) {
                            Row(Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top) {
                                Icon(Icons.Outlined.DateRange, null, tint = MaterialTheme.colorScheme.primary)
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Перед стартом розкладу", style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold)
                                    Text("Додай предмети, обери поточний семестр у Прогресі та задай дати занять і екзаменів у Налаштування → Періоди навчання.",
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
                        Text("Швидкий тур", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                        LinearProgressIndicator(progress = { (tourIndex + 1) / tourPages.size.toFloat() },
                            modifier = Modifier.fillMaxWidth())
                        TourCard(tourPages[tourIndex])
                        Text("${tourIndex + 1} із ${tourPages.size}", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
