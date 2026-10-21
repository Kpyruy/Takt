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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.*
import com.kpyruy.takt.core.ui.components.TaktFullSheet
import com.kpyruy.takt.core.ui.components.TaktIconButton
import com.kpyruy.takt.core.ui.components.TaktPrimaryButton
import com.kpyruy.takt.core.ui.components.TaktSegmentedTabs
import com.kpyruy.takt.core.ui.components.TaktSurface
import com.kpyruy.takt.core.ui.components.TaktTextField
import com.kpyruy.takt.core.ui.i18n.t
import com.kpyruy.takt.core.ui.theme.TaktTheme
import com.kpyruy.takt.core.ui.theme.taktThemePreviewColors
import com.kpyruy.takt.core.ui.theme.taktEditableCustomTheme
import java.util.Locale
import java.util.UUID

internal enum class PaletteRole { PRIMARY, SECONDARY, TERTIARY }

@Composable
internal fun PaletteRole.label(): String = when (this) {
    PaletteRole.PRIMARY -> t("Primary color")
    PaletteRole.SECONDARY -> t("Secondary color")
    PaletteRole.TERTIARY -> t("Tertiary color")
}

@Composable
private fun familyLabel(family: ThemeFamily): String = when (family) {
    ThemeFamily.BLUE -> t("Blue")
    ThemeFamily.GREEN -> t("Green")
    ThemeFamily.PURPLE -> t("Violet")
    ThemeFamily.WARM -> t("Warm")
    ThemeFamily.MONOCHROME -> t("Monochrome")
    ThemeFamily.ROSE -> t("Rose")
    ThemeFamily.AMBER -> t("Amber")
    ThemeFamily.CYAN -> t("Cyan")
}

/** Both Appearance and onboarding open the same transactional palette editor. */
@Composable
fun AppearanceThemeChoices(settings: AppSettings, onChange: (AppSettings) -> Unit, tagPrefix: String = "appearance-color") {
    var open by remember { mutableStateOf(false) }
    TaktSurface(Modifier.fillMaxWidth().selectable(false, role = Role.Button, onClick = { open = true })
        .testTag("$tagPrefix-open"), shape = MaterialTheme.shapes.medium) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(t("Palette & custom themes"), style = MaterialTheme.typography.titleSmall)
                Text(settings.selectedCustomTheme?.name ?: familyLabel(settings.themeFamily),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                PaletteDots(taktThemePreviewColors(settings, false))
            }
            Icon(Icons.Outlined.ChevronRight, null)
        }
    }
    if (open) AppearancePaletteSheet(settings, tagPrefix, onDismiss = { open = false }, onApply = {
        // A long-lived popup draft must not replace its caller's newer non-palette settings.
        onChange(settings.copy(themeFamily = it.themeFamily, customThemes = it.customThemes,
            selectedCustomThemeId = it.selectedCustomThemeId))
        open = false
    })
}

