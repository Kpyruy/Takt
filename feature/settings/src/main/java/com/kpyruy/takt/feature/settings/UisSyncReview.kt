package com.kpyruy.takt.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kpyruy.takt.core.data.matchesUisCourse
import com.kpyruy.takt.core.data.matchingUisCourse
import com.kpyruy.takt.core.data.UniversityAccountRepository
import com.kpyruy.takt.core.data.UisSyncSection
import com.kpyruy.takt.core.model.Course
import com.kpyruy.takt.core.model.CourseStatus
import com.kpyruy.takt.core.model.AppSettings
import kotlinx.coroutines.flow.flowOf
import com.kpyruy.takt.core.model.OneOffScheduleEvent
import com.kpyruy.takt.core.model.ScheduleRule
import com.kpyruy.takt.core.ui.components.SectionCard
import com.kpyruy.takt.core.ui.components.GlassSurface
import com.kpyruy.takt.core.ui.i18n.TaktI18n
import com.kpyruy.takt.core.ui.i18n.t
import kotlinx.coroutines.launch
import java.time.format.TextStyle

@Composable
fun UisSyncReview(repository: UniversityAccountRepository, showHeading: Boolean = true) {
    val preview by repository.syncPreview.collectAsStateWithLifecycle()
    val data = preview ?: return
    val settings by (repository.settingsRepository?.settings ?: flowOf(AppSettings()))
        .collectAsStateWithLifecycle(initialValue = AppSettings())
    val action by repository.syncAction.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var selectingCourse by remember { mutableStateOf<Course?>(null) }
    var confirmReplacement by remember { mutableStateOf(false) }
    val progress = if (UisSyncSection.PROGRESS in data.availableSections) data.courses.mapNotNull { remote ->
        data.localCourses.matchingUisCourse(remote)?.let { local ->
            if (local.status != remote.status || local.passFailResult != remote.passFailResult ||
                local.officialGrade != (remote.officialGrade ?: local.officialGrade) || local.fulfilledOn != (remote.fulfilledOn ?: local.fulfilledOn)) local to remote else null
        }
    } else emptyList()
    val metadata = data.courses.filter { remote ->
        val matches = data.localCourses.filter { it.matchesUisCourse(remote) }
        (remote.id !in data.associatedCourseIds && matches.any { it.id != remote.id }) ||
            (UisSyncSection.SUBJECTS in data.availableSections &&
                (matches.isEmpty() || matches.none { sameSubject(it, remote) }))
    }
    val scheduleAvailable = UisSyncSection.TIMETABLE in data.availableSections
    val addedRules = if (scheduleAvailable) data.rules.filter { remote -> data.localRules.none { sameRule(it, remote) } } else emptyList()
    val addedEvents = if (scheduleAvailable) data.oneOffEvents.filter { remote -> data.localOneOffEvents.none { sameEvent(it, remote) } } else emptyList()
    val removedRules = if (scheduleAvailable) data.localRules.filter { local -> data.rules.none { sameRule(it, local) } } else emptyList()
    val removedEvents = if (scheduleAvailable) data.localOneOffEvents.filter { local -> data.oneOffEvents.none { sameEvent(it, local) } } else emptyList()
    val remotePeriod = data.period

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (showHeading) Text(t("Порівняння з UIS"), style = MaterialTheme.typography.titleLarge)
        action.message?.let { Text(t(it), color = MaterialTheme.colorScheme.primary) }
        action.error?.let {
            Text(t(it), color = MaterialTheme.colorScheme.error)
            TextButton(enabled = !action.running, onClick = { scope.launch { repository.retrySyncAction() } }) { Text(t("Повторити")) }
        }
        if (action.running) Text(t("Збереження…"))
        if (UisSyncSection.PERIODS in data.availableSections && remotePeriod != null && data.localPeriod != remotePeriod) {
            Text(t("Навчальний період"), style = MaterialTheme.typography.titleMedium)
            Text(t("Локально: ${data.localPeriod?.studyStart ?: "—"} – ${data.localPeriod?.studyEnd ?: "—"}; іспити ${data.localPeriod?.examStart ?: "—"} – ${data.localPeriod?.examEnd ?: "—"}"))
            Text(t("UIS: ${remotePeriod.studyStart} – ${remotePeriod.studyEnd}; іспити ${remotePeriod.examStart} – ${remotePeriod.examEnd}"))
            OutlinedButton(enabled = !action.running, onClick = { scope.launch { repository.applyPeriod() } }) {
                Text(t("Оновити період"))
            }
        }
        if (UisSyncSection.PROGRESS in data.availableSections &&
            (data.localEarnedCredits != data.earnedCredits || data.localRequiredCredits != data.requiredCredits)) {
            Text(t("Кредити UIS"), style = MaterialTheme.typography.titleMedium)
            Text(t("Локально: ${data.localEarnedCredits ?: "—"} із ${data.localRequiredCredits ?: "—"}; UIS: ${data.earnedCredits ?: "—"} із ${data.requiredCredits ?: "—"}"))
            OutlinedButton(enabled = !action.running, onClick = repository::applyCredits) { Text(t("Оновити кредити UIS")) }
        }
        if (progress.isNotEmpty()) {
            Text(t("Прогрес · ${progress.size} змін"), style = MaterialTheme.typography.titleMedium)
            OutlinedButton(enabled = !action.running, onClick = { scope.launch { repository.applyAllProgress() } }) {
                Text(t("Оновити весь прогрес"))
            }
            progress.forEach { (local, remote) ->
                SectionCard {
                Text(local.title, style = MaterialTheme.typography.titleMedium)
                Text(remote.code, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (local.status != remote.status) Text("${statusLabel(local.status)} → ${statusLabel(remote.status)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (local.passFailResult != remote.passFailResult) {
                    Text("${local.passFailResult?.name ?: "—"} → ${remote.passFailResult?.name ?: "—"}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                val nextGrade = remote.officialGrade ?: local.officialGrade
                val nextDate = remote.fulfilledOn ?: local.fulfilledOn
                if (local.officialGrade != nextGrade) {
                    Text(t("Офіційна оцінка UIS") + ": ${local.officialGrade?.name ?: "—"} → ${nextGrade?.name ?: "—"}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (local.fulfilledOn != nextDate) {
                    Text(t("Дата складання") + ": ${local.fulfilledOn ?: "—"} → ${nextDate ?: "—"}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(enabled = !action.running, onClick = { scope.launch { repository.applyProgress(remote.id) } }) {
                    Text(t("Оновити прогрес предмета"))
                }
                }
            }
        }
        if (metadata.isNotEmpty()) {
            Text(t("Предмети · ${metadata.size} змін"), style = MaterialTheme.typography.titleMedium)
            metadata.forEach { remote ->
                SectionCard {
                val local = data.localCourses.matchingUisCourse(remote)
                Text("${remote.code} · ${local?.title ?: "—"} → ${settings.displayCourseTitle(remote)}")
                val candidates = data.localCourses.filter { it.matchesUisCourse(remote) }
                TextButton(enabled = !action.running, onClick = {
                    if (candidates.size > 1) selectingCourse = remote
                    else scope.launch { repository.applyCourse(remote.id) }
                }) {
                    Text(t(if (local == null) "Додати предмет" else "Оновити предмет"))
                }
                }
            }
        }
        if (addedRules.isNotEmpty() || addedEvents.isNotEmpty() || removedRules.isNotEmpty() || removedEvents.isNotEmpty()) {
            Text(t("Розклад · ${addedRules.size + addedEvents.size} нових, ${removedRules.size + removedEvents.size} лише локально"),
                style = MaterialTheme.typography.titleMedium)
            OutlinedButton(enabled = !action.running, onClick = { confirmReplacement = true },
                modifier = Modifier.fillMaxWidth()) { Text(t("Замінити весь розклад розкладом UIS")) }
            addedRules.forEach { rule ->
                TimetableDifference(enabled = !action.running, title = rule.title,
                    details = rule.dayOfWeek.getDisplayName(TextStyle.FULL, TaktI18n.locale) + " · ${rule.startTime}–${rule.endTime}" +
                        rule.room?.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()) {
                    scope.launch { repository.addRule(rule.id) }
                }
            }
            addedEvents.forEach { event ->
                TimetableDifference(enabled = !action.running, title = event.title,
                    details = "${event.date} · ${event.startTime}–${event.endTime}" +
                        event.room?.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()) {
                    scope.launch { repository.addOneOff(event.id) }
                }
            }
            if (removedRules.isNotEmpty() || removedEvents.isNotEmpty()) {
                Text(t("Лише локально: ${removedRules.size + removedEvents.size} занять. Вони зникнуть лише після повної заміни."),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                removedRules.forEach { rule -> Text("${rule.dayOfWeek} ${rule.startTime}–${rule.endTime} · ${rule.title}") }
                removedEvents.forEach { event -> Text("${event.date} ${event.startTime}–${event.endTime} · ${event.title}") }
            }
        }
        if (progress.isEmpty() && metadata.isEmpty() && addedRules.isEmpty() && addedEvents.isEmpty() &&
            removedRules.isEmpty() && removedEvents.isEmpty() &&
            (UisSyncSection.PERIODS !in data.availableSections || data.localPeriod == remotePeriod) &&
            (UisSyncSection.PROGRESS !in data.availableSections ||
                data.localEarnedCredits == data.earnedCredits && data.localRequiredCredits == data.requiredCredits)) {
            Text(t("Дані збігаються"))
        }
        TextButton(enabled = !action.running, onClick = repository::dismissPreview) { Text(t("Залишити локальні дані")) }
    }
    selectingCourse?.let { remote ->
        AlertDialog(
            onDismissRequest = { selectingCourse = null },
            title = { Text(t("Обери відповідний предмет")) },
            text = { Column {
                Text("${remote.code} · ${settings.displayCourseTitle(remote)}")
                Text(t("Особисті дані обраного предмета буде перенесено до ID UIS."))
                data.localCourses.filter { it.matchesUisCourse(remote) }.forEach { candidate ->
                    TextButton(enabled = !action.running, onClick = {
                        selectingCourse = null
                        scope.launch { repository.applyCourse(remote.id, candidate.id) }
                    }) {
                        Text("${candidate.title} · ${candidate.semester} сем. · ${candidate.credits} кр. · ${if (candidate.id.startsWith("uis:")) "UIS" else "Локальний"}")
                    }
                }
            } },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { selectingCourse = null }) { Text(t("Скасувати")) } },
        )
    }
    if (confirmReplacement) AlertDialog(
        onDismissRequest = { confirmReplacement = false },
        title = { Text(t("Замінити розклад?")) },
        text = { Text(t("Усі локальні заняття та їх зміни буде видалено. Якщо маєш резервну копію, з неї можна відновити розклад.")) },
        confirmButton = { TextButton(enabled = !action.running, onClick = {
            confirmReplacement = false
            scope.launch { repository.replaceSchedule() }
        }) { Text(t("Замінити")) } },
        dismissButton = { TextButton(onClick = { confirmReplacement = false }) { Text(t("Скасувати")) } },
    )
}

@Composable
private fun TimetableDifference(title: String, details: String, enabled: Boolean, onAdd: () -> Unit) {
    SectionCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            GlassSurface(Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primaryContainer) {
                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Schedule, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f))
        OutlinedButton(enabled = enabled, onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.Add, null, Modifier.padding(end = 8.dp).size(18.dp))
            Text(t("Додати заняття"))
        }
    }
}

