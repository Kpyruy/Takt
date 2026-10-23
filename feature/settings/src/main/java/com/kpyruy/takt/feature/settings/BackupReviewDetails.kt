package com.kpyruy.takt.feature.settings

import com.kpyruy.takt.core.model.UiKey
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.data.*
import com.kpyruy.takt.core.model.AppSettings
import com.kpyruy.takt.core.model.AppThemeMode
import com.kpyruy.takt.core.model.CardAppearance
import com.kpyruy.takt.core.model.CustomColorTheme
import com.kpyruy.takt.core.model.LessonType
import com.kpyruy.takt.core.model.ThemeFamily
import com.kpyruy.takt.core.ui.components.GlassSurface
import com.kpyruy.takt.core.ui.components.TaktGlassBackground
import com.kpyruy.takt.core.ui.theme.TaktTheme
import com.kpyruy.takt.core.ui.i18n.TaktI18n
import com.kpyruy.takt.core.ui.i18n.t
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

@Composable
internal fun BackupScheduleChanges(phone: BackupPayload, file: BackupPayload) {
    val old = phone.scheduleRules.associateBy { it.id }
    val incoming = file.scheduleRules.associateBy { it.id }
    val codes = (phone.courses + file.courses).associate { it.id to it.code }
    val rules = file.scheduleRules.filter { old[it.id] != it } + phone.scheduleRules.filter { it.id !in incoming }
    if (rules.isNotEmpty()) Text(t(UiKey.TEXT_RECURRING_LESSONS), style = MaterialTheme.typography.titleSmall)
    rules.take(12).forEach { rule ->
        val removed = rule.id !in incoming
        val day = runCatching { DayOfWeek.of(rule.dayOfWeek).getDisplayName(TextStyle.FULL, TaktI18n.locale) }.getOrDefault("—")
        val recurrence = t(when (rule.recurrence) { "ODD_WEEKS" -> UiKey.TEXT_ODD_WEEKS; "EVEN_WEEKS" -> UiKey.TEXT_EVEN_WEEKS; else -> UiKey.TEXT_EVERY_WEEK })
        val type = runCatching { t(LessonType.valueOf(rule.lessonType).label) }.getOrDefault("")
        BackupChangeRow(rule.title, "$day · ${time(rule.startMinute)}–${time(rule.endMinute)}",
            listOfNotNull(codes[rule.courseId], type.takeIf(String::isNotBlank), rule.room?.takeIf(String::isNotBlank), recurrence).joinToString(" · "),
            if (removed) t(UiKey.TEXT_REMOVED) else if (rule.id in old) t(UiKey.TEXT_CHANGED) else t(UiKey.TEXT_ADDED))
    }
    val oldEvents = phone.oneOffEvents.associateBy { it.id }
    val events = file.oneOffEvents.associateBy { it.id }
    val changed = file.oneOffEvents.filter { oldEvents[it.id] != it } + phone.oneOffEvents.filter { it.id !in events }
    if (changed.isNotEmpty()) Text(t(UiKey.TEXT_ONE_OFF_LESSONS), style = MaterialTheme.typography.titleSmall)
    changed.take(12).forEach { event ->
        val date = runCatching { LocalDate.ofEpochDay(event.dateEpochDay).format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) }.getOrDefault("—")
        BackupChangeRow(event.title, "$date · ${time(event.startMinute)}–${time(event.endMinute)}",
            listOfNotNull(codes[event.courseId], event.room?.takeIf(String::isNotBlank)).joinToString(" · "),
            if (event.id !in events) t(UiKey.TEXT_REMOVED) else if (event.id in oldEvents) t(UiKey.TEXT_CHANGED) else t(UiKey.TEXT_ADDED))
    }
    if (rules.size > 12 || changed.size > 12) Text(t(UiKey.TEXT_FIRST_CHANGES_SHOWN_THE_ENTIRE_SELECTED_SECTION_WILL_BE_APPLIED), style = MaterialTheme.typography.bodySmall)
}

