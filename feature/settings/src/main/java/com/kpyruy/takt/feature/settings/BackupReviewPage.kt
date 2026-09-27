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
import com.kpyruy.takt.core.data.BackupMerger
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
    val differing = remember(local, source) { BackupMerger.changedSections(local, source) }
    var selected by remember(review, differing) { mutableStateOf(review.initialSelection.intersect(differing)) }
    var applying by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val sourceName = if (review.source == BackupReviewSource.DOCUMENTS) "Documents/Takt" else t("Файл")
    val localByCode = local.courses.associateBy { it.code.uppercase(Locale.ROOT) }
    fun toggle(section: BackupSection) {
        selected = if (section in selected) selected - section else selected + section
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ScreenHeader(title = t("Порівняння даних"), navigation = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, t("Назад")) }
        })
        if (differing.isEmpty()) SectionCard {
            Text(t("Дані збігаються"), style = MaterialTheme.typography.titleMedium)
            Text(t("У файлі немає змін для перенесення на телефон."),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else SectionCard {
            Text(t("Обери джерело для кожної частини"), style = MaterialTheme.typography.titleMedium)
            Text(t("Позначено — взяти з файлу. Без позначки — залишити на телефоні."),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(t("Наприклад: можна взяти розклад з файлу, а прогрес і предмети залишити з телефона."),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (BackupSection.SUBJECTS in differing) BackupChoice(BackupSection.SUBJECTS, selected, ::toggle, sourceName, t("Предмети"),
            t("Телефон: ${local.courses.size} · $sourceName: ${source.courses.size}")) {
            source.courses.mapNotNull { course ->
                val old = localByCode[course.code.uppercase(Locale.ROOT)]
                if (old == null) "+ ${course.code} · ${course.title}"
                else buildList {
                    if (old.title != course.title) add(t("Назва"))
                    if (old.titleEn != course.titleEn) add("English")
                    if (old.titleSk != course.titleSk) add("Slovenčina")
                    if (old.credits != course.credits) add(t("Кредити"))
                    if (old.semester != course.semester) add(t("Семестр"))
                    if (old.requirementType != course.requirementType) add(t("Тип"))
                    if (old.gradingType != course.gradingType) add(t("Оцінювання"))
                    if (old.syllabusUrl != course.syllabusUrl) add(t("Програма предмета"))
                }.takeIf { it.isNotEmpty() }?.let { "${course.code} · ${it.joinToString(", ")}" }
            }.take(8).forEach { Text(it) }
            Text(t("Предмети, яких немає у файлі, залишаться на телефоні."),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (BackupSection.ICONS in differing) {
                Text(t("Іконки предметів обираються окремо нижче."),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        val changedIcons = source.courses.mapNotNull { file ->
            val phone = localByCode[file.code.uppercase(Locale.ROOT)]
            if (phone?.iconKey == file.iconKey) null else phone to file
        }
        if (BackupSection.ICONS in differing) BackupChoice(BackupSection.ICONS, selected, ::toggle, sourceName, t("Іконки предметів"),
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
        if (BackupSection.PROGRESS in differing) BackupChoice(BackupSection.PROGRESS, selected, ::toggle, sourceName, t("Прогрес"),
            "") {
            val sections = if (BackupSection.SUBJECTS in selected)
                setOf(BackupSection.SUBJECTS, BackupSection.PROGRESS) else setOf(BackupSection.PROGRESS)
            val preview = BackupMerger.merge(local, source, sections)
            Text(t("Оцінки: ${local.gradeItems.size} → ${preview.gradeItems.size}; задачі: ${local.studyTasks.size} → ${preview.studyTasks.size}"),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            preview.courses.mapNotNull { result ->
                val old = localByCode[result.code.uppercase(Locale.ROOT)]
                if (old == null || old.status != result.status || old.passFailResult != result.passFailResult)
                    "${result.code}: ${old?.status ?: "—"}/${old?.passFailResult ?: "—"} → ${result.status}/${result.passFailResult ?: "—"}"
                else null
            }.take(8).forEach { Text(it) }
            val localGrades = local.gradeItems.associateBy { it.id }
            val previewGrades = preview.gradeItems.associateBy { it.id }
            preview.gradeItems.filter { localGrades[it.id] != it }.take(5).forEach {
                Text("${if (it.id in localGrades) "~" else "+"} ${it.title} · ${it.earnedPoints}/${it.maxPoints}")
            }
            local.gradeItems.filter { it.id !in previewGrades }.take(3).forEach { Text("− ${it.title}") }
            val localTasks = local.studyTasks.associateBy { it.id }
            val previewTasks = preview.studyTasks.associateBy { it.id }
            preview.studyTasks.filter { localTasks[it.id] != it }.take(4).forEach {
                Text("${if (it.id in localTasks) "~" else "+"} ${it.title}")
            }
            local.studyTasks.filter { it.id !in previewTasks }.take(3).forEach { Text("− ${it.title}") }
            if (preview.gradeItems == local.gradeItems && preview.studyTasks == local.studyTasks &&
                preview.courses == local.courses && BackupSection.SUBJECTS !in selected) {
                Text(t("Для прогресу нових предметів також вибери «Предмети»."),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(t("Статуси, оцінки й задачі оновляться лише для предметів з однаковим кодом."),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (BackupSection.SCHEDULE in differing) BackupChoice(BackupSection.SCHEDULE, selected, ::toggle, sourceName, t("Розклад"),
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
        if (BackupSection.PERIODS in differing) BackupChoice(BackupSection.PERIODS, selected, ::toggle, sourceName, t("Періоди навчання"),
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
        if (BackupSection.MATERIALS in differing) BackupChoice(BackupSection.MATERIALS, selected, ::toggle, sourceName, t("Нотатки й матеріали"),
            t("Нотатки: ${local.courseNotes.size} → ${source.courseNotes.size}; матеріали: ${local.examMaterials.size} → ${source.examMaterials.size}")) {
            Text(t("Нотатки й матеріали зіставляються за кодом предмета."),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (BackupSection.LANGUAGE in differing) BackupChoice(
            BackupSection.LANGUAGE, selected, ::toggle, sourceName, t("Мова"), "") {
            BackupSettingChange(t("Мова застосунку"), languageName(local.settings.language),
                languageName(source.settings.language))
            BackupSettingChange(t("Мова назв предметів"), courseLanguageName(local.settings.courseNameLanguage),
                courseLanguageName(source.settings.courseNameLanguage))
            BackupSettingChange(t("Для української мови інтерфейсу"),
                courseLanguageName(local.settings.ukrainianCourseNameFallback),
                courseLanguageName(source.settings.ukrainianCourseNameFallback))
        }
        if (BackupSection.APPEARANCE in differing) BackupChoice(
            BackupSection.APPEARANCE, selected, ::toggle, sourceName, t("Вигляд"), "") {
            BackupSettingChange(t("Освітлення"), themeModeName(local.settings.themeMode),
                themeModeName(source.settings.themeMode))
            BackupSettingChange(t("Палітра"), themeFamilyName(local.settings.themeFamily),
                themeFamilyName(source.settings.themeFamily))
            BackupSettingChange(t("Стиль карток"), cardAppearanceName(local.settings.cardAppearance),
                cardAppearanceName(source.settings.cardAppearance))
        }
        if (BackupSection.CURRENT_SEMESTER in differing) BackupChoice(
            BackupSection.CURRENT_SEMESTER, selected, ::toggle, sourceName, t("Поточний семестр"), "") {
            BackupSettingChange(t("Поточний семестр"), local.settings.currentSemester?.toString() ?: t("Не вказано"),
                source.settings.currentSemester?.toString() ?: t("Не вказано"))
        }
        if (BackupSection.PREFERENCES in differing) BackupChoice(
            BackupSection.PREFERENCES, selected, ::toggle, sourceName, t("Інші налаштування"), "") {
            val phone = local.settings
            val file = source.settings
            if (phone.cancellationStyle != file.cancellationStyle) Text(t("Скасовані заняття"))
            if (phone.showHiddenLessons != file.showHiddenLessons) Text(t("Приховані заняття"))
            if (phone.parityOverride != file.parityOverride) Text(t("Парність тижня"))
            if (phone.weekLayout != file.weekLayout) Text(t("Вигляд тижня"))
            if (phone.homeWorkFilter != file.homeWorkFilter) Text(t("Фільтри задач"))
            if (phone.uisProgressFrequency != file.uisProgressFrequency ||
                phone.uisSubjectFrequency != file.uisSubjectFrequency ||
                phone.uisPeriodFrequency != file.uisPeriodFrequency ||
                phone.uisTimetableFrequency != file.uisTimetableFrequency ||
                phone.uisApplyProgressAutomatically != file.uisApplyProgressAutomatically) {
                Text(t("Автооновлення UIS"))
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (differing.isNotEmpty()) {
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
        if (summary.isNotBlank()) Text(summary, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

@Composable
private fun BackupSettingChange(label: String, phone: String, file: String) {
    if (phone == file) return
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("$phone → $file", style = MaterialTheme.typography.bodyMedium)
    }
}

private fun languageName(value: String): String = when (value) {
    "UKRAINIAN" -> t("Українська")
    "ENGLISH" -> "English"
    "SLOVAK" -> "Slovenčina"
    else -> value
}

private fun courseLanguageName(value: String): String = when (value) {
    "FOLLOW_APP" -> t("За мовою застосунку")
    "ENGLISH" -> "English"
    "SLOVAK" -> "Slovenčina"
    else -> value
}

private fun themeModeName(value: String): String = when (value) {
    "SYSTEM" -> t("Як телефон")
    "LIGHT" -> t("Світла")
    "DARK" -> t("Темна")
    else -> value
}

private fun themeFamilyName(value: String): String = when (value) {
    "BLUE" -> t("Синя")
    "GREEN" -> t("Зелена")
    "PURPLE" -> t("Фіолетова")
    "WARM" -> t("Тепла")
    "MONOCHROME" -> t("Монохром")
    else -> value
}

private fun cardAppearanceName(value: String): String = when (value) {
    "ELEVATED" -> t("Підняті")
    "TONAL_FILLED" -> t("Заливка")
    else -> value
}

private fun periodLabel(period: com.kpyruy.takt.core.data.BackupSemesterPeriod?): String =
    if (period == null) "—" else "${period.studyStart ?: "—"}–${period.studyEnd ?: "—"}; ${period.examStart ?: "—"}–${period.examEnd ?: "—"}"
