package com.kpyruy.takt.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.data.BackupHomeWorkFilter
import com.kpyruy.takt.core.data.BackupPayload
import com.kpyruy.takt.core.data.BackupReminderSettings
import com.kpyruy.takt.core.model.GradeItemType
import com.kpyruy.takt.core.model.ReminderCategory
import com.kpyruy.takt.core.model.ReminderRule
import com.kpyruy.takt.core.model.ReminderSettings
import com.kpyruy.takt.core.model.UisRefreshFrequency
import com.kpyruy.takt.core.ui.i18n.NotificationTranslations
import com.kpyruy.takt.core.ui.i18n.TaktI18n
import com.kpyruy.takt.core.ui.i18n.t
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Compare the actual merge result so course selections use the phone's remapped course IDs. */
@Composable
internal fun BackupPreferenceChanges(phone: BackupPayload, result: BackupPayload, sourceName: String) {
    val a = phone.settings
    val b = result.settings
    PreferenceChange("cancellation", t("Скасовані пари"), a.cancellationStyle, b.cancellationStyle, sourceName, ::cancellationLabel)
    PreferenceChange("hidden", t("Приховані заняття"), a.showHiddenLessons, b.showHiddenLessons, sourceName, ::enabledLabel)
    PreferenceChange("parity", t("Парність тижня"), a.parityOverride, b.parityOverride, sourceName, ::parityLabel)
    PreferenceChange("week", t("Вигляд тижня"), a.weekLayout, b.weekLayout, sourceName) {
        when (it) { "TIMETABLE" -> t("Таймтейбл"); "COMPACT_LIST" -> t("Список"); else -> t("Невідомо") }
    }
    BackupFilterChanges(a.homeWorkFilter, b.homeWorkFilter, phone, result, sourceName)
    val uis = t("Автооновлення UIS")
    listOf(
        Triple("progress", t("Прогрес"), a.uisProgressFrequency to b.uisProgressFrequency),
        Triple("subjects", t("Предмети"), a.uisSubjectFrequency to b.uisSubjectFrequency),
        Triple("periods", t("Періоди навчання"), a.uisPeriodFrequency to b.uisPeriodFrequency),
        Triple("timetable", t("Розклад"), a.uisTimetableFrequency to b.uisTimetableFrequency),
    ).forEach { (key, label, values) ->
        PreferenceChange("uis-$key", "$uis · $label", values.first, values.second, sourceName) { value ->
            UisRefreshFrequency.entries.firstOrNull { it.name == value }?.let { t(it.label()) } ?: t("Невідомо")
        }
    }
    PreferenceChange("uis-automatic", t("Автоматично застосовувати прогрес"),
        a.uisApplyProgressAutomatically, b.uisApplyProgressAutomatically, sourceName, ::enabledLabel)

    // Use the same normalized defaults and legacy TEST alias as the backup import.
    val remindersA = a.reminders.reviewModel()
    val remindersB = b.reminders.reviewModel()
    PreferenceChange("reminders-enabled", t("Локальні нагадування"), remindersA.enabled, remindersB.enabled, sourceName, ::enabledLabel)
    PreferenceChange("reminders-academic", t("Навчальні роботи"), remindersA.academicEnabled, remindersB.academicEnabled, sourceName, ::enabledLabel)
    PreferenceChange("reminders-time", t("Час для робіт без часу початку"),
        remindersA.dateOnlyTime, remindersB.dateOnlyTime, sourceName) { it.toString() }
    ReminderCategory.entries.forEach { category ->
        val old = remindersA.rule(category)
        val new = remindersB.rule(category)
        val label = t("Нагадування") + " · " + t(category.label)
        PreferenceChange("reminders-${category.name}-enabled", label, old.enabled, new.enabled, sourceName, ::enabledLabel)
        PreferenceChange("reminders-${category.name}-timing", "$label · ${t("Час нагадувань")}",
            old.offsetsMinutes, new.offsetsMinutes, sourceName) { offsets ->
            if (offsets.isEmpty()) t("Жодного часу не вибрано") else
                offsets.joinToString(" · ") { NotificationTranslations.offset(it, TaktI18n.language) }
        }
    }
}

