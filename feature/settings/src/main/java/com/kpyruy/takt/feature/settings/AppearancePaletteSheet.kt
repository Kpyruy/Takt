package com.kpyruy.takt.feature.settings

import com.kpyruy.takt.core.model.UiKey
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
    PaletteRole.PRIMARY -> t(UiKey.TEXT_PRIMARY_COLOR)
    PaletteRole.SECONDARY -> t(UiKey.TEXT_SECONDARY_COLOR)
    PaletteRole.TERTIARY -> t(UiKey.TEXT_TERTIARY_COLOR)
}

@Composable
private fun familyLabel(family: ThemeFamily): String = when (family) {
    ThemeFamily.BLUE -> t(UiKey.TEXT_BLUE)
    ThemeFamily.GREEN -> t(UiKey.TEXT_GREEN)
    ThemeFamily.PURPLE -> t(UiKey.TEXT_VIOLET)
    ThemeFamily.WARM -> t(UiKey.TEXT_WARM)
    ThemeFamily.MONOCHROME -> t(UiKey.TEXT_MONOCHROME)
    ThemeFamily.ROSE -> t(UiKey.TEXT_ROSE)
    ThemeFamily.AMBER -> t(UiKey.TEXT_AMBER)
    ThemeFamily.CYAN -> t(UiKey.TEXT_CYAN)
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
                Text(t(UiKey.TEXT_PALETTE_CUSTOM_THEMES), style = MaterialTheme.typography.titleSmall)
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
                        if (editor != null) TaktIconButton(Icons.AutoMirrored.Outlined.ArrowBack, t(UiKey.ACTION_BACK), { closeEditor() })
                        Text(if (editor == null) t(UiKey.TEXT_PALETTE_CUSTOM_THEMES) else t(UiKey.TEXT_EDIT_THEME),
                            Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                        TextButton(onClick = dismiss, modifier = Modifier.testTag("palette-popup-cancel")) {
                            Text(t(UiKey.ACTION_CANCEL))
                        }
                    }
                    val currentEditor = editor
                    val validHex = normalizeThemeHex(primaryInput)
                    val previewTheme = currentEditor?.let { it.copy(name = it.name.trim().ifEmpty { t(UiKey.TEXT_NEW_THEME) }.take(40)) }
                    val previewSettings = if (previewTheme != null) draft.withPreview(previewTheme) else draft
                    // Pin the sample outside both preset and editor scroll containers. When IME or
                    // a small window reduces height, use a two-row sample of all three roles.
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!shortPreview) Text(t(UiKey.TEXT_LIVE_PREVIEW), style = MaterialTheme.typography.titleSmall)
                        TaktSegmentedTabs(listOf(t(UiKey.TEXT_LIGHT), t(UiKey.TEXT_DARK)), if (previewDark) 1 else 0,
                            { previewDark = it == 1 }, Modifier.testTag("palette-preview-mode"))
                        AppearanceLivePreview(
                            previewSettings.copy(themeMode = if (previewDark) AppThemeMode.DARK else AppThemeMode.LIGHT),
                            compact = true, condensed = shortPreview)
                    }
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)
                        .testTag("palette-popup-content"), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        if (currentEditor == null) {
                            Text(t(UiKey.TEXT_PRESET_PALETTES), style = MaterialTheme.typography.titleSmall)
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
                            Text(t(UiKey.TEXT_CUSTOM_THEMES), style = MaterialTheme.typography.titleSmall)
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
                                            Spacer(Modifier.width(6.dp)); Text(t(UiKey.TEXT_EDIT_THEME))
                                        }
                                        IconButton(onClick = {
                                            draft = draft.copy(customThemes = draft.customThemes.filterNot { it.id == theme.id },
                                                selectedCustomThemeId = draft.selectedCustomThemeId.takeUnless { it == theme.id })
                                        }, modifier = Modifier.testTag("theme-delete-${theme.id}")) {
                                            Icon(Icons.Outlined.DeleteOutline, t(UiKey.TEXT_DELETE_THEME) + ": " + theme.name)
                                        }
                                    }
                                }
                            }
                            TextButton(onClick = {
                                val colors = taktThemePreviewColors(draft, false)
                                openEditor(CustomColorTheme(UUID.randomUUID().toString(), "", colors[0].hex(), colors[1].hex(), colors[2].hex()))
                            }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("theme-create")) {
                                Icon(Icons.Outlined.Add, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text(t(UiKey.TEXT_CREATE_THEME))
                            }
                        } else {
                            val duplicate = draft.customThemes.any { it.id != currentEditor.id && it.name.equals(currentEditor.name.trim(), true) }
                            TaktTextField(currentEditor.name, { editor = currentEditor.copy(name = it) },
                                Modifier.fillMaxWidth().testTag("theme-name"), label = { Text(t(UiKey.TEXT_THEME_NAME)) }, singleLine = true,
                                isError = currentEditor.name.length > 40 || duplicate,
                                supportingText = { Text(if (duplicate) t(UiKey.TEXT_A_THEME_WITH_THIS_NAME_ALREADY_EXISTS) else t(UiKey.TEXT_1_TO_40_CHARACTERS)) })
                            Text(t(UiKey.TEXT_THREE_INDEPENDENT_COLORS), style = MaterialTheme.typography.titleSmall)
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
                            }, Modifier.fillMaxWidth().testTag("theme-hex"), label = { Text(t(UiKey.TEXT_PRIMARY_COLOR)) },
                                singleLine = true, isError = validHex == null,
                                supportingText = { Text(t(UiKey.TEXT_HEX_RRGGBB_FOR_EXAMPLE_B52D67)) })
                            Text(t(UiKey.TEXT_COLOR_CHANGES_APPEAR_IMMEDIATELY_SAVED_ONLY_AFTER_APPLYING),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        val current = editor
                        if (current == null) {
                            TaktPrimaryButton(onClick = { onApply(draft.normalizedAppearance()) },
                                modifier = Modifier.fillMaxWidth().testTag("palette-popup-apply")) { Text(t(UiKey.TEXT_APPLY_PALETTE)) }
                        } else {
                            val clean = current.normalizedOrNull()
                            val duplicate = draft.customThemes.any { it.id != current.id && it.name.equals(current.name.trim(), true) }
                            TextButton(onClick = { closeEditor() }, modifier = Modifier.testTag("theme-cancel")) { Text(t(UiKey.ACTION_CANCEL)) }
                            TaktPrimaryButton(enabled = clean != null && validHex != null && !duplicate, onClick = {
                                val theme = checkNotNull(clean)
                                draft = draft.copy(customThemes = draft.customThemes.filterNot { it.id == theme.id } + theme,
                                    selectedCustomThemeId = theme.id)
                                closeEditor()
                            }, modifier = Modifier.weight(1f).testTag("theme-save")) { Text(t(UiKey.TEXT_SAVE_THEME)) }
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
