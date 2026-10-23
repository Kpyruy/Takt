package com.kpyruy.takt.feature.settings

import com.kpyruy.takt.core.model.uiMessage
import com.kpyruy.takt.core.model.UiKey
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
import com.kpyruy.takt.core.ui.components.TaktPrimaryButton as Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.kpyruy.takt.core.ui.components.TaktSurface as Surface
import androidx.compose.material3.Text
import com.kpyruy.takt.core.ui.components.TaktIconButton
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.data.BackupPayloadCodec
import com.kpyruy.takt.core.data.BackupMerger
import com.kpyruy.takt.core.data.BackupSection
import com.kpyruy.takt.core.data.BackupPayload
import com.kpyruy.takt.core.data.BackupTeaching
import com.kpyruy.takt.core.data.BackupTeacher
import com.kpyruy.takt.core.ui.components.CourseIcons
import com.kpyruy.takt.core.ui.components.ScreenHeader
import com.kpyruy.takt.core.ui.components.SectionCard
import com.kpyruy.takt.core.ui.i18n.t
import kotlinx.coroutines.launch
import java.util.Locale

enum class BackupReviewSource { JSON_FILE, DOCUMENTS }
data class BackupReviewState(
    val localRaw: String,
    val sourceRaw: String,
    val source: BackupReviewSource,
    val initialSelection: Set<BackupSection> = emptySet(),
)

