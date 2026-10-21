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
    PreferenceChange("cancellation", t("Canceled classes"), a.cancellationStyle, b.cancellationStyle, sourceName, ::cancellationLabel)
    PreferenceChange("hidden", t("Hidden classes"), a.showHiddenLessons, b.showHiddenLessons, sourceName, ::enabledLabel)
    PreferenceChange("parity", t("Week parity"), a.parityOverride, b.parityOverride, sourceName, ::parityLabel)
    PreferenceChange("week", t("Week view"), a.weekLayout, b.weekLayout, sourceName) {
        when (it) { "TIMETABLE" -> t("Schedule timetable"); "COMPACT_LIST" -> t("List"); else -> t("Unknown") }
    }
    BackupFilterChanges(a.homeWorkFilter, b.homeWorkFilter, phone, result, sourceName)
    val uis = t("Automatic UIS updates")
    listOf(
        Triple("progress", t("Progress"), a.uisProgressFrequency to b.uisProgressFrequency),
        Triple("subjects", t("Subjects"), a.uisSubjectFrequency to b.uisSubjectFrequency),
        Triple("periods", t("Study periods"), a.uisPeriodFrequency to b.uisPeriodFrequency),
        Triple("timetable", t("Timetable"), a.uisTimetableFrequency to b.uisTimetableFrequency),
    ).forEach { (key, label, values) ->
        PreferenceChange("uis-$key", "$uis · $label", values.first, values.second, sourceName) { value ->
            UisRefreshFrequency.entries.firstOrNull { it.name == value }?.let { t(it.label()) } ?: t("Unknown")
        }
    }
    PreferenceChange("uis-automatic", t("Apply progress automatically"),
        a.uisApplyProgressAutomatically, b.uisApplyProgressAutomatically, sourceName, ::enabledLabel)

    // Use the same normalized defaults and legacy TEST alias as the backup import.
    val remindersA = a.reminders.reviewModel()
    val remindersB = b.reminders.reviewModel()
    PreferenceChange("reminders-enabled", t("Local reminders"), remindersA.enabled, remindersB.enabled, sourceName, ::enabledLabel)
    PreferenceChange("reminders-academic", t("Academic work"), remindersA.academicEnabled, remindersB.academicEnabled, sourceName, ::enabledLabel)
    PreferenceChange("reminders-time", t("Time for work without a start time"),
        remindersA.dateOnlyTime, remindersB.dateOnlyTime, sourceName) { it.toString() }
    ReminderCategory.entries.forEach { category ->
        val old = remindersA.rule(category)
        val new = remindersB.rule(category)
        val label = t("Reminder") + " · " + t(category.label)
        PreferenceChange("reminders-${category.name}-enabled", label, old.enabled, new.enabled, sourceName, ::enabledLabel)
        PreferenceChange("reminders-${category.name}-timing", "$label · ${t("Reminder timing")}",
            old.offsetsMinutes, new.offsetsMinutes, sourceName) { offsets ->
            if (offsets.isEmpty()) t("No reminder times selected") else
                offsets.joinToString(" · ") { NotificationTranslations.offset(it, TaktI18n.language) }
        }
    }
}

@Composable
private fun BackupFilterChanges(a: BackupHomeWorkFilter, b: BackupHomeWorkFilter,
    phone: BackupPayload, result: BackupPayload, sourceName: String) {
    val prefix = t("Task filters")
    PreferenceChange("filter-period", "$prefix · ${t("Period")}", a.period, b.period, sourceName) {
        when (it) {
            "SEVEN_DAYS", "SEVEN_CLASS_DAYS" -> t("7 days")
            "FOURTEEN_DAYS" -> t("14 days")
            "ALL" -> t("All dates")
            "CUSTOM" -> t("Custom period")
            else -> t("Unknown")
        }
    }
    // Both the legacy single course and the current multi-course selection are portable fields.
    val coursesA = a.courseIds.ifEmpty { listOfNotNull(a.courseId) }.toSet()
    val coursesB = b.courseIds.ifEmpty { listOfNotNull(b.courseId) }.toSet()
    if (a.courseId != b.courseId || a.courseIds.toSet() != b.courseIds.toSet()) {
        PreferenceValues("filter-courses", "$prefix · ${t("Subjects")}",
            courseLabels(coursesA, phone), courseLabels(coursesB, result), sourceName)
    }
    PreferenceChange("filter-types", "$prefix · ${t("Task type")}", a.types.toSet(), b.types.toSet(), sourceName) { values ->
        if (values.isEmpty()) t("All types") else values.sorted().joinToString(" · ") { value ->
            if (value == "TASK") t("Assignment") else
                GradeItemType.entries.firstOrNull { it.name == value }?.let { t(it.label) } ?: t("Unknown")
        }
    }
    PreferenceChange("filter-completed", t("Show completed"), a.includeCompleted, b.includeCompleted, sourceName, ::enabledLabel)
    PreferenceChange("filter-undated", t("Show tasks without a date"), a.includeUndated, b.includeUndated, sourceName, ::enabledLabel)
    PreferenceChange("filter-from", "$prefix · ${t("From")}", a.fromDate, b.fromDate, sourceName, ::dateLabel)
    PreferenceChange("filter-to", "$prefix · ${t("To")}", a.toDate, b.toDate, sourceName, ::dateLabel)
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
        Text("${t("Phone")}: $phone", style = MaterialTheme.typography.bodyMedium)
        Text("$sourceName: $file", style = MaterialTheme.typography.bodyMedium)
    }
}

private fun enabledLabel(enabled: Boolean): String = t(if (enabled) "On" else "Off")
private fun cancellationLabel(value: String): String = t(when (value) {
    "STRIKETHROUGH" -> "Cross out"; "HIDDEN" -> "Hide"; "MARKED" -> "To mark"; else -> "Unknown"
})
private fun parityLabel(value: String): String = t(when (value) {
    "AUTO" -> "Automatically"; "EVEN" -> "Always even"; "ODD" -> "Always odd"; else -> "Unknown"
})
private fun dateLabel(value: String?): String = value?.let {
    runCatching { LocalDate.parse(it).format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) }.getOrDefault(t("Unknown"))
} ?: t("Unspecified")

private fun courseLabels(ids: Set<String>, payload: BackupPayload): String =
    if (ids.isEmpty()) t("All subjects") else ids.map { id ->
        payload.courses.firstOrNull { it.id == id }?.let { "${it.code} · ${it.title}" } ?: t("Unknown")
    }.sorted().joinToString(" · ")

private fun BackupReminderSettings.reviewModel(): ReminderSettings = ReminderSettings(
    enabled, academicEnabled, runCatching { LocalTime.parse(dateOnlyTime) }.getOrDefault(LocalTime.of(9, 0)),
    rules.mapNotNull { record ->
        ReminderCategory.entries.firstOrNull { it.name == record.category || (it == ReminderCategory.TEST && record.category == "MIDTERM") }
            ?.let { it to ReminderRule(record.enabled, record.offsetsMinutes).normalized() }
    }.toMap(),
).normalized()
