package com.kpyruy.takt.feature.settings

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.data.*
import com.kpyruy.takt.core.model.LessonType
import com.kpyruy.takt.core.ui.components.GlassSurface
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
    if (rules.isNotEmpty()) Text(t("Повторювані заняття"), style = MaterialTheme.typography.titleSmall)
    rules.take(12).forEach { rule ->
        val removed = rule.id !in incoming
        val day = runCatching { DayOfWeek.of(rule.dayOfWeek).getDisplayName(TextStyle.FULL, TaktI18n.locale) }.getOrDefault("—")
        val recurrence = t(when (rule.recurrence) { "ODD_WEEKS" -> "Непарні тижні"; "EVEN_WEEKS" -> "Парні тижні"; else -> "Щотижня" })
        val type = runCatching { t(LessonType.valueOf(rule.lessonType).label) }.getOrDefault("")
        BackupChangeRow(rule.title, "$day · ${time(rule.startMinute)}–${time(rule.endMinute)}",
            listOfNotNull(codes[rule.courseId], type.takeIf(String::isNotBlank), rule.room?.takeIf(String::isNotBlank), recurrence).joinToString(" · "),
            if (removed) t("Видалено") else if (rule.id in old) t("Змінено") else t("Додано"))
    }
    val oldEvents = phone.oneOffEvents.associateBy { it.id }
    val events = file.oneOffEvents.associateBy { it.id }
    val changed = file.oneOffEvents.filter { oldEvents[it.id] != it } + phone.oneOffEvents.filter { it.id !in events }
    if (changed.isNotEmpty()) Text(t("Окремі заняття"), style = MaterialTheme.typography.titleSmall)
    changed.take(12).forEach { event ->
        val date = runCatching { LocalDate.ofEpochDay(event.dateEpochDay).format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) }.getOrDefault("—")
        BackupChangeRow(event.title, "$date · ${time(event.startMinute)}–${time(event.endMinute)}",
            listOfNotNull(codes[event.courseId], event.room?.takeIf(String::isNotBlank)).joinToString(" · "),
            if (event.id !in events) t("Видалено") else if (event.id in oldEvents) t("Змінено") else t("Додано"))
    }
    if (rules.size > 12 || changed.size > 12) Text(t("Показано перші зміни. Застосовується весь вибраний розділ."), style = MaterialTheme.typography.bodySmall)
}

@Composable
internal fun BackupNoteChanges(phone: BackupPayload, file: BackupPayload) {
    val old = phone.courseNotes.associateBy { it.id }
    val codes = (phone.courses + file.courses).associate { it.id to it.code }
    val notes = file.courseNotes.filter { old[it.id] != it }
    notes.take(8).forEach { note ->
        BackupChangeRow(note.title, codes[note.courseId].orEmpty(),
            note.content.replace(Regex("\\s+"), " ").take(180), if (note.id in old) t("Змінено") else t("Додано"))
        if (note.attachments.isNotEmpty()) Text(t("Вкладення") + ": " + note.attachments.joinToString(", ") { it.name }, style = MaterialTheme.typography.bodySmall)
    }
    file.examMaterials.filter { item -> phone.examMaterials.none { it == item } }.take(6).forEach { item ->
        BackupChangeRow(item.title, codes[item.courseId].orEmpty(), t("Навчальний матеріал"), t("Додано"))
    }
    if (notes.isEmpty() && phone.courseNotes != file.courseNotes) Text(t("Змінено вміст або склад нотаток."), style = MaterialTheme.typography.bodySmall)
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
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        AppearanceSource(phone, t("Телефон"), Modifier.weight(1f), "backup-appearance-phone")
        AppearanceSource(file, sourceName, Modifier.weight(1f), "backup-appearance-file")
    }
}

@Composable
private fun AppearanceSource(settings: BackupSettings, sourceName: String, modifier: Modifier, tag: String) {
    val custom = settings.customThemes.firstOrNull { it.id == settings.selectedCustomThemeId }
    val family = t(when (settings.themeFamily) { "GREEN" -> "Зелена"; "PURPLE" -> "Фіолетова"; "WARM" -> "Тепла"; "MONOCHROME" -> "Монохром"; else -> "Синя" })
    Column(modifier.testTag(tag), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text(sourceName, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(custom?.name?.takeIf(String::isNotBlank) ?: family, style = MaterialTheme.typography.titleMedium)
        if (custom != null) Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            listOfNotNull(custom.primaryHex, custom.secondaryHex, custom.tertiaryHex).forEach { hex ->
                val color = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(MaterialTheme.colorScheme.primary)
                Surface(Modifier.size(22.dp), shape = RoundedCornerShape(8.dp), color = color) {}
            }
        }
        Text(t(when (settings.themeMode) { "DARK" -> "Темна"; "LIGHT" -> "Світла"; else -> "Як телефон" }), style = MaterialTheme.typography.bodySmall)
        Text(t(when (settings.cardAppearance) { "GLASS" -> "Скло"; "TONAL_FILLED" -> "Заливка"; else -> "Підняті" }), style = MaterialTheme.typography.bodySmall)
        val photo = settings.wallpaper
        val bitmap by produceState<Bitmap?>(null, photo?.sha256) {
            value = withContext(Dispatchers.Default) { if (photo == null) null else runCatching {
                val bytes = photo.decodedBytes()
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                options.inSampleSize = 1
                while (maxOf(options.outWidth, options.outHeight) / options.inSampleSize > 360) options.inSampleSize *= 2
                options.inJustDecodeBounds = false
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            }.getOrNull() }
        }
        val ready = bitmap
        if (ready != null) Image(ready.asImageBitmap(), t("Фон"), Modifier.fillMaxWidth().aspectRatio(.9f).clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop)
        Text(t(if (photo != null) "Власне фото" else if (settings.wallpaperRecorded) "Вбудований фон" else "Фото не включено в цю копію"), style = MaterialTheme.typography.bodySmall)
        if (settings.customThemes.isNotEmpty()) Text(t("Збережені теми") + ": " + settings.customThemes.joinToString(", ") { it.name }, style = MaterialTheme.typography.bodySmall)
    }
}
private fun time(value: Int) = if (value in 0..1440) "%02d:%02d".format(value / 60, value % 60) else "—"
