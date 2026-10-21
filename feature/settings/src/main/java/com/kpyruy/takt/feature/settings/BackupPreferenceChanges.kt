package com.kpyruy.takt.feature.settings

import com.kpyruy.takt.core.model.UiKey
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
    PreferenceChange("cancellation", t(UiKey.TEXT_CANCELED_CLASSES), a.cancellationStyle, b.cancellationStyle, sourceName, ::cancellationLabel)
    PreferenceChange("hidden", t(UiKey.TEXT_HIDDEN_CLASSES), a.showHiddenLessons, b.showHiddenLessons, sourceName, ::enabledLabel)
    PreferenceChange("parity", t(UiKey.TEXT_WEEK_PARITY), a.parityOverride, b.parityOverride, sourceName, ::parityLabel)
    PreferenceChange("week", t(UiKey.TEXT_WEEK_VIEW), a.weekLayout, b.weekLayout, sourceName) {
        when (it) { "TIMETABLE" -> t(UiKey.TEXT_SCHEDULE_TIMETABLE); "COMPACT_LIST" -> t(UiKey.TEXT_LIST); else -> t(UiKey.TEXT_UNKNOWN) }
    }
    BackupFilterChanges(a.homeWorkFilter, b.homeWorkFilter, phone, result, sourceName)
    val uis = t(UiKey.TEXT_AUTOMATIC_UIS_UPDATES)
    listOf(
        Triple("progress", t(UiKey.TEXT_PROGRESS), a.uisProgressFrequency to b.uisProgressFrequency),
        Triple("subjects", t(UiKey.TEXT_SUBJECTS), a.uisSubjectFrequency to b.uisSubjectFrequency),
        Triple("periods", t(UiKey.TEXT_STUDY_PERIODS), a.uisPeriodFrequency to b.uisPeriodFrequency),
        Triple("timetable", t(UiKey.TEXT_TIMETABLE), a.uisTimetableFrequency to b.uisTimetableFrequency),
    ).forEach { (key, label, values) ->
        PreferenceChange("uis-$key", "$uis · $label", values.first, values.second, sourceName) { value ->
            UisRefreshFrequency.entries.firstOrNull { it.name == value }?.let { t(it.label()) } ?: t(UiKey.TEXT_UNKNOWN)
        }
    }
    PreferenceChange("uis-automatic", t(UiKey.TEXT_APPLY_PROGRESS_AUTOMATICALLY),
        a.uisApplyProgressAutomatically, b.uisApplyProgressAutomatically, sourceName, ::enabledLabel)

    // Use the same normalized defaults and legacy TEST alias as the backup import.
    val remindersA = a.reminders.reviewModel()
    val remindersB = b.reminders.reviewModel()
    PreferenceChange("reminders-enabled", t(UiKey.TEXT_LOCAL_REMINDERS), remindersA.enabled, remindersB.enabled, sourceName, ::enabledLabel)
    PreferenceChange("reminders-academic", t(UiKey.TEXT_ACADEMIC_WORK), remindersA.academicEnabled, remindersB.academicEnabled, sourceName, ::enabledLabel)
    PreferenceChange("reminders-time", t(UiKey.TEXT_TIME_FOR_WORK_WITHOUT_A_START_TIME),
        remindersA.dateOnlyTime, remindersB.dateOnlyTime, sourceName) { it.toString() }
    ReminderCategory.entries.forEach { category ->
        val old = remindersA.rule(category)
        val new = remindersB.rule(category)
        val label = t(UiKey.TEXT_REMINDER) + " · " + t(category.label)
        PreferenceChange("reminders-${category.name}-enabled", label, old.enabled, new.enabled, sourceName, ::enabledLabel)
        PreferenceChange("reminders-${category.name}-timing", "$label · ${t(UiKey.TEXT_REMINDER_TIMING)}",
            old.offsetsMinutes, new.offsetsMinutes, sourceName) { offsets ->
            if (offsets.isEmpty()) t(UiKey.TEXT_NO_REMINDER_TIMES_SELECTED) else
                offsets.joinToString(" · ") { NotificationTranslations.offset(it, TaktI18n.language) }
        }
    }
}