@Composable
internal fun BackupNoteChanges(phone: BackupPayload, file: BackupPayload) {
    val old = phone.courseNotes.associateBy { it.id }
    val codes = (phone.courses + file.courses).associate { it.id to it.code }
    val notes = file.courseNotes.filter { old[it.id] != it }
    notes.take(8).forEach { note ->
        BackupChangeRow(note.title, codes[note.courseId].orEmpty(),
            note.content.replace(Regex("\\s+"), " ").take(180), if (note.id in old) t(UiKey.TEXT_CHANGED) else t(UiKey.TEXT_ADDED))
        if (note.attachments.isNotEmpty()) Text(t(UiKey.TEXT_ATTACHMENTS) + ": " + note.attachments.joinToString(", ") { it.name }, style = MaterialTheme.typography.bodySmall)
    }
    file.examMaterials.filter { item -> phone.examMaterials.none { it == item } }.take(6).forEach { item ->
        BackupChangeRow(item.title, codes[item.courseId].orEmpty(), t(UiKey.TEXT_STUDY_MATERIAL), t(UiKey.TEXT_ADDED))
    }
    if (notes.isEmpty() && phone.courseNotes != file.courseNotes) Text(t(UiKey.TEXT_NOTE_CONTENTS_OR_SELECTION_CHANGED), style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun BackupChangeRow(title: String, timing: String, detail: String, change: String) {
    GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                Text(change, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            if (timing.isNotBlank()) Text(timing, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun BackupAppearanceChanges(phone: BackupSettings, file: BackupSettings, sourceName: String) {
    val phonePreview = remember(phone) { phone.appearancePreviewSettings() }
    val filePreview = remember(file) { file.appearancePreviewSettings() }
    val phonePhoto = backupPreviewPhoto(phone.wallpaper)
    val filePhoto = backupPreviewPhoto(file.wallpaper)
    // Each property owns one shared row, so longer names and missing photos cannot shift the comparison.
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AppearanceComparisonRow(
            left = { Text(t(UiKey.TEXT_PHONE), Modifier.testTag("backup-appearance-phone"), style = MaterialTheme.typography.labelLarge) },
            right = { Text(sourceName, Modifier.testTag("backup-appearance-file"), style = MaterialTheme.typography.labelLarge) },
        )
        AppearanceComparisonRow(
            left = { Text(appearanceThemeName(phonePreview), style = MaterialTheme.typography.titleMedium) },
            right = { Text(appearanceThemeName(filePreview), style = MaterialTheme.typography.titleMedium) },
        )
        AppearanceComparisonRow(
            left = { AppearancePalette(phonePreview, "backup-appearance-phone-palette") },
            right = { AppearancePalette(filePreview, "backup-appearance-file-palette") },
        )
        AppearanceComparisonRow(
            left = { Text(appearanceModeName(phonePreview), style = MaterialTheme.typography.bodySmall) },
            right = { Text(appearanceModeName(filePreview), style = MaterialTheme.typography.bodySmall) },
        )
        BackupSettingChange(t(UiKey.APPEARANCE_ADD_PLACEMENT),
            t(if (phone.addButtonPlacement == "FLOATING") UiKey.APPEARANCE_ADD_FLOATING else UiKey.APPEARANCE_ADD_CENTER),
            t(if (file.addButtonPlacement == "FLOATING") UiKey.APPEARANCE_ADD_FLOATING else UiKey.APPEARANCE_ADD_CENTER))
        Text(t(UiKey.TEXT_CARD_STYLE), style = MaterialTheme.typography.labelLarge)
        AppearanceComparisonRow(
            left = { AppearanceMaterial(phonePreview, phonePhoto, "backup-appearance-phone-material") },
            right = { AppearanceMaterial(filePreview, filePhoto, "backup-appearance-file-material") },
        )
        AppearanceComparisonRow(
            left = { Text(appearanceMaterialName(phonePreview), style = MaterialTheme.typography.bodySmall) },
            right = { Text(appearanceMaterialName(filePreview), style = MaterialTheme.typography.bodySmall) },
        )
        Text(t(UiKey.TEXT_BACKGROUND), style = MaterialTheme.typography.labelLarge)
        AppearanceComparisonRow(
            left = { AppearanceWallpaper(phone, phonePreview, phonePhoto, "backup-appearance-phone-background") },
            right = { AppearanceWallpaper(file, filePreview, filePhoto, "backup-appearance-file-background") },
        )
        AppearanceComparisonRow(
            left = { Text(appearanceWallpaperName(phone), style = MaterialTheme.typography.bodySmall) },
            right = { Text(appearanceWallpaperName(file), style = MaterialTheme.typography.bodySmall) },
        )
        if (phone.customThemes.isNotEmpty() || file.customThemes.isNotEmpty()) {
            Text(t(UiKey.TEXT_SAVED_THEMES), style = MaterialTheme.typography.labelLarge)
            AppearanceComparisonRow(
                left = { Text(phone.customThemes.joinToString(", ") { it.name }.ifBlank { "—" }, style = MaterialTheme.typography.bodySmall) },
                right = { Text(file.customThemes.joinToString(", ") { it.name }.ifBlank { "—" }, style = MaterialTheme.typography.bodySmall) },
            )
        }
    }
}

@Composable
private fun AppearanceComparisonRow(left: @Composable () -> Unit, right: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.weight(1f)) { left() }
        Box(Modifier.weight(1f)) { right() }
    }
}

@Composable
private fun AppearancePalette(settings: AppSettings, tag: String) {
    TaktTheme(settings) {
        Row(Modifier.testTag(tag), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            val colors = MaterialTheme.colorScheme
            listOf(colors.primary, colors.secondary, colors.tertiary).forEach { color ->
                Surface(Modifier.size(26.dp), shape = RoundedCornerShape(9.dp), color = color) {}
            }
        }
    }
}

@Composable
private fun AppearanceMaterial(settings: AppSettings, photo: Bitmap?, tag: String) {
    TaktTheme(settings) {
        // TaktGlassBackground fills its constraints; always give a miniature a bounded parent.
        Box(Modifier.fillMaxWidth().height(116.dp).clip(RoundedCornerShape(14.dp)).testTag(tag)) {
            TaktGlassBackground(Modifier.matchParentSize(), photoPreview = photo) {
                GlassSurface(Modifier.align(Alignment.Center).padding(12.dp).fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(t(UiKey.TEXT_LESSONS), style = MaterialTheme.typography.titleSmall, maxLines = 1)
                        Box(Modifier.fillMaxWidth(.7f).height(5.dp).clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = .5f)))
                        Box(Modifier.fillMaxWidth(.45f).height(5.dp).clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .35f)))
                    }
                }
            }
        }
    }
}

