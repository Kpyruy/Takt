package com.kpyruy.takt.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
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
import com.kpyruy.takt.core.ui.components.TaktTextButton
import com.kpyruy.takt.core.ui.i18n.t
import com.kpyruy.takt.core.ui.theme.TaktTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

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
                val reference = store.import(uri, currentApplied)
                onChange(currentDraft.copy(customWallpaper = reference))
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { failed = true }
            finally { importing = false; onImporting(false) }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TaktTheme(draft) {
            TaktGlassBackground(Modifier.fillMaxWidth().height(180.dp).clip(MaterialTheme.shapes.medium)) {
                GlassSurface(Modifier.fillMaxWidth().padding(22.dp), shape = MaterialTheme.shapes.medium) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(t("Твій простір"), style = MaterialTheme.typography.titleMedium)
                        Text(t("Твій день у власному настрої"), style = MaterialTheme.typography.bodyMedium)
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
        if (draft.customWallpaper != null) TaktTextButton(
            onClick = { failed = false; onChange(currentDraft.copy(customWallpaper = null)) }, enabled = !importing,
            modifier = Modifier.fillMaxWidth().testTag("appearance-wallpaper-reset")) { Text(t("Вбудований фон")) }
        if (failed) Text(t("Не вдалося відкрити фото. Обери інше зображення до 24 МБ і 64 Мп."),
            color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.testTag("appearance-wallpaper-error"))
    }
}
