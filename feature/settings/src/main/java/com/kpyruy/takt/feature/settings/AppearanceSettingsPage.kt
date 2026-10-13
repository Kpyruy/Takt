package com.kpyruy.takt.feature.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface as SolidSurface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.*
import com.kpyruy.takt.core.ui.components.GlassSurface
import com.kpyruy.takt.core.ui.components.ScreenHeader
import com.kpyruy.takt.core.ui.components.SectionCard
import com.kpyruy.takt.core.ui.components.TaktGlassBackground
import com.kpyruy.takt.core.ui.components.TaktIconButton
import com.kpyruy.takt.core.ui.components.TaktPrimaryButton as Button
import com.kpyruy.takt.core.ui.components.TaktSurface as Surface
import com.kpyruy.takt.core.ui.components.TaktTextField
import com.kpyruy.takt.core.ui.i18n.t
import com.kpyruy.takt.core.ui.theme.TaktTheme
import com.kpyruy.takt.core.ui.theme.taktThemePreviewColors
import java.util.UUID

@Composable
internal fun AppearanceSettingsPage(settings: AppSettings, onBack: () -> Unit, onApply: (AppSettings) -> Unit) {
    BackHandler(onBack = onBack)
    var draft by remember { mutableStateOf(settings.normalizedAppearance()) }
    val changed = draft.themeMode != settings.themeMode || draft.themeFamily != settings.themeFamily ||
        draft.cardAppearance != settings.cardAppearance || draft.customThemes != settings.customThemes ||
        draft.selectedCustomThemeId != settings.selectedCustomThemeId
    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Surface(shadowElevation = 10.dp) {
                Button(onClick = { onApply(draft.normalizedAppearance()) }, enabled = changed,
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
            AppearanceLivePreview(draft)
            SettingsSectionTitle(t("Освітлення"))
            AppearanceLightingChoices(draft, { draft = it })
            SettingsSectionTitle(t("Палітра"))
            AppearanceThemeChoices(draft, { draft = it })
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
fun AppearanceThemeChoices(settings: AppSettings, onChange: (AppSettings) -> Unit, tagPrefix: String = "appearance-color") {
    var editorOpen by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf("") }
    var hex by remember { mutableStateOf("#145BFA") }
    BackHandler(enabled = editorOpen) { editorOpen = false }
    val dark = when (settings.themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            ThemeFamily.BLUE to t("Синя"), ThemeFamily.GREEN to t("Зелена"),
            ThemeFamily.PURPLE to t("Фіолетова"), ThemeFamily.WARM to t("Тепла"),
            ThemeFamily.MONOCHROME to t("Монохром"), ThemeFamily.ROSE to t("Рожева"),
            ThemeFamily.AMBER to t("Бурштинова"), ThemeFamily.CYAN to t("Блакитна"),
        ).chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { (family, label) ->
                    PaletteOption(label, taktThemePreviewColors(family, dark),
                        settings.selectedCustomTheme == null && settings.themeFamily == family,
                        Modifier.weight(1f).testTag("$tagPrefix-${family.name}")) {
                        onChange(settings.copy(themeFamily = family, selectedCustomThemeId = null))
                    }
                }
            }
        }
        Text(t("Власні теми"), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
        settings.customThemes.forEach { theme ->
            val selected = settings.selectedCustomThemeId == theme.id
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                PaletteOption(theme.name, taktThemePreviewColors(settings.copy(selectedCustomThemeId = theme.id), dark),
                    selected, Modifier.weight(1f).testTag("$tagPrefix-custom-${theme.id}")) {
                    onChange(settings.copy(selectedCustomThemeId = theme.id))
                }
                IconButton(onClick = {
                    editingId = theme.id; name = theme.name; hex = theme.primaryHex; editorOpen = true
                }, modifier = Modifier.size(48.dp).testTag("theme-edit-${theme.id}")) {
                    Icon(Icons.Outlined.Edit, t("Редагувати тему") + ": " + theme.name)
                }
                IconButton(onClick = {
                    onChange(settings.copy(customThemes = settings.customThemes.filterNot { it.id == theme.id },
                        selectedCustomThemeId = settings.selectedCustomThemeId.takeUnless { it == theme.id }))
                    if (editingId == theme.id) editorOpen = false
                }, modifier = Modifier.size(48.dp).testTag("theme-delete-${theme.id}")) {
                    Icon(Icons.Outlined.DeleteOutline, t("Видалити тему") + ": " + theme.name)
                }
            }
        }
        TextButton(onClick = {
            editingId = null; name = ""; hex = "#145BFA"; editorOpen = true
        }, modifier = Modifier.heightIn(min = 48.dp).testTag("theme-create")) {
            Icon(Icons.Outlined.Add, null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(t("Створити тему"))
        }
        if (editorOpen) {
            val normalizedHex = normalizeThemeHex(hex)
            val validName = name.trim().isNotEmpty() && name.trim().length <= 40
            val duplicateName = settings.customThemes.any {
                it.id != editingId && it.name.equals(name.trim(), ignoreCase = true)
            }
            val valid = validName && normalizedHex != null && !duplicateName
            SectionCard {
                Text(if (editingId == null) t("Нова тема") else t("Редагувати тему"),
                    style = MaterialTheme.typography.titleMedium)
                TaktTextField(name, { name = it }, Modifier.fillMaxWidth().testTag("theme-name"),
                    label = { Text(t("Назва теми")) }, singleLine = true,
                    isError = name.isNotEmpty() && (!validName || duplicateName),
                    supportingText = { Text(if (duplicateName) t("Тема з такою назвою вже існує") else t("Від 1 до 40 символів")) })
                TaktTextField(hex, { hex = it }, Modifier.fillMaxWidth().testTag("theme-hex"),
                    label = { Text(t("Основний колір")) }, singleLine = true, isError = normalizedHex == null,
                    supportingText = { Text(t("HEX: #RRGGBB, наприклад #B52D67")) },
                    leadingIcon = {
                        val color = normalizedHex?.let { Color(CustomColorTheme("preview", "preview", it).primaryArgb) }
                            ?: MaterialTheme.colorScheme.outline
                        Box(Modifier.size(22.dp).background(color, CircleShape))
                    })
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    listOf("#145BFA", "#147D64", "#7048C8", "#B52D67", "#9B6500", "#007E91").forEach { value ->
                        Box(Modifier.weight(1f).heightIn(min = 48.dp)
                            .selectable(hex.equals(value, true), role = Role.RadioButton, onClick = { hex = value })
                            .semantics { contentDescription = value }
                            .testTag("theme-swatch-${value.drop(1)}"), contentAlignment = Alignment.Center) {
                            Box(Modifier.size(26.dp).background(Color(CustomColorTheme("swatch", "swatch", value).primaryArgb), CircleShape))
                            if (hex.equals(value, true)) Icon(Icons.Outlined.Check, value, Modifier.size(18.dp), tint = Color.White)
                        }
                    }
                }
                if (normalizedHex != null) {
                    val preview = CustomColorTheme(editingId ?: "editor-preview", name.trim().ifEmpty { t("Нова тема") }.take(40), normalizedHex)
                    AppearanceLivePreview(settings.copy(customThemes = settings.customThemes.filterNot { it.id == preview.id } + preview,
                        selectedCustomThemeId = preview.id))
                }
                Text(t("Тему буде збережено разом із налаштуваннями."), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { editorOpen = false }, modifier = Modifier.heightIn(min = 48.dp)) { Text(t("Скасувати")) }
                    Button(enabled = valid, onClick = {
                        val theme = CustomColorTheme(editingId ?: UUID.randomUUID().toString(), name.trim(), checkNotNull(normalizedHex))
                        onChange(settings.copy(customThemes = settings.customThemes.filterNot { it.id == theme.id } + theme,
                            selectedCustomThemeId = theme.id))
                        editorOpen = false
                    }, modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("theme-save")) { Text(t("Зберегти тему")) }
                }
            }
        }
    }
}

@Composable
private fun PaletteOption(label: String, colors: List<Color>, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier = modifier.selectable(selected, role = Role.RadioButton, onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                if (selected) Icon(Icons.Outlined.Check, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                colors.forEach { color -> Box(Modifier.size(18.dp).background(color, CircleShape)) }
            }
        }
    }
}

@Composable
fun AppearanceLivePreview(settings: AppSettings) {
    TaktTheme(settings) {
        TaktGlassBackground(Modifier.fillMaxWidth().heightIn(min = 144.dp).clip(MaterialTheme.shapes.large),
            drawWallpaper = false) {
            SectionCard(Modifier.fillMaxWidth().padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GlassSurface(Modifier.size(48.dp), tint = MaterialTheme.colorScheme.secondaryContainer) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.MenuBook, null, Modifier.size(26.dp), tint = MaterialTheme.colorScheme.secondary)
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(settings.selectedCustomTheme?.name ?: t("ПРИКЛАД"), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary)
                        Text(t("Твоя перша пара"), style = MaterialTheme.typography.titleMedium)
                        Text("09:00–10:30 · " + t("Лекція · ауд. 302"), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(t("Підготувати конспект") + " · " + t("завтра"), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
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