@Composable
fun BackupReviewPage(
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
    val sourceName = if (review.source == BackupReviewSource.DOCUMENTS) "Documents/Takt" else t(UiKey.TEXT_FILE)
    val localByCode = local.courses.associateBy { it.code.uppercase(Locale.ROOT) }
    fun toggle(section: BackupSection) {
        selected = if (section in selected) selected - section else selected + section
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ScreenHeader(title = t(UiKey.TEXT_COMPARE_DATA), navigation = {
            TaktIconButton(Icons.AutoMirrored.Outlined.ArrowBack, t(UiKey.ACTION_BACK), onBack)
        })
        if (differing.isEmpty()) SectionCard {
            Text(t(UiKey.TEXT_DATA_MATCHES), style = MaterialTheme.typography.titleMedium)
            Text(t(UiKey.TEXT_THE_FILE_HAS_NO_CHANGES_TO_BRING_TO_THE_PHONE),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (BackupSection.SUBJECTS in differing) BackupChoice(BackupSection.SUBJECTS, selected, ::toggle, sourceName, t(UiKey.TEXT_SUBJECTS),
            t(uiMessage(UiKey.FORMAT_PHONE_VALUE_VALUE_VALUE, local.courses.size, sourceName, source.courses.size))) {
            source.courses.mapNotNull { course ->
                val old = localByCode[course.code.uppercase(Locale.ROOT)]
                if (old == null) "+ ${course.code} · ${course.title}"
                else buildList {
                    if (old.title != course.title) add(t(UiKey.COMMON_NAME))
                    if (old.titleEn != course.titleEn) add(t(UiKey.TEXT_ENGLISH))
                    if (old.titleSk != course.titleSk) add(t(UiKey.TEXT_SLOVEN_INA))
                    if (old.credits != course.credits) add(t(UiKey.TEXT_CREDITS_2A6B24AD))
                    if (old.semester != course.semester) add(t(UiKey.TEXT_SEMESTER_A83EF1C1))
                    if (old.requirementType != course.requirementType) add(t(UiKey.WORK_TYPE))
                    if (old.gradingType != course.gradingType) add(t(UiKey.TEXT_ASSESSMENT_7808EEEB))
                    if (old.syllabusUrl != course.syllabusUrl) add(t(UiKey.TEXT_COURSE_SYLLABUS))
                }.takeIf { it.isNotEmpty() }?.let { "${course.code} · ${it.joinToString(", ")}" }
            }.take(8).forEach { Text(it) }
            Text(t(UiKey.TEXT_COURSES_ABSENT_FROM_THE_FILE_STAY_ON_THE_PHONE),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (BackupSection.ICONS in differing) {
                Text(t(UiKey.TEXT_CHOOSE_COURSE_ICONS_SEPARATELY_BELOW),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        val changedIcons = source.courses.mapNotNull { file ->
            val phone = localByCode[file.code.uppercase(Locale.ROOT)]
            if (phone?.iconKey == file.iconKey && phone?.colorKey == file.colorKey) null else phone to file
        }
        if (BackupSection.ICONS in differing) BackupChoice(BackupSection.ICONS, selected, ::toggle, sourceName, t(UiKey.BACKUP_COURSE_APPEARANCE),
            t(UiKey.TEXT_DIFFERENT) + changedIcons.size) {
            if (changedIcons.isEmpty()) {
                Text(t(UiKey.TEXT_ICONS_MATCH_OR_ARE_ABSENT_IN_BOTH_SOURCES),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            changedIcons.take(6).forEach { (phone, file) ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BackupIcon(phone?.iconKey, phone?.colorKey)
                    Text("→", style = MaterialTheme.typography.titleMedium)
                    BackupIcon(file.iconKey, file.colorKey)
                    Text("${file.code} · ${file.title}", modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(t(UiKey.TEXT_LEFT_PHONE_RIGHT_FILE_YOU_CAN_IMPORT_ICONS_WITHOUT_REPLACING_C_BD902093),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (BackupSection.PROGRESS in differing) BackupChoice(BackupSection.PROGRESS, selected, ::toggle, sourceName, t(UiKey.TEXT_PROGRESS),
            "") {
            val sections = if (BackupSection.SUBJECTS in selected)
                setOf(BackupSection.SUBJECTS, BackupSection.PROGRESS) else setOf(BackupSection.PROGRESS)
            val preview = BackupMerger.merge(local, source, sections)
            Text(t(uiMessage(UiKey.FORMAT_GRADES_VALUE_VALUE_TASKS_VALUE_VALUE, local.gradeItems.size, preview.gradeItems.size, local.studyTasks.size, preview.studyTasks.size)),
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
                Text(t(UiKey.TEXT_TO_IMPORT_PROGRESS_FOR_NEW_COURSES_ALSO_SELECT_COURSES),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(t(UiKey.TEXT_STATUSES_GRADES_AND_TASKS_UPDATE_ONLY_FOR_COURSES_WITH_THE_SAME_CODE),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (BackupSection.SCHEDULE in differing) BackupChoice(BackupSection.SCHEDULE, selected, ::toggle, sourceName, t(UiKey.TEXT_TIMETABLE),
            t(uiMessage(UiKey.FORMAT_PHONE_VALUE_VALUE_VALUE, local.scheduleRules.size + local.oneOffEvents.size, sourceName, source.scheduleRules.size + source.oneOffEvents.size))) {
            BackupScheduleChanges(local, source)
            Text(t(UiKey.TEXT_CHOOSING_THE_TIMETABLE_REPLACES_LOCAL_CLASSES_CHANGES_AND_ATTENDANCE),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (BackupSection.PERIODS in differing) BackupChoice(BackupSection.PERIODS, selected, ::toggle, sourceName, t(UiKey.TEXT_STUDY_PERIODS),
            t(uiMessage(UiKey.FORMAT_PHONE_VALUE_VALUE_VALUE, local.settings.semesterPeriods.size, sourceName, source.settings.semesterPeriods.size))) {
            (local.settings.semesterPeriods.map { it.semester } + source.settings.semesterPeriods.map { it.semester })
                .distinct().sorted().forEach { semester ->
                    val a = local.settings.semesterPeriods.firstOrNull { it.semester == semester }
                    val b = source.settings.semesterPeriods.firstOrNull { it.semester == semester }
                    if (a != b) {
                        Text(t(uiMessage(UiKey.FORMAT_SEMESTER_VALUE, semester)), style = MaterialTheme.typography.titleSmall)
                        Text("${t(UiKey.TEXT_PHONE)}: ${periodLabel(a)}")
                        Text("$sourceName: ${periodLabel(b)}")
                    }
                }
        }
        if (BackupSection.MATERIALS in differing) BackupChoice(BackupSection.MATERIALS, selected, ::toggle, sourceName, t(UiKey.TEXT_NOTES_AND_MATERIALS),
            t(uiMessage(UiKey.FORMAT_NOTES_VALUE_VALUE_MATERIALS_VALUE_VALUE, local.courseNotes.size, source.courseNotes.size, local.examMaterials.size, source.examMaterials.size))) {
            BackupNoteChanges(local, source)
            Text(t(UiKey.TEXT_NOTES_AND_MATERIALS_ARE_MATCHED_BY_COURSE_CODE),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (BackupSection.TEACHERS in differing) BackupChoice(
            BackupSection.TEACHERS, selected, ::toggle, sourceName, t(UiKey.TEXT_TEACHERS_AND_CONSULTATIONS), "") {
            val sections = setOf(BackupSection.TEACHERS) +
                if (BackupSection.SUBJECTS in selected) setOf(BackupSection.SUBJECTS) else emptySet()
            BackupTeachingChanges(local, BackupMerger.merge(local, source, sections), sourceName)
        }
        if (BackupSection.STUDIES in differing) BackupChoice(
            BackupSection.STUDIES, selected, ::toggle, sourceName, t(UiKey.STUDIES_TITLE), "") {
            BackupSettingChange(t(UiKey.STUDIES_PROFILE),
                local.studentHub?.identity?.name ?: t(UiKey.TEXT_UNSPECIFIED),
                source.studentHub?.identity?.name ?: t(UiKey.TEXT_UNSPECIFIED))
            BackupSettingChange(t(UiKey.STUDIES_DISPLAY_NAME),
                local.studentHub?.displayName?.takeIf { it.isNotBlank() } ?: t(UiKey.TEXT_UNSPECIFIED),
                source.studentHub?.displayName?.takeIf { it.isNotBlank() } ?: t(UiKey.TEXT_UNSPECIFIED))
            BackupSettingChange(t(UiKey.STUDIES_HISTORY),
                local.studentHub?.studies?.joinToString(" · ") { it.title } ?: t(UiKey.TEXT_UNSPECIFIED),
                source.studentHub?.studies?.joinToString(" · ") { it.title } ?: t(UiKey.TEXT_UNSPECIFIED))
        }
        if (BackupSection.LANGUAGE in differing) BackupChoice(
            BackupSection.LANGUAGE, selected, ::toggle, sourceName, t(UiKey.TEXT_LANGUAGE), "") {
            BackupSettingChange(t(UiKey.TEXT_APPLICATION_LANGUAGE), languageName(local.settings.language),
                languageName(source.settings.language))
            BackupSettingChange(t(UiKey.TEXT_COURSE_NAME_LANGUAGE), courseLanguageName(local.settings.courseNameLanguage),
                courseLanguageName(source.settings.courseNameLanguage))
            BackupSettingChange(t(UiKey.TEXT_WHEN_THE_APP_IS_IN_UKRAINIAN),
                courseLanguageName(local.settings.ukrainianCourseNameFallback),
                courseLanguageName(source.settings.ukrainianCourseNameFallback))
        }
        if (BackupSection.APPEARANCE in differing) BackupChoice(
            BackupSection.APPEARANCE, selected, ::toggle, sourceName, t(UiKey.TEXT_APPEARANCE), "") {
            BackupAppearanceChanges(local.settings, source.settings, sourceName)
        }
        if (BackupSection.CURRENT_SEMESTER in differing) BackupChoice(
            BackupSection.CURRENT_SEMESTER, selected, ::toggle, sourceName, t(UiKey.TEXT_CURRENT_SEMESTER), "") {
            BackupSettingChange(t(UiKey.TEXT_CURRENT_SEMESTER), local.settings.currentSemester?.toString() ?: t(UiKey.TEXT_UNSPECIFIED),
                source.settings.currentSemester?.toString() ?: t(UiKey.TEXT_UNSPECIFIED))
        }
        if (BackupSection.PREFERENCES in differing) BackupChoice(
            BackupSection.PREFERENCES, selected, ::toggle, sourceName, t(UiKey.TEXT_OTHER_SETTINGS), "") {
            val sections = setOf(BackupSection.PREFERENCES) +
                if (BackupSection.SUBJECTS in selected) setOf(BackupSection.SUBJECTS) else emptySet()
            BackupPreferenceChanges(local, BackupMerger.merge(local, source, sections), sourceName)
        }
        if (BackupSection.PROGRESS in selected || BackupSection.SCHEDULE in selected) {
            Text(t(UiKey.TEXT_TESTS_WITHOUT_A_MATCHING_CLASS_WILL_KEEP_THEIR_DATE_WITHOUT_A_CLASS_LINK),
                modifier = Modifier.testTag("backup-lesson-link-notice"), style = MaterialTheme.typography.bodySmall)
        }
        error?.let { Text(t(it), color = MaterialTheme.colorScheme.error) }
        if (differing.isNotEmpty()) {
            Button(onClick = { scope.launch {
                applying = true
                runCatching { onApply(selected) }.onSuccess { onBack() }
                    .onFailure { error = it.message ?: uiMessage(UiKey.TEXT_COULD_NOT_APPLY_SELECTED_DATA) }
                applying = false
            } }, enabled = selected.isNotEmpty() && !applying, modifier = Modifier.fillMaxWidth()) {
                Text(t(UiKey.TEXT_APPLY_SELECTED_DATA))
            }
        }
    }
}

@Composable
private fun BackupChoice(section: BackupSection, selected: Set<BackupSection>, onToggle: (BackupSection) -> Unit,
    sourceName: String, title: String, summary: String, detail: @Composable () -> Unit) {
    var expanded by remember(section) { mutableStateOf(false) }
    SectionCard(Modifier.testTag("backup-section-${section.name}")) {
        Row(Modifier.fillMaxWidth().testTag("backup-choice-${section.name}").clickable { onToggle(section) }, verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = section in selected, onCheckedChange = null)
            androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(if (section in selected) t(UiKey.TEXT_USE) + sourceName else t(UiKey.TEXT_KEEP_ON_PHONE),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (section in selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { expanded = !expanded }, modifier = Modifier.testTag("backup-expand-${section.name}")) {
                Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, t(UiKey.TEXT_REVIEW_CHANGES))
            }
        }
        if (summary.isNotBlank()) Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        AnimatedVisibility(expanded) { Column(Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { detail() } }
    }
}

@Composable
private fun BackupIcon(key: String?, colorKey: String? = null) {
    Surface(color = com.kpyruy.takt.core.model.CourseColor.fromKey(colorKey)?.let {
        androidx.compose.ui.graphics.Color(it.argb).copy(alpha = .22f)
    } ?: MaterialTheme.colorScheme.secondaryContainer,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)) {
        val icon = CourseIcons.find(key)
        if (icon == null) Text("—", modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
            style = MaterialTheme.typography.titleSmall)
        else Icon(icon.vector, contentDescription = t(icon.label),
            modifier = Modifier.padding(8.dp).size(20.dp))
    }
}

@Composable
private fun BackupTeachingChanges(local: BackupPayload, result: BackupPayload, sourceName: String) {
    val phone = local.teaching ?: BackupTeaching()
    val file = result.teaching ?: BackupTeaching()
    if (phone == file) {
        Text(t(UiKey.TEXT_TO_INCLUDE_TEACHERS_FOR_NEW_COURSES_ALSO_SELECT_COURSES),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    fun code(value: String) = value.trim().uppercase(Locale.ROOT)
    val phoneTeams = phone.teams.associateBy { code(it.courseCode) }
    val fileTeams = file.teams.associateBy { code(it.courseCode) }
    (phoneTeams.keys + fileTeams.keys).sorted().forEach { courseCode ->
        val old = phoneTeams[courseCode]
        val new = fileTeams[courseCode]
        if (old != new) Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("$courseCode · ${t(UiKey.TEXT_TEACHING_TEAM)}", style = MaterialTheme.typography.titleSmall)
            Text("${t(UiKey.TEXT_PHONE)}: ${teacherTeamLabel(old?.teachers.orEmpty())}")
            Text("$sourceName: ${teacherTeamLabel(new?.teachers.orEmpty())}")
        }
    }
    val phoneProfiles = phone.profiles.associateBy { it.personId to it.language }
    val fileProfiles = file.profiles.associateBy { it.personId to it.language }
    (phoneProfiles.keys + fileProfiles.keys).sortedBy { key ->
        (fileProfiles[key]?.name ?: phoneProfiles[key]?.name).orEmpty() + key.second
    }.forEach { key ->
        val old = phoneProfiles[key]
        val new = fileProfiles[key]
        if (old != new) Column(Modifier.testTag("backup-teacher-profile-${key.first}-${key.second}"),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val courseCodes = (phone.teams + file.teams).filter { team -> team.teachers.any { it.personId == key.first } }
                .map { code(it.courseCode) }.distinct().sorted()
            Text(listOfNotNull((new ?: old)?.name, courseCodes.joinToString(" · ").takeIf { it.isNotBlank() },
                languageName(key.second)).joinToString(" · "), style = MaterialTheme.typography.titleSmall)
            val fields = listOf(
                Triple(t(UiKey.TEXT_TEACHER_NAME), old?.name, new?.name),
                Triple(t(UiKey.TEXT_POSITION), old?.position, new?.position),
                Triple(t(UiKey.TEXT_UNIVERSITY_EMAIL), old?.universityEmail, new?.universityEmail),
                Triple(t(UiKey.TEXT_UIS_EMAIL), old?.systemEmail, new?.systemEmail),
                Triple(t(UiKey.TEXT_ADDRESS), old?.officeAddress, new?.officeAddress),
                Triple(t(UiKey.TEXT_OFFICE), old?.officeNumber, new?.officeNumber),
                Triple(t(UiKey.TEXT_OFFICE_PHONE), old?.officePhone, new?.officePhone),
                Triple(t(UiKey.TEXT_CONSULTATION_HOURS), old?.consultationHours, new?.consultationHours),
                Triple(t(UiKey.TEXT_UNIVERSITY_MAILBOX_LAST_CHECKED), old?.mailboxLastChecked, new?.mailboxLastChecked),
            )
            fields.filter { it.second != it.third }.forEach { (label, a, b) ->
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${t(UiKey.TEXT_PHONE)}: ${a ?: t(UiKey.TEXT_UNSPECIFIED)}")
                Text("$sourceName: ${b ?: t(UiKey.TEXT_UNSPECIFIED)}")
            }
        }
    }
}

private fun teacherTeamLabel(teachers: List<BackupTeacher>): String = teachers.joinToString("; ") { teacher ->
    val roles = teacher.roles.map { role -> t(when (role.lowercase(Locale.ROOT)) {
        "examiner", "skúšajúci", "zkoušející" -> UiKey.TEXT_EXAMINER
        "instructor", "cvičiaci", "cvičící" -> UiKey.TEXT_TUTORIALS
        "lecturer", "prednášajúci", "přednášející" -> UiKey.TEXT_LECTURES
        "person responsible for course", "garant predmetu", "garant předmětu", "supervisor" -> UiKey.TEXT_COURSE_SUPERVISOR
        else -> role
    }) }
    (listOf(teacher.name) + roles + teacher.languages).joinToString(" · ")
}.ifBlank { t(UiKey.TEXT_UNSPECIFIED) }

@Composable
internal fun BackupSettingChange(label: String, phone: String, file: String) {
    if (phone == file) return
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("$phone → $file", style = MaterialTheme.typography.bodyMedium)
    }
}

private fun languageName(value: String): String = when (value) {
    "UKRAINIAN" -> t(UiKey.TEXT_UKRAINIAN)
    "ENGLISH" -> t(UiKey.TEXT_ENGLISH)
    "SLOVAK" -> t(UiKey.TEXT_SLOVEN_INA)
    else -> value
}

private fun courseLanguageName(value: String): String = when (value) {
    "FOLLOW_APP" -> t(UiKey.TEXT_FOLLOW_APP_LANGUAGE)
    "ENGLISH" -> t(UiKey.TEXT_ENGLISH)
    "SLOVAK" -> t(UiKey.TEXT_SLOVEN_INA)
    else -> value
}

private fun themeModeName(value: String): String = when (value) {
    "SYSTEM" -> t(UiKey.TEXT_DEVICE_THEME)
    "LIGHT" -> t(UiKey.TEXT_LIGHT)
    "DARK" -> t(UiKey.TEXT_DARK)
    else -> value
}

private fun themeFamilyName(value: String): String = when (value) {
    "BLUE" -> t(UiKey.TEXT_BLUE)
    "GREEN" -> t(UiKey.TEXT_GREEN)
    "PURPLE" -> t(UiKey.TEXT_VIOLET)
    "WARM" -> t(UiKey.TEXT_WARM)
    "MONOCHROME" -> t(UiKey.TEXT_MONOCHROME)
    else -> value
}

private fun cardAppearanceName(value: String): String = when (value) {
    "GLASS" -> t(UiKey.TEXT_GLASS)
    "ELEVATED" -> t(UiKey.TEXT_ELEVATED)
    "TONAL_FILLED" -> t(UiKey.TEXT_FILLED)
    else -> value
}

private fun periodLabel(period: com.kpyruy.takt.core.data.BackupSemesterPeriod?): String =
    if (period == null) "—" else "${period.studyStart ?: "—"}–${period.studyEnd ?: "—"}; ${period.examStart ?: "—"}–${period.examEnd ?: "—"}"
