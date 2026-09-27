package com.kpyruy.takt.feature.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.data.BackupPayloadCodec
import com.kpyruy.takt.core.data.BackupSection
import com.kpyruy.takt.core.ui.components.CourseIcons
import com.kpyruy.takt.core.ui.components.ScreenHeader
import com.kpyruy.takt.core.ui.components.SectionCard
import com.kpyruy.takt.core.ui.i18n.t
import kotlinx.coroutines.launch
import java.util.Locale

internal enum class BackupReviewSource { JSON_FILE, DOCUMENTS }
internal data class BackupReviewState(
    val localRaw: String,
    val sourceRaw: String,
    val source: BackupReviewSource,
    val initialSelection: Set<BackupSection> = emptySet(),
)

@Composable
internal fun BackupReviewPage(
    review: BackupReviewState,
    onBack: () -> Unit,
    onApply: suspend (Set<BackupSection>) -> Unit,
) {
    BackHandler(onBack = onBack)
    val local = remember(review.localRaw) { BackupPayloadCodec.decode(review.localRaw) }
    val source = remember(review.sourceRaw) { BackupPayloadCodec.decode(review.sourceRaw) }
    var selected by remember(review) { mutableStateOf(review.initialSelection) }
    var applying by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val sourceName = if (review.source == BackupReviewSource.DOCUMENTS) "Documents/Takt" else t("Файл")
    val localByCode = local.courses.associateBy { it.code.uppercase(Locale.ROOT) }
    val sourceByCode = source.courses.associateBy { it.code.uppercase(Locale.ROOT) }
    fun toggle(section: BackupSection) {
        selected = if (section in selected) selected - section else selected + section
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ScreenHeader(title = t("Порівняння даних"), navigation = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, t("Назад")) }
        })
        SectionCard {
            Text(t("Обери джерело для кожної частини"), style = MaterialTheme.typography.titleMedium)
            Text(t("Позначено — взяти з файлу. Без позначки — залишити на телефоні."),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(t("Наприклад: можна взяти розклад з файлу, а прогрес і предмети залишити з телефона."),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        BackupChoice(BackupSection.SUBJECTS, selected, ::toggle, sourceName, t("Предмети"),
            t("Телефон: ${local.courses.size} · $sourceName: ${source.courses.size}")) {
            source.courses.filter { localByCode[it.code.uppercase(Locale.ROOT)]?.let { old ->
                old.title != it.title || old.credits != it.credits || old.semester != it.semester
            } ?: true }.take(8).forEach { course ->
                Text("${course.code} · ${localByCode[course.code.uppercase(Locale.ROOT)]?.title ?: "—"} → ${course.title}")
            }
            local.courses.filter { it.code.uppercase(Locale.ROOT) !in sourceByCode }.take(4).forEach {
                Text(t("Лише на телефоні: ${it.code} · ${it.title}"))
            }
            Text(t("Іконки предметів обираються окремо нижче. Предмети, яких немає у файлі, залишаться на телефоні."),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val changedIcons = source.courses.mapNotNull { file ->
            val phone = localByCode[file.code.uppercase(Locale.ROOT)]
            if (phone?.iconKey == file.iconKey) null else phone to file
        }
        BackupChoice(BackupSection.ICONS, selected, ::toggle, sourceName, t("Іконки предметів"),
            t("Відрізняються: ") + changedIcons.size) {
            if (changedIcons.isEmpty()) {
                Text(t("Іконки однакові або відсутні в обох джерелах."),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            changedIcons.take(6).forEach { (phone, file) ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BackupIcon(phone?.iconKey)
                    Text("→", style = MaterialTheme.typography.titleMedium)
                    BackupIcon(file.iconKey)
                    Text("${file.code} · ${file.title}", modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(t("Ліворуч — телефон, праворуч — файл. Іконки можна взяти без заміни предметів чи прогресу."),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        BackupChoice(BackupSection.PROGRESS, selected, ::toggle, sourceName, t("Прогрес"),
            t("Оцінки: ${local.gradeItems.size} → ${source.gradeItems.size}; задачі: ${local.studyTasks.size} → ${source.studyTasks.size}")) {
            source.courses.mapNotNull { fileCourse ->
                localByCode[fileCourse.code.uppercase(Locale.ROOT)]?.takeIf {
                    it.status != fileCourse.status || it.passFailResult != fileCourse.passFailResult
                }?.let {
                    "${fileCourse.code}: ${it.status}/${it.passFailResult ?: "—"} → ${fileCourse.status}/${fileCourse.passFailResult ?: "—"}"
                }
            }.take(8).forEach { Text(it) }
            val localGrades = local.gradeItems.associateBy { it.id }
            val sourceGrades = source.gradeItems.associateBy { it.id }
            source.gradeItems.filter { localGrades[it.id]?.copy(courseId = it.courseId) != it }
                .take(5).forEach { Text("${if (it.id in localGrades) "~" else "+"} ${it.title} · ${it.earnedPoints}/${it.maxPoints}") }
            local.gradeItems.filter { it.id !in sourceGrades }.take(3).forEach { Text("− ${it.title}") }
            Text(t("Статуси, оцінки й задачі оновляться лише для предметів з однаковим кодом."),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        BackupChoice(BackupSection.SCHEDULE, selected, ::toggle, sourceName, t("Розклад"),
            t("Телефон: ${local.scheduleRules.size + local.oneOffEvents.size} · $sourceName: ${source.scheduleRules.size + source.oneOffEvents.size}")) {
            val localRules = local.scheduleRules.associateBy { it.id }
            val sourceRules = source.scheduleRules.associateBy { it.id }
            val localEvents = local.oneOffEvents.associateBy { it.id }
            val sourceEvents = source.oneOffEvents.associateBy { it.id }
            source.scheduleRules.filter { localRules[it.id] != it }.take(6).forEach {
                Text("${if (it.id in localRules) "~" else "+"} ${it.dayOfWeek} · ${it.startMinute / 60}:${(it.startMinute % 60).toString().padStart(2, '0')} · ${it.title}")
            }
            source.oneOffEvents.filter { localEvents[it.id] != it }.take(4).forEach {
                Text("${if (it.id in localEvents) "~" else "+"} ${it.title}")
            }
            local.scheduleRules.filter { it.id !in sourceRules }.take(3).forEach { Text("− ${it.title}") }
            local.oneOffEvents.filter { it.id !in sourceEvents }.take(3).forEach { Text("− ${it.title}") }
            Text(t("Вибір розкладу замінить локальні заняття, зміни й відвідування."),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        BackupChoice(BackupSection.PERIODS, selected, ::toggle, sourceName, t("Періоди навчання"),
            t("Телефон: ${local.settings.semesterPeriods.size} · $sourceName: ${source.settings.semesterPeriods.size}")) {
            (local.settings.semesterPeriods.map { it.semester } + source.settings.semesterPeriods.map { it.semester })
                .distinct().sorted().forEach { semester ->
                    val a = local.settings.semesterPeriods.firstOrNull { it.semester == semester }
                    val b = source.settings.semesterPeriods.firstOrNull { it.semester == semester }
                    if (a != b) {
                        Text(t("$semester семестр"), style = MaterialTheme.typography.titleSmall)
                        Text("${t("Телефон")}: ${periodLabel(a)}")
                        Text("$sourceName: ${periodLabel(b)}")
                    }
                }
        }
        BackupChoice(BackupSection.MATERIALS, selected, ::toggle, sourceName, t("Нотатки й матеріали"),
            t("Нотатки: ${local.courseNotes.size} → ${source.courseNotes.size}; матеріали: ${local.examMaterials.size} → ${source.examMaterials.size}")) {
            Text(t("Нотатки й матеріали зіставляються за кодом предмета."),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        BackupChoice(BackupSection.PREFERENCES, selected, ::toggle, sourceName, t("Інші налаштування"),
            t("Мова застосунку: ${local.settings.language} → ${source.settings.language}")) {
            Text(t("Мова, вигляд, фільтри й поточний семестр."),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Text(t("Після застосування підключений сейф Documents/Takt оновиться автоматично."),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = { scope.launch {
            applying = true
            runCatching { onApply(selected) }.onSuccess { onBack() }
                .onFailure { error = it.message ?: t("Не вдалося застосувати вибрані дані") }
            applying = false
        } }, enabled = selected.isNotEmpty() && !applying, modifier = Modifier.fillMaxWidth()) {
            Text(t("Застосувати вибрані дані"))
        }
    }
}

@Composable
private fun BackupChoice(section: BackupSection, selected: Set<BackupSection>, onToggle: (BackupSection) -> Unit,
    sourceName: String, title: String, summary: String, detail: @Composable () -> Unit) {
    SectionCard {
        Row(Modifier.fillMaxWidth().clickable { onToggle(section) }, verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = section in selected, onCheckedChange = null)
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(if (section in selected) t("Взяти з ") + sourceName else t("Залишити на телефоні"),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (section in selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(summary, color = MaterialTheme.colorScheme.onSurfaceVariant)
        detail()
    }
}

@Composable
private fun BackupIcon(key: String?) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)) {
        val icon = CourseIcons.find(key)
        if (icon == null) Text("—", modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
            style = MaterialTheme.typography.titleSmall)
        else Icon(icon.vector, contentDescription = icon.label,
            modifier = Modifier.padding(8.dp).size(20.dp))
    }
}

private fun periodLabel(period: com.kpyruy.takt.core.data.BackupSemesterPeriod?): String =
    if (period == null) "—" else "${period.studyStart ?: "—"}–${period.studyEnd ?: "—"}; ${period.examStart ?: "—"}–${period.examEnd ?: "—"}"