@Composable
private fun backupPreviewPhoto(photo: BackupWallpaper?): Bitmap? {
    val bitmap by produceState<Bitmap?>(null, photo?.sha256) {
        value = withContext(Dispatchers.Default) {
            if (photo == null) null else runCatching {
                val bytes = photo.decodedBytes()
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                options.inSampleSize = 1
                while (maxOf(options.outWidth, options.outHeight) / options.inSampleSize > 360) options.inSampleSize *= 2
                options.inJustDecodeBounds = false
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            }.getOrNull()
        }
    }
    return bitmap
}

@Composable
private fun AppearanceWallpaper(source: BackupSettings, settings: AppSettings, bitmap: Bitmap?, tag: String) {
    val photo = source.wallpaper
    TaktTheme(settings) {
        Box(Modifier.fillMaxWidth().height(156.dp).clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant).testTag(tag), contentAlignment = Alignment.Center) {
            val ready = bitmap
            when {
                ready != null -> Image(ready.asImageBitmap(), t(UiKey.TEXT_CUSTOM_PHOTO), Modifier.matchParentSize(), contentScale = ContentScale.Crop)
                photo == null && source.wallpaperRecorded -> TaktGlassBackground(Modifier.matchParentSize()) {}
                else -> Text(t(if (photo != null) UiKey.TEXT_PHOTO_UNAVAILABLE else UiKey.TEXT_PHOTO_NOT_INCLUDED_IN_THIS_BACKUP),
                    Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** Preview only portable appearance fields; a backup never contains a local wallpaper reference. */
private fun BackupSettings.appearancePreviewSettings() = AppSettings(
    cardAppearance = runCatching { CardAppearance.valueOf(cardAppearance) }.getOrDefault(CardAppearance.ELEVATED),
    themeFamily = runCatching { ThemeFamily.valueOf(themeFamily) }.getOrDefault(ThemeFamily.BLUE),
    themeMode = runCatching { AppThemeMode.valueOf(themeMode) }.getOrDefault(AppThemeMode.SYSTEM),
    customThemes = customThemes.map { CustomColorTheme(it.id, it.name, it.primaryHex, it.secondaryHex, it.tertiaryHex) },
    selectedCustomThemeId = selectedCustomThemeId,
).normalizedAppearance()

@Composable
private fun appearanceThemeName(settings: AppSettings): String = settings.selectedCustomTheme?.name ?: t(when (settings.themeFamily) {
    ThemeFamily.GREEN -> UiKey.TEXT_GREEN; ThemeFamily.PURPLE -> UiKey.TEXT_VIOLET; ThemeFamily.WARM -> UiKey.TEXT_WARM
    ThemeFamily.MONOCHROME -> UiKey.TEXT_MONOCHROME; ThemeFamily.ROSE -> UiKey.TEXT_ROSE; ThemeFamily.AMBER -> UiKey.TEXT_AMBER
    ThemeFamily.CYAN -> UiKey.TEXT_CYAN; ThemeFamily.BLUE -> UiKey.TEXT_BLUE
})

@Composable
private fun appearanceModeName(settings: AppSettings): String = t(when (settings.themeMode) {
    AppThemeMode.DARK -> UiKey.TEXT_DARK; AppThemeMode.LIGHT -> UiKey.TEXT_LIGHT; AppThemeMode.SYSTEM -> UiKey.TEXT_DEVICE_THEME
})

@Composable
private fun appearanceMaterialName(settings: AppSettings): String = t(when (settings.cardAppearance) {
    CardAppearance.GLASS -> UiKey.TEXT_GLASS; CardAppearance.TONAL_FILLED -> UiKey.TEXT_FILLED; CardAppearance.ELEVATED -> UiKey.TEXT_ELEVATED
})

@Composable
private fun appearanceWallpaperName(settings: BackupSettings): String = t(when {
    settings.wallpaper != null -> UiKey.TEXT_CUSTOM_PHOTO
    settings.wallpaperRecorded -> UiKey.TEXT_USE_BUILT_IN_BACKGROUND
    else -> UiKey.TEXT_PHOTO_NOT_INCLUDED_IN_THIS_BACKUP
})

private fun time(value: Int) = if (value in 0..1440) "%02d:%02d".format(value / 60, value % 60) else "—"