@Composable
private fun AppearancePaletteSheet(initial: AppSettings, tagPrefix: String, onDismiss: () -> Unit, onApply: (AppSettings) -> Unit) {
    var draft by remember { mutableStateOf(initial.normalizedAppearance()) }
    var editor by remember { mutableStateOf<CustomColorTheme?>(null) }
    var primaryInput by remember { mutableStateOf("") }
    var pickerRole by remember { mutableStateOf<PaletteRole?>(null) }
    var previewDark by remember { mutableStateOf(initial.themeMode == AppThemeMode.DARK) }
    val systemDark = isSystemInDarkTheme()
    LaunchedEffect(Unit) { if (initial.themeMode == AppThemeMode.SYSTEM) previewDark = systemDark }
    fun closeEditor() { editor = null; pickerRole = null }
    fun openEditor(theme: CustomColorTheme) { editor = taktEditableCustomTheme(theme); primaryInput = theme.primaryHex }
    val dismiss = { if (editor != null) closeEditor() else onDismiss() }
    BackHandler { dismiss() }
    TaktTheme(draft) {
        TaktFullSheet(onDismissRequest = onDismiss) {
            BoxWithConstraints(Modifier.fillMaxWidth().fillMaxHeight(.88f).imePadding()) {
                val shortPreview = maxHeight < 480.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
                Column(Modifier.fillMaxSize().testTag("palette-popup")) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (editor != null) TaktIconButton(Icons.AutoMirrored.Outlined.ArrowBack, t("Back"), { closeEditor() })
                        Text(if (editor == null) t("Palette & custom themes") else t("Edit theme"),
                            Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                        TextButton(onClick = dismiss, modifier = Modifier.testTag("palette-popup-cancel")) {
                            Text(t("Cancel"))
                        }
                    }
                    val currentEditor = editor
                    val validHex = normalizeThemeHex(primaryInput)
                    val previewTheme = currentEditor?.let { it.copy(name = it.name.trim().ifEmpty { t("New theme") }.take(40)) }
                    val previewSettings = if (previewTheme != null) draft.withPreview(previewTheme) else draft
                    // Pin the sample outside both preset and editor scroll containers. When IME or
                    // a small window reduces height, use a two-row sample of all three roles.
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!shortPreview) Text(t("Live preview"), style = MaterialTheme.typography.titleSmall)
                        TaktSegmentedTabs(listOf(t("Light"), t("Dark")), if (previewDark) 1 else 0,
                            { previewDark = it == 1 }, Modifier.testTag("palette-preview-mode"))
                        AppearanceLivePreview(
                            previewSettings.copy(themeMode = if (previewDark) AppThemeMode.DARK else AppThemeMode.LIGHT),
                            compact = true, condensed = shortPreview)
                    }
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)
                        .testTag("palette-popup-content"), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        if (currentEditor == null) {
                            Text(t("Preset palettes"), style = MaterialTheme.typography.titleSmall)
                            ThemeFamily.entries.chunked(2).forEach { pair ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    pair.forEach { family ->
                                        PaletteOption(familyLabel(family), taktThemePreviewColors(family, previewDark),
                                            draft.selectedCustomTheme == null && draft.themeFamily == family,
                                            Modifier.weight(1f).testTag("$tagPrefix-${family.name}")) {
                                            draft = draft.copy(themeFamily = family, selectedCustomThemeId = null)
                                        }
                                    }
                                }
                            }
                            Text(t("Custom themes"), style = MaterialTheme.typography.titleSmall)
                            draft.customThemes.forEach { theme ->
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    PaletteOption(theme.name, taktThemePreviewColors(draft.withPreview(theme), previewDark),
                                        draft.selectedCustomThemeId == theme.id,
                                        Modifier.fillMaxWidth().testTag("$tagPrefix-custom-${theme.id}")) {
                                        draft = draft.copy(selectedCustomThemeId = theme.id)
                                    }
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                        TextButton(onClick = { openEditor(theme) }, modifier = Modifier.testTag("theme-edit-${theme.id}")) {
                                            Icon(Icons.Outlined.Edit, null, Modifier.size(18.dp))
                                            Spacer(Modifier.width(6.dp)); Text(t("Edit theme"))
                                        }
                                        IconButton(onClick = {
                                            draft = draft.copy(customThemes = draft.customThemes.filterNot { it.id == theme.id },
                                                selectedCustomThemeId = draft.selectedCustomThemeId.takeUnless { it == theme.id })
                                        }, modifier = Modifier.testTag("theme-delete-${theme.id}")) {
                                            Icon(Icons.Outlined.DeleteOutline, t("Delete theme") + ": " + theme.name)
                                        }
                                    }
                                }
                            }
                            TextButton(onClick = {
                                val colors = taktThemePreviewColors(draft, false)
                                openEditor(CustomColorTheme(UUID.randomUUID().toString(), "", colors[0].hex(), colors[1].hex(), colors[2].hex()))
                            }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("theme-create")) {
                                Icon(Icons.Outlined.Add, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text(t("Create theme"))
                            }
                        } else {
                            val duplicate = draft.customThemes.any { it.id != currentEditor.id && it.name.equals(currentEditor.name.trim(), true) }
                            TaktTextField(currentEditor.name, { editor = currentEditor.copy(name = it) },
                                Modifier.fillMaxWidth().testTag("theme-name"), label = { Text(t("Theme name")) }, singleLine = true,
                                isError = currentEditor.name.length > 40 || duplicate,
                                supportingText = { Text(if (duplicate) t("A theme with this name already exists") else t("1 to 40 characters")) })
                            Text(t("Three independent colors"), style = MaterialTheme.typography.titleSmall)
                            val roleColors = taktThemePreviewColors(previewSettings, false)
                            PaletteRole.entries.forEachIndexed { index, role ->
                                val roleHex = currentEditor.colorHex(role, roleColors[index])
                                TaktSurface(Modifier.fillMaxWidth().selectable(false, role = Role.Button,
                                    onClick = { pickerRole = role }).testTag("theme-color-${role.name}"),
                                    shape = MaterialTheme.shapes.medium) {
                                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Box(Modifier.size(30.dp).background(Color((0xFF000000L or roleHex.drop(1).toLong(16)).toInt()), CircleShape))
                                        Column(Modifier.weight(1f)) {
                                            Text(role.label(), style = MaterialTheme.typography.labelLarge)
                                            Text(roleHex, style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Icon(Icons.Outlined.Edit, null, Modifier.size(20.dp))
                                    }
                                }
                            }
                            // Retain the old direct primary HEX field/tag; the popup picker also has HEX.
                            TaktTextField(primaryInput, {
                                primaryInput = it
                                normalizeThemeHex(it)?.let { hex -> editor = currentEditor.copy(primaryHex = hex) }
                            }, Modifier.fillMaxWidth().testTag("theme-hex"), label = { Text(t("Primary color")) },
                                singleLine = true, isError = validHex == null,
                                supportingText = { Text(t("HEX: #RRGGBB, for example #B52D67")) })
                            Text(t("Color changes appear immediately. Saved only after applying."),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        val current = editor
                        if (current == null) {
                            TaktPrimaryButton(onClick = { onApply(draft.normalizedAppearance()) },
                                modifier = Modifier.fillMaxWidth().testTag("palette-popup-apply")) { Text(t("Apply palette")) }
                        } else {
                            val clean = current.normalizedOrNull()
                            val duplicate = draft.customThemes.any { it.id != current.id && it.name.equals(current.name.trim(), true) }
                            TextButton(onClick = { closeEditor() }, modifier = Modifier.testTag("theme-cancel")) { Text(t("Cancel")) }
                            TaktPrimaryButton(enabled = clean != null && validHex != null && !duplicate, onClick = {
                                val theme = checkNotNull(clean)
                                draft = draft.copy(customThemes = draft.customThemes.filterNot { it.id == theme.id } + theme,
                                    selectedCustomThemeId = theme.id)
                                closeEditor()
                            }, modifier = Modifier.weight(1f).testTag("theme-save")) { Text(t("Save theme")) }
                        }
                    }
                    val role = pickerRole
                    if (role != null && currentEditor != null && previewTheme != null) {
                        val colors = taktThemePreviewColors(previewSettings, false)
                        PaletteColorPicker(role, currentEditor.colorHex(role, colors[role.ordinal]),
                            previewSettings.copy(themeMode = if (previewDark) AppThemeMode.DARK else AppThemeMode.LIGHT),
                            previewTheme, onDismiss = { pickerRole = null }, onApply = { hex ->
                                editor = currentEditor.withColor(role, hex)
                                if (role == PaletteRole.PRIMARY) primaryInput = hex
                                pickerRole = null
                            })
                    }
                }
            }
        }
    }
}

