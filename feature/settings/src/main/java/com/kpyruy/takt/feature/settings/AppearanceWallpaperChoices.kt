package com.kpyruy.takt.feature.settings

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
                Text(t("Фон"), style = MaterialTheme.typography.titleSmall)
                Text(t(if (draft.customWallpaper == null) "Стандартний фон" else "Власне фото"),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Outlined.ChevronRight, null)
        }
    }
    if (open) TaktFullSheet(onDismissRequest = { if (!importing) open = false }) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.82f).testTag("appearance-wallpaper-menu")) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(t("Фон"), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                TaktIconButton(Icons.Outlined.Close, t("Закрити"), { if (!importing) open = false })
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
                    .testTag("appearance-wallpaper-done")) { Text(t("Готово")) }
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
                        Text(t("Твій простір"), style = MaterialTheme.typography.titleMedium)
                        Text(t("Твій день у власному настрої"), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                }
            }
        }
        Text(t("Фото зберігається на цьому пристрої. Зміни набудуть чинності після застосування."),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TaktPrimaryButton(onClick = { picker.launch(arrayOf("image/*")) }, enabled = !importing,
            modifier = Modifier.fillMaxWidth().testTag("appearance-wallpaper-choose")) {
            Text(t(if (importing) "Підготовка фото…" else "Обрати своє фото"))
        }
        if (draft.customWallpaper != null) TextButton(
            onClick = { failed = false; onChange(currentDraft.copy(customWallpaper = null)) }, enabled = !importing,
            modifier = Modifier.fillMaxWidth().testTag("appearance-wallpaper-reset")) { Text(t("Вбудований фон")) }
        if (failed) Text(t("Не вдалося відкрити фото. Обери інше зображення до 24 МБ і 64 Мп."),
            color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.testTag("appearance-wallpaper-error"))
    }
}
