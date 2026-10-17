package com.kpyruy.takt.feature.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface as SolidSurface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.*
import com.kpyruy.takt.core.ui.components.ScreenHeader
import com.kpyruy.takt.core.ui.components.TaktIconButton
import com.kpyruy.takt.core.ui.components.TaktPrimaryButton as Button
import com.kpyruy.takt.core.ui.components.TaktSurface as Surface
import com.kpyruy.takt.core.ui.i18n.t
import com.kpyruy.takt.core.ui.theme.TaktTheme

@Composable
internal fun AppearanceSettingsPage(settings: AppSettings, onBack: () -> Unit, onApply: (AppSettings) -> Unit) {
    BackHandler(onBack = onBack)
    var wallpaperDraft by rememberSaveable { mutableStateOf(settings.customWallpaper) }
    val draftSaver = remember(settings) { listSaver<AppSettings, String>(
        save = { value -> listOf(value.themeMode.name, value.themeFamily.name, value.cardAppearance.name,
            value.selectedCustomThemeId.orEmpty(), value.customWallpaper.orEmpty()) + value.customThemes.flatMap {
                listOf(it.id, it.name, it.primaryHex, it.secondaryHex.orEmpty(), it.tertiaryHex.orEmpty())
            } },
        restore = { values -> settings.copy(themeMode = AppThemeMode.valueOf(values[0]),
            themeFamily = ThemeFamily.valueOf(values[1]), cardAppearance = CardAppearance.valueOf(values[2]),
            selectedCustomThemeId = values[3].ifEmpty { null }, customWallpaper = values[4].ifEmpty { null },
            customThemes = values.drop(5).chunked(5).map { CustomColorTheme(it[0], it[1], it[2],
                it[3].ifEmpty { null }, it[4].ifEmpty { null }) }).normalizedAppearance() }
    ) }
    var draft by rememberSaveable(stateSaver = draftSaver) { mutableStateOf(settings.normalizedAppearance().copy(customWallpaper = wallpaperDraft)) }
    var importingWallpaper by remember { mutableStateOf(false) }
    val changed = draft.customWallpaper != settings.customWallpaper || draft.themeMode != settings.themeMode || draft.themeFamily != settings.themeFamily ||
        draft.cardAppearance != settings.cardAppearance || draft.customThemes != settings.customThemes ||
        draft.selectedCustomThemeId != settings.selectedCustomThemeId
    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Surface(shadowElevation = 10.dp) {
                Button(onClick = { onApply(draft.normalizedAppearance()) }, enabled = changed && !importingWallpaper,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).height(54.dp)) {
                    Text(if (changed) t("Застосувати вигляд") else t("Вигляд застосовано"))
                }
            }
        },
    ) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ScreenHeader(title = t("Твій вигляд"), navigation = {
                TaktIconButton(Icons.AutoMirrored.Outlined.ArrowBack, t("Назад"), onBack)
            })
            SettingsSectionTitle(t("Освітлення"))
            AppearanceLightingChoices(draft, { draft = it })
            SettingsSectionTitle(t("Палітра"))
            AppearanceThemeChoices(draft, { draft = it })
            AppearanceWallpaperMenu(draft, settings.customWallpaper, { draft = it; wallpaperDraft = it.customWallpaper }, { importingWallpaper = it })
            SettingsSectionTitle(t("Стиль карток"))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    Triple(CardAppearance.ELEVATED, t("Підняті"), t("Суцільний фон і м’яка тінь")),
                    Triple(CardAppearance.TONAL_FILLED, t("Заливка"), t("Кольоровий фон без тіні")),
                    Triple(CardAppearance.GLASS, t("Скло"), t("Напівпрозорі картки з відблиском")),
                ).forEach { (appearance, label, description) ->
                    CardStyleOption(draft, appearance, label, description) { draft = draft.copy(cardAppearance = appearance) }
                }
            }
        }
    }
}

/** Shared appearance controls keep setup's language and navigation ownership with its caller. */
@Composable
fun AppearanceLightingChoices(settings: AppSettings, onChange: (AppSettings) -> Unit, tagPrefix: String = "appearance-theme") {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            Triple(AppThemeMode.SYSTEM, t("Системний"), Icons.Outlined.BrightnessAuto),
            Triple(AppThemeMode.LIGHT, t("Світла"), Icons.Outlined.LightMode),
            Triple(AppThemeMode.DARK, t("Темна"), Icons.Outlined.DarkMode),
        ).forEach { (mode, label, icon) ->
            LightingOption(label, icon, settings.themeMode == mode,
                Modifier.weight(1f).testTag("$tagPrefix-${mode.name}")) { onChange(settings.copy(themeMode = mode)) }
        }
    }
}

@Composable
private fun CardStyleOption(settings: AppSettings, appearance: CardAppearance, label: String, description: String, onClick: () -> Unit) {
    val selected = settings.cardAppearance == appearance
    SolidSurface(modifier = Modifier.fillMaxWidth().selectable(selected, role = Role.RadioButton, onClick = onClick)
        .testTag("appearance-card-${appearance.name}"), shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface.copy(alpha = .55f),
        border = BorderStroke(if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            TaktTheme(settings.copy(cardAppearance = appearance)) {
                // The same colored backdrop makes the three material differences visible.
                Box(Modifier.size(100.dp, 76.dp).clip(MaterialTheme.shapes.small).background(
                    Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = .6f),
                        MaterialTheme.colorScheme.tertiary.copy(alpha = .6f), MaterialTheme.colorScheme.secondary.copy(alpha = .6f))))) {
                    Box(Modifier.width(15.dp).fillMaxHeight().padding(start = 5.dp).background(Color.White.copy(alpha = .45f)))
                    val colors = MaterialTheme.colorScheme
                    val dark = colors.background.luminance() < .3f
                    val fill = when (appearance) {
                        CardAppearance.ELEVATED -> colors.surface
                        CardAppearance.TONAL_FILLED -> colors.primaryContainer
                        CardAppearance.GLASS -> if (dark) colors.surface.copy(alpha = .32f) else Color.White.copy(alpha = .5f)
                    }
                    SolidSurface(Modifier.fillMaxSize().padding(10.dp), shape = MaterialTheme.shapes.small,
                        color = fill, shadowElevation = if (appearance == CardAppearance.ELEVATED) 8.dp else 0.dp,
                        border = if (appearance == CardAppearance.GLASS) BorderStroke(1.dp, Color.White.copy(alpha = .85f)) else null) {
                        Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.fillMaxWidth(.6f).height(6.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                            Box(Modifier.fillMaxWidth().height(5.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = .18f), CircleShape))
                            Box(Modifier.fillMaxWidth(.8f).height(5.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = .12f), CircleShape))
                        }
                    }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(label, style = MaterialTheme.typography.titleSmall)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (selected) Icon(Icons.Outlined.Check, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun LightingOption(label: String, icon: ImageVector, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier = modifier.selectable(selected, role = Role.RadioButton, onClick = onClick),
        shape = MaterialTheme.shapes.small,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 4.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, Modifier.size(22.dp), tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            Text(label, style = MaterialTheme.typography.labelMedium,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
        }
    }
}