internal fun AppSettings.withPreview(theme: CustomColorTheme): AppSettings = copy(
    customThemes = customThemes.filterNot { it.id == theme.id } + theme, selectedCustomThemeId = theme.id)

internal fun CustomColorTheme.withColor(role: PaletteRole, hex: String): CustomColorTheme = when (role) {
    PaletteRole.PRIMARY -> copy(primaryHex = hex)
    PaletteRole.SECONDARY -> copy(secondaryHex = hex)
    PaletteRole.TERTIARY -> copy(tertiaryHex = hex)
}

private fun CustomColorTheme.colorHex(role: PaletteRole, fallback: Color): String = when (role) {
    PaletteRole.PRIMARY -> primaryHex
    PaletteRole.SECONDARY -> secondaryHex ?: fallback.hex()
    PaletteRole.TERTIARY -> tertiaryHex ?: fallback.hex()
}

private fun Color.hex(): String = String.format(Locale.ROOT, "#%06X", toArgb() and 0xFFFFFF)

@Composable
private fun PaletteOption(label: String, colors: List<Color>, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    TaktSurface(modifier.selectable(selected, role = Role.RadioButton, onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                if (selected) Icon(Icons.Outlined.Check, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            }
            PaletteDots(colors)
        }
    }
}

@Composable
private fun PaletteDots(colors: List<Color>) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        colors.forEach { color -> Box(Modifier.size(18.dp).background(color, CircleShape)) }
    }
}