@Composable
private fun BackupFilterChanges(a: BackupHomeWorkFilter, b: BackupHomeWorkFilter,
    phone: BackupPayload, result: BackupPayload, sourceName: String) {
    val prefix = t("Фільтри задач")
    PreferenceChange("filter-period", "$prefix · ${t("Період")}", a.period, b.period, sourceName) {
        when (it) {
            "SEVEN_DAYS", "SEVEN_CLASS_DAYS" -> t("7 днів")
            "FOURTEEN_DAYS" -> t("14 днів")
            "ALL" -> t("Усі дати")
            "CUSTOM" -> t("Свій період")
            else -> t("Невідомо")
        }
    }
    // Both the legacy single course and the current multi-course selection are portable fields.
    val coursesA = a.courseIds.ifEmpty { listOfNotNull(a.courseId) }.toSet()
    val coursesB = b.courseIds.ifEmpty { listOfNotNull(b.courseId) }.toSet()
    if (a.courseId != b.courseId || a.courseIds.toSet() != b.courseIds.toSet()) {
        PreferenceValues("filter-courses", "$prefix · ${t("Предмети")}",
            courseLabels(coursesA, phone), courseLabels(coursesB, result), sourceName)
    }
    PreferenceChange("filter-types", "$prefix · ${t("Тип задачі")}", a.types.toSet(), b.types.toSet(), sourceName) { values ->
        if (values.isEmpty()) t("Усі типи") else values.sorted().joinToString(" · ") { value ->
            if (value == "TASK") t("Завдання") else
                GradeItemType.entries.firstOrNull { it.name == value }?.let { t(it.label) } ?: t("Невідомо")
        }
    }
    PreferenceChange("filter-completed", t("Показувати виконані"), a.includeCompleted, b.includeCompleted, sourceName, ::enabledLabel)
    PreferenceChange("filter-undated", t("Показувати задачі без дати"), a.includeUndated, b.includeUndated, sourceName, ::enabledLabel)
    PreferenceChange("filter-from", "$prefix · ${t("Від")}", a.fromDate, b.fromDate, sourceName, ::dateLabel)
    PreferenceChange("filter-to", "$prefix · ${t("До")}", a.toDate, b.toDate, sourceName, ::dateLabel)
}

@Composable
private fun <T> PreferenceChange(key: String, label: String, phone: T, file: T, sourceName: String, format: (T) -> String) {
    if (phone != file) PreferenceValues(key, label, format(phone), format(file), sourceName)
}

@Composable
private fun PreferenceValues(key: String, label: String, phone: String, file: String, sourceName: String) {
    Column(Modifier.testTag("backup-preference-$key").semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("${t("Телефон")}: $phone", style = MaterialTheme.typography.bodyMedium)
        Text("$sourceName: $file", style = MaterialTheme.typography.bodyMedium)
    }
}

private fun enabledLabel(enabled: Boolean): String = t(if (enabled) "Увімкнено" else "Вимкнено")
private fun cancellationLabel(value: String): String = t(when (value) {
    "STRIKETHROUGH" -> "Закреслювати"; "HIDDEN" -> "Ховати"; "MARKED" -> "Позначати"; else -> "Невідомо"
})
private fun parityLabel(value: String): String = t(when (value) {
    "AUTO" -> "Автоматично"; "EVEN" -> "Завжди парний"; "ODD" -> "Завжди непарний"; else -> "Невідомо"
})
private fun dateLabel(value: String?): String = value?.let {
    runCatching { LocalDate.parse(it).format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) }.getOrDefault(t("Невідомо"))
} ?: t("Не вказано")

private fun courseLabels(ids: Set<String>, payload: BackupPayload): String =
    if (ids.isEmpty()) t("Усі предмети") else ids.map { id ->
        payload.courses.firstOrNull { it.id == id }?.let { "${it.code} · ${it.title}" } ?: t("Невідомо")
    }.sorted().joinToString(" · ")

private fun BackupReminderSettings.reviewModel(): ReminderSettings = ReminderSettings(
    enabled, academicEnabled, runCatching { LocalTime.parse(dateOnlyTime) }.getOrDefault(LocalTime.of(9, 0)),
    rules.mapNotNull { record ->
        ReminderCategory.entries.firstOrNull { it.name == record.category || (it == ReminderCategory.TEST && record.category == "MIDTERM") }
            ?.let { it to ReminderRule(record.enabled, record.offsetsMinutes).normalized() }
    }.toMap(),
).normalized()