private fun sameSubject(local: Course, remote: Course) =
    local.id == remote.id && local.titleEn == remote.titleEn && local.titleSk == remote.titleSk &&
        local.credits == remote.credits && local.semester == remote.semester &&
        local.gradingType == remote.gradingType && local.requirementType == remote.requirementType

private fun statusLabel(status: CourseStatus): String = t(when (status) {
    CourseStatus.FULFILLED -> "Здано"
    CourseStatus.ENROLLED -> "Активний"
    CourseStatus.PLANNED -> "Заплановано"
    CourseStatus.NOT_ENROLLED -> "Не записаний"
    CourseStatus.NOT_NEEDED -> "Не потрібний"
})

private fun sameRule(a: ScheduleRule, b: ScheduleRule) =
    a.courseId == b.courseId && a.dayOfWeek == b.dayOfWeek && a.startTime == b.startTime &&
        a.endTime == b.endTime && a.room == b.room && a.lessonType == b.lessonType && a.recurrence == b.recurrence &&
        (a.courseId != null || a.title == b.title)

private fun sameEvent(a: OneOffScheduleEvent, b: OneOffScheduleEvent) =
    a.courseId == b.courseId && a.date == b.date && a.startTime == b.startTime &&
        a.endTime == b.endTime && a.room == b.room && a.lessonType == b.lessonType &&
        (a.courseId != null || a.title == b.title)