@Composable
private fun BackupFilterChanges(a: BackupHomeWorkFilter, b: BackupHomeWorkFilter,
    phone: BackupPayload, result: BackupPayload, sourceName: String) {
    val prefix = t(UiKey.TEXT_TASK_FILTERS)
    PreferenceChange("filter-period", "$prefix · ${t(UiKey.TEXT_PERIOD_6E795D4D)}", a.period, b.period, sourceName) {
        when (it) {
            "SEVEN_DAYS", "SEVEN_CLASS_DAYS" -> t(UiKey.TEXT_7_DAYS)
            "FOURTEEN_DAYS" -> t(UiKey.TEXT_14_DAYS)
            "ALL" -> t(UiKey.TEXT_ALL_DATES)
            "CUSTOM" -> t(UiKey.TEXT_CUSTOM_PERIOD)
            else -> t(UiKey.TEXT_UNKNOWN)
        }
    }
    // Both the legacy single course and the current multi-course selection are portable fields.
    val coursesA = a.courseIds.ifEmpty { listOfNotNull(a.courseId) }.toSet()
    val coursesB = b.courseIds.ifEmpty { listOfNotNull(b.courseId) }.toSet()
    if (a.courseId != b.courseId || a.courseIds.toSet() != b.courseIds.toSet()) {
        PreferenceValues("filter-courses", "$prefix · ${t(UiKey.TEXT_SUBJECTS)}",
            courseLabels(coursesA, phone), courseLabels(coursesB, result), sourceName)
    }
    PreferenceChange("filter-types", "$prefix · ${t(UiKey.TEXT_TASK_TYPE)}", a.types.toSet(), b.types.toSet(), sourceName) { values ->
        if (values.isEmpty()) t(UiKey.TEXT_ALL_TYPES) else values.sorted().joinToString(" · ") { value ->
            if (value == "TASK") t(UiKey.TEXT_ASSIGNMENT) else
                GradeItemType.entries.firstOrNull { it.name == value }?.let { t(it.label) } ?: t(UiKey.TEXT_UNKNOWN)
        }
    }
    PreferenceChange("filter-completed", t(UiKey.TEXT_SHOW_COMPLETED), a.includeCompleted, b.includeCompleted, sourceName, ::enabledLabel)
    PreferenceChange("filter-undated", t(UiKey.TEXT_SHOW_TASKS_WITHOUT_A_DATE), a.includeUndated, b.includeUndated, sourceName, ::enabledLabel)
    PreferenceChange("filter-from", "$prefix · ${t(UiKey.TEXT_FROM_21819769)}", a.fromDate, b.fromDate, sourceName, ::dateLabel)
    PreferenceChange("filter-to", "$prefix · ${t(UiKey.TEXT_TO_F4B06EF6)}", a.toDate, b.toDate, sourceName, ::dateLabel)
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
        Text("${t(UiKey.TEXT_PHONE)}: $phone", style = MaterialTheme.typography.bodyMedium)
        Text("$sourceName: $file", style = MaterialTheme.typography.bodyMedium)
    }
}

private fun enabledLabel(enabled: Boolean): String = t(if (enabled) UiKey.TEXT_ON_13001175 else UiKey.TEXT_OFF)
private fun cancellationLabel(value: String): String = t(when (value) {
    "STRIKETHROUGH" -> UiKey.TEXT_CROSS_OUT; "HIDDEN" -> UiKey.TEXT_HIDE; "MARKED" -> UiKey.TEXT_TO_MARK; else -> UiKey.TEXT_UNKNOWN
})
private fun parityLabel(value: String): String = t(when (value) {
    "AUTO" -> UiKey.TEXT_AUTOMATICALLY; "EVEN" -> UiKey.TEXT_ALWAYS_EVEN; "ODD" -> UiKey.TEXT_ALWAYS_ODD; else -> UiKey.TEXT_UNKNOWN
})
private fun dateLabel(value: String?): String = value?.let {
    runCatching { LocalDate.parse(it).format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) }.getOrDefault(t(UiKey.TEXT_UNKNOWN))
} ?: t(UiKey.TEXT_UNSPECIFIED)

private fun courseLabels(ids: Set<String>, payload: BackupPayload): String =
    if (ids.isEmpty()) t(UiKey.TEXT_ALL_SUBJECTS) else ids.map { id ->
        payload.courses.firstOrNull { it.id == id }?.let { "${it.code} · ${it.title}" } ?: t(UiKey.TEXT_UNKNOWN)
    }.sorted().joinToString(" · ")

private fun BackupReminderSettings.reviewModel(): ReminderSettings = ReminderSettings(
    enabled, academicEnabled, runCatching { LocalTime.parse(dateOnlyTime) }.getOrDefault(LocalTime.of(9, 0)),
    rules.mapNotNull { record ->
        ReminderCategory.entries.firstOrNull { it.name == record.category || (it == ReminderCategory.TEST && record.category == "MIDTERM") }
            ?.let { it to ReminderRule(record.enabled, record.offsetsMinutes).normalized() }
    }.toMap(),
).normalized()
