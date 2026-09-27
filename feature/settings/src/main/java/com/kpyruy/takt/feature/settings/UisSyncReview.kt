package com.kpyruy.takt.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.kpyruy.takt.core.data.UniversityAccountRepository
import com.kpyruy.takt.core.model.Course
import com.kpyruy.takt.core.model.CourseStatus
import com.kpyruy.takt.core.model.AppSettings
import kotlinx.coroutines.flow.flowOf
import com.kpyruy.takt.core.model.OneOffScheduleEvent
import com.kpyruy.takt.core.model.ScheduleRule
import com.kpyruy.takt.core.ui.components.SectionCard
import com.kpyruy.takt.core.ui.i18n.t
import kotlinx.coroutines.launch

@Composable
internal fun UisSyncReview(repository: UniversityAccountRepository) {
    val preview by repository.syncPreview.collectAsStateWithLifecycle()
    val data = preview ?: return
    val settings by (repository.settingsRepository?.settings ?: flowOf(AppSettings()))
        .collectAsStateWithLifecycle(initialValue = AppSettings())
    val scope = rememberCoroutineScope()
    var confirmReplacement by remember { mutableStateOf(false) }
    val progress = data.courses.mapNotNull { remote ->
        data.localCourses.firstOrNull { it.code == remote.code }?.let { local ->
            if (local.status != remote.status) local to remote else null
        }
    }
    val metadata = data.courses.filter { remote ->
        val local = data.localCourses.firstOrNull { it.code == remote.code }
        local == null || !sameSubject(local, remote)
    }
    val addedRules = data.rules.filter { remote -> data.localRules.none { sameRule(it, remote) } }
    val addedEvents = data.oneOffEvents.filter { remote -> data.localOneOffEvents.none { sameEvent(it, remote) } }
    val removedRules = data.localRules.filter { local -> data.rules.none { sameRule(it, local) } }
    val removedEvents = data.localOneOffEvents.filter { local -> data.oneOffEvents.none { sameEvent(it, local) } }

    SectionCard {
        Text(t("Порівняння з UIS"), style = MaterialTheme.typography.titleLarge)
        Text(t("Обери окремі зміни або залиш локальні дані."),
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (data.localPeriod != data.period) {
            Text(t("Навчальний період"), style = MaterialTheme.typography.titleMedium)
            Text(t("Локально: ${data.localPeriod?.studyStart ?: "—"} – ${data.localPeriod?.studyEnd ?: "—"}; іспити ${data.localPeriod?.examStart ?: "—"} – ${data.localPeriod?.examEnd ?: "—"}"))
            Text(t("UIS: ${data.period.studyStart} – ${data.period.studyEnd}; іспити ${data.period.examStart} – ${data.period.examEnd}"))
            OutlinedButton(onClick = { scope.launch { repository.applyPeriod() } }) {
                Text(t("Оновити період"))
            }
        }
        if (data.localEarnedCredits != data.earnedCredits || data.localRequiredCredits != data.requiredCredits) {
            Text(t("Кредити UIS"), style = MaterialTheme.typography.titleMedium)
            Text(t("Локально: ${data.localEarnedCredits ?: "—"} із ${data.localRequiredCredits ?: "—"}; UIS: ${data.earnedCredits ?: "—"} із ${data.requiredCredits ?: "—"}"))
            OutlinedButton(onClick = repository::applyCredits) { Text(t("Оновити кредити UIS")) }
        }
        if (progress.isNotEmpty()) {
            Text(t("Прогрес · ${progress.size} змін"), style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = { scope.launch { repository.applyAllProgress() } }) {
                Text(t("Оновити весь прогрес"))
            }
            progress.forEach { (local, remote) ->
                Text("${remote.code} · ${local.title}")
                Text("${statusLabel(local.status)} → ${statusLabel(remote.status)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { scope.launch { repository.applyProgress(remote.id) } }) {
                    Text(t("Оновити лише статус"))
                }
            }
        }
        if (metadata.isNotEmpty()) {
            Text(t("Предмети · ${metadata.size} змін"), style = MaterialTheme.typography.titleMedium)
            metadata.forEach { remote ->
                val local = data.localCourses.firstOrNull { it.code == remote.code }
                Text("${remote.code} · ${local?.title ?: "—"} → ${settings.displayCourseTitle(remote)}")
                TextButton(onClick = { scope.launch { repository.applyCourse(remote.id) } }) {
                    Text(t(if (local == null) "Додати предмет" else "Оновити предмет"))
                }
            }
        }
        if (addedRules.isNotEmpty() || addedEvents.isNotEmpty() || removedRules.isNotEmpty() || removedEvents.isNotEmpty()) {
            Text(t("Розклад · ${addedRules.size + addedEvents.size} нових, ${removedRules.size + removedEvents.size} лише локально"),
                style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = { confirmReplacement = true },
                modifier = Modifier.fillMaxWidth()) { Text(t("Замінити весь розклад розкладом UIS")) }
            addedRules.forEach { rule ->
                TimetableDifference("${rule.dayOfWeek} ${rule.startTime}–${rule.endTime} · ${rule.title} · ${rule.room.orEmpty()}") {
                    scope.launch { repository.addRule(rule.id) }
                }
            }
            addedEvents.forEach { event ->
                TimetableDifference("${event.date} ${event.startTime}–${event.endTime} · ${event.title} · ${event.room.orEmpty()}") {
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
            removedRules.isEmpty() && removedEvents.isEmpty() && data.localPeriod == data.period &&
            data.localEarnedCredits == data.earnedCredits && data.localRequiredCredits == data.requiredCredits) {
            Text(t("Дані збігаються"))
        }
        TextButton(onClick = repository::dismissPreview) { Text(t("Залишити локальні дані")) }
    }
    if (confirmReplacement) AlertDialog(
        onDismissRequest = { confirmReplacement = false },
        title = { Text(t("Замінити розклад?")) },
        text = { Text(t("Усі локальні заняття та їх зміни буде видалено. Якщо маєш резервну копію, з неї можна відновити розклад.")) },
        confirmButton = { TextButton(onClick = {
            confirmReplacement = false
            scope.launch { repository.replaceSchedule() }
        }) { Text(t("Замінити")) } },
        dismissButton = { TextButton(onClick = { confirmReplacement = false }) { Text(t("Скасувати")) } },
    )
}

@Composable
private fun TimetableDifference(label: String, onAdd: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label)
        TextButton(onClick = onAdd) { Text(t("Додати заняття")) }
    }
}

private fun sameSubject(local: Course, remote: Course) =
    local.titleEn == remote.titleEn && local.titleSk == remote.titleSk &&
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
