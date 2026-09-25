package com.kpyruy.takt.feature.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.kpyruy.takt.core.model.AppSettings
import com.kpyruy.takt.core.model.AppThemeMode
import com.kpyruy.takt.core.model.CardAppearance
import com.kpyruy.takt.core.model.ThemeFamily
import com.kpyruy.takt.core.ui.components.ScreenHeader
import com.kpyruy.takt.core.ui.components.SectionCard
import com.kpyruy.takt.core.ui.theme.TaktTheme
import com.kpyruy.takt.core.ui.theme.taktThemePreviewColors

@Composable
internal fun AppearanceSettingsPage(
    settings: AppSettings,
    onBack: () -> Unit,
    onApply: (AppSettings) -> Unit,
) {
    BackHandler(onBack = onBack)
    var draft by remember { mutableStateOf(settings) }
    val changed = draft.themeMode != settings.themeMode ||
        draft.themeFamily != settings.themeFamily || draft.cardAppearance != settings.cardAppearance

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Surface(shadowElevation = 10.dp) {
                Button(
                    onClick = { onApply(draft) },
                    enabled = changed,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).height(54.dp),
                ) { Text(if (changed) "Застосувати вигляд" else "Вигляд застосовано") }
            }
        },
    ) { insets ->
        Column(
            Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            ScreenHeader(
                title = "Твій вигляд",
                navigation = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Назад") }
                },
            )
            SettingsSectionTitle("Так це виглядатиме")
            AppearanceLivePreview(draft)
            SettingsSectionTitle("Освітлення")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    AppThemeMode.SYSTEM to "Як телефон",
                    AppThemeMode.LIGHT to "Світла",
                    AppThemeMode.DARK to "Темна",
                ).forEach { (mode, label) ->
                    SelectablePill(
                        label = label,
                        selected = draft.themeMode == mode,
                        modifier = Modifier.weight(1f),
                        onClick = { draft = draft.copy(themeMode = mode) },
                    )
                }
            }
            SettingsSectionTitle("Палітра")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    ThemeFamily.BLUE to "Синя",
                    ThemeFamily.GREEN to "Зелена",
                    ThemeFamily.PURPLE to "Фіолетова",
                    ThemeFamily.WARM to "Тепла",
                    ThemeFamily.MONOCHROME to "Монохром",
                ).forEach { (family, label) ->
                    val selected = draft.themeFamily == family
                    val dark = when (draft.themeMode) {
                        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
                        AppThemeMode.LIGHT -> false
                        AppThemeMode.DARK -> true
                    }
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { draft = draft.copy(themeFamily = family) },
                        shape = MaterialTheme.shapes.medium,
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 62.dp).padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                taktThemePreviewColors(family, dark).take(3).forEach { color ->
                                    Box(Modifier.size(18.dp).background(color, CircleShape))
                                }
                            }
                            Text(label, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                            if (selected) Icon(Icons.Outlined.Check, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            SettingsSectionTitle("Стиль карток")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    CardAppearance.ELEVATED to "Підняті",
                    CardAppearance.TONAL_FILLED to "Заливка",
                ).forEach { (appearance, label) ->
                    CardStyleOption(
                        settings = draft,
                        appearance = appearance,
                        label = label,
                        modifier = Modifier.weight(1f),
                        onClick = { draft = draft.copy(cardAppearance = appearance) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AppearanceLivePreview(settings: AppSettings) {
    TaktTheme(settings) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("СЬОГОДНІ", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text("Пʼятниця, 25 вересня", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                SectionCard {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(4.dp, 54.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        Column {
                            Text("09:00 — 10:30", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text("Теорія права", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("Лекція · ауд. 302", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(28.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Check, null, tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp))
                        }
                        Column {
                            Text("Підготувати конспект", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("TPAR_6B · завтра", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CardStyleOption(
    settings: AppSettings,
    appearance: CardAppearance,
    label: String,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val selected = settings.cardAppearance == appearance
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TaktTheme(settings.copy(cardAppearance = appearance)) {
                Surface(color = MaterialTheme.colorScheme.background, shape = MaterialTheme.shapes.small) {
                    Box(Modifier.padding(8.dp)) {
                        SectionCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp)) {
                            Box(Modifier.fillMaxWidth(0.7f).height(6.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                            Box(Modifier.fillMaxWidth().height(5.dp).background(MaterialTheme.colorScheme.outlineVariant, CircleShape))
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                if (selected) Icon(Icons.Outlined.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun SelectablePill(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = MaterialTheme.shapes.small,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(Modifier.heightIn(min = 46.dp).padding(horizontal = 6.dp), contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.labelMedium,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
        }
    }
}
