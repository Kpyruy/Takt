package com.kpyruy.takt.feature.settings

import com.kpyruy.takt.core.model.UiKey
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import com.kpyruy.takt.core.ui.components.TaktFullSheet
import com.kpyruy.takt.core.ui.components.TaktIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.data.WallpaperStore
import com.kpyruy.takt.core.model.AppSettings
import com.kpyruy.takt.core.ui.components.GlassSurface
import com.kpyruy.takt.core.ui.components.TaktGlassBackground
import com.kpyruy.takt.core.ui.components.TaktPrimaryButton
import androidx.compose.material3.TextButton
import com.kpyruy.takt.core.ui.i18n.t
import com.kpyruy.takt.core.ui.theme.TaktTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Keep the Appearance page compact; photo actions and their preview live in a dedicated sheet. */
@Composable
internal fun AppearanceWallpaperMenu(draft: AppSettings, appliedReference: String?, onChange: (AppSettings) -> Unit,
    onImporting: (Boolean) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    var importing by remember { mutableStateOf(false) }
    GlassSurface(Modifier.fillMaxWidth().testTag("appearance-wallpaper-open").clickable { open = true },
        shape = MaterialTheme.shapes.medium) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.Wallpaper, null, Modifier.size(26.dp), tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(t(UiKey.TEXT_BACKGROUND), style = MaterialTheme.typography.titleSmall)
                Text(t(if (draft.customWallpaper == null) UiKey.TEXT_BUILT_IN_BACKGROUND else UiKey.TEXT_CUSTOM_PHOTO),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Outlined.ChevronRight, null)
        }
    }
    if (open) TaktFullSheet(onDismissRequest = { if (!importing) open = false }) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.82f).testTag("appearance-wallpaper-menu")) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(t(UiKey.TEXT_BACKGROUND), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                TaktIconButton(Icons.Outlined.Close, t(UiKey.ACTION_CLOSE), { if (!importing) open = false })
            }
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                AppearanceWallpaperChoices(draft, appliedReference, onChange) {
                    importing = it
                    onImporting(it)
                }
            }
            TaktPrimaryButton(onClick = { open = false }, enabled = !importing,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)
                    .testTag("appearance-wallpaper-done")) { Text(t(UiKey.TEXT_DONE)) }
        }
    }
}

@Composable
internal fun AppearanceWallpaperChoices(draft: AppSettings, appliedReference: String?, onChange: (AppSettings) -> Unit,
    onImporting: (Boolean) -> Unit) {
    val context = LocalContext.current.applicationContext
    val store = remember(context) { WallpaperStore(context) }
    val scope = rememberCoroutineScope()
    val currentDraft by rememberUpdatedState(draft)
    val currentApplied by rememberUpdatedState(appliedReference)
    var importing by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            importing = true
            onImporting(true)
            failed = false
            try {
                val reference = store.importImage(uri, currentApplied)
                onChange(currentDraft.copy(customWallpaper = reference))
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { failed = true }
            finally { importing = false; onImporting(false) }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TaktTheme(draft) {
            Box(Modifier.fillMaxWidth().height(240.dp).clip(MaterialTheme.shapes.medium)
                .testTag("appearance-wallpaper-preview")) {
                TaktGlassBackground(Modifier.matchParentSize()) {
                GlassSurface(Modifier.fillMaxWidth().padding(22.dp), shape = MaterialTheme.shapes.medium) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(t(UiKey.TEXT_YOUR_SPACE), style = MaterialTheme.typography.titleMedium)
                        Text(t(UiKey.TEXT_YOUR_DAY_YOUR_ATMOSPHERE), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                }
            }
        }
        Text(t(UiKey.TEXT_THE_PHOTO_IS_SAVED_ON_THIS_DEVICE_APPLY_TO_USE_YOUR_CHANGES),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TaktPrimaryButton(onClick = { picker.launch(arrayOf("image/*")) }, enabled = !importing,
            modifier = Modifier.fillMaxWidth().testTag("appearance-wallpaper-choose")) {
            Text(t(if (importing) UiKey.TEXT_PREPARING_PHOTO else UiKey.TEXT_CHOOSE_YOUR_PHOTO))
        }
        if (draft.customWallpaper != null) TextButton(
            onClick = { failed = false; onChange(currentDraft.copy(customWallpaper = null)) }, enabled = !importing,
            modifier = Modifier.fillMaxWidth().testTag("appearance-wallpaper-reset")) { Text(t(UiKey.TEXT_USE_BUILT_IN_BACKGROUND)) }
        if (failed) Text(t(UiKey.TEXT_COULD_NOT_OPEN_THIS_PHOTO_CHOOSE_ANOTHER_IMAGE_UP_TO_24_MB_AND_64_MP),
            color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.testTag("appearance-wallpaper-error"))
    }
}
