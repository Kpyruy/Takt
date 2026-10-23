package com.kpyruy.takt.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.data.BackupCourse
import com.kpyruy.takt.core.data.BackupScholarshipPayment
import com.kpyruy.takt.core.data.BackupStudentFee
import com.kpyruy.takt.core.data.BackupStudentHub
import com.kpyruy.takt.core.model.UiKey
import com.kpyruy.takt.core.model.CourseColor
import com.kpyruy.takt.core.ui.components.CourseIcons
import com.kpyruy.takt.core.ui.i18n.TaktI18n
import com.kpyruy.takt.core.ui.i18n.t
import com.kpyruy.takt.core.ui.i18n.tf
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Currency

/** Mirrors the complete Studies selection without exposing university IDs or account hashes. */
@Composable
internal fun BackupStudyChanges(phone: BackupStudentHub?, file: BackupStudentHub?, sourceName: String) {
    val groups = studyChanges(phone, file)
    Column(Modifier.fillMaxWidth().testTag("backup-study-changes"), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        groups.forEach { group ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(group.title, style = MaterialTheme.typography.titleSmall)
                group.fields.forEach { field ->
                    Column(Modifier.testTag("backup-study-field-${field.key}"), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(t(field.key), style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${t(UiKey.TEXT_PHONE)}: ${field.phone}", style = MaterialTheme.typography.bodyMedium)
                        Text("$sourceName: ${field.file}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

private data class StudyChange(val key: String, val phone: String, val file: String)
private data class StudyChangeGroup(val title: String, val fields: List<StudyChange>)

private class StudyFields {
    val changes = mutableListOf<StudyChange>()
    fun <T> field(key: String, phone: T?, file: T?, format: (T) -> String = { it.toString() }) {
        if (phone != file) changes += StudyChange(key, phone?.let(format) ?: missing(), file?.let(format) ?: missing())
    }
    fun <T> sourcedField(key: String, phone: T?, file: T?, phoneFormat: (T) -> String, fileFormat: (T) -> String) {
        if (phone != file) changes += StudyChange(key, phone?.let(phoneFormat) ?: missing(), file?.let(fileFormat) ?: missing())
    }
    fun linkage(key: String, phone: String?, file: String?) {
        field(key, phone, file) { t(UiKey.BACKUP_STUDY_SAVED_LINK) }
        if (phone != null && file != null && phone != file) {
            changes[changes.lastIndex] = StudyChange(key, t(UiKey.BACKUP_STUDY_EXISTING_RECORD), t(UiKey.BACKUP_STUDY_UPDATED_RECORD))
        }
    }
    fun fallback() {
        if (changes.isEmpty()) changes += StudyChange(UiKey.BACKUP_STUDY_SAVED_RECORD,
            t(UiKey.BACKUP_STUDY_EXISTING_RECORD), t(UiKey.BACKUP_STUDY_UPDATED_RECORD))
    }
}

private fun studyChanges(phone: BackupStudentHub?, file: BackupStudentHub?): List<StudyChangeGroup> = buildList {
    if (phone == file) return@buildList
    fun group(title: String, changed: Boolean, explainEmpty: Boolean = true, fields: StudyFields.() -> Unit) {
        if (!changed) return
        val values = StudyFields().apply(fields)
        if (values.changes.isEmpty() && !explainEmpty) return
        values.fallback()
        add(StudyChangeGroup(title, values.changes))
    }
    group(t(UiKey.STUDIES_PROFILE), phone?.copy(studies = emptyList()) != file?.copy(studies = emptyList()) ||
        phone?.studies?.map { it.id } != file?.studies?.map { it.id }) {
        field(UiKey.BACKUP_STUDY_SAVED_RECORD, phone?.let { true }, file?.let { true }, ::yesNo)
        field(UiKey.STUDIES_NAME_IN_UIS, phone?.identity?.name, file?.identity?.name)
        field(UiKey.STUDIES_DISPLAY_NAME, phone?.displayName, file?.displayName) { it.ifBlank { missing() } }
        field(UiKey.STUDIES_UNIVERSITY, phone?.identity?.university, file?.identity?.university)
        field(UiKey.BACKUP_STUDY_FACULTY, phone?.identity?.faculty, file?.identity?.faculty)
        field(UiKey.BACKUP_STUDY_STUDENT_NUMBER, phone?.identity?.studentNumber, file?.identity?.studentNumber)
        sourcedField(UiKey.STUDIES_CURRENT, phone?.currentStudyId, file?.currentStudyId,
            { studyName(phone, it) }, { studyName(file, it) })
        sourcedField(UiKey.BACKUP_STUDY_VIEWED, phone?.viewedStudyId, file?.viewedStudyId,
            { studyName(phone, it) }, { studyName(file, it) })
        field(UiKey.STUDIES_INCLUDE_PREVIOUS, phone?.showPreviousStudies, file?.showPreviousStudies, ::yesNo)
        sourcedField(UiKey.STUDIES_HISTORY, phone?.studies?.map { it.id }, file?.studies?.map { it.id },
            { ids -> ids.joinToString(" · ") { studyName(phone, it) }.ifBlank { missing() } },
            { ids -> ids.joinToString(" · ") { studyName(file, it) }.ifBlank { missing() } })
        linkage(UiKey.BACKUP_STUDY_PROFILE_LINK, phone?.identity?.personId, file?.identity?.personId)
        linkage(UiKey.BACKUP_STUDY_ACCOUNT_LINK, phone?.ownerKey, file?.ownerKey)
    }
    val oldStudies = phone?.studies.orEmpty().associateBy { it.id }
    val newStudies = file?.studies.orEmpty().associateBy { it.id }
    (oldStudies.keys + newStudies.keys).forEach { id ->
        val a = oldStudies[id]
        val b = newStudies[id]
        group((b ?: a)!!.title, a != b,
            explainEmpty = a?.courses == b?.courses && a?.fees == b?.fees && a?.scholarships == b?.scholarships) {
            field(UiKey.BACKUP_STUDY_SAVED_RECORD, a?.let { true }, b?.let { true }, ::yesNo)
            field(UiKey.COMMON_NAME, a?.title, b?.title)
            field(UiKey.TEXT_ENGLISH, a?.titleEn, b?.titleEn)
            field(UiKey.TEXT_SLOVEN_INA, a?.titleSk, b?.titleSk)
            field(UiKey.STUDIES_UNIVERSITY, a?.university, b?.university)
            field(UiKey.STUDIES_DEGREE, a?.degree, b?.degree)
            field(UiKey.STUDIES_YEARS, a?.periodLabel, b?.periodLabel)
            field(UiKey.STUDIES_ACTIVE_STATUS, a?.active, b?.active, ::yesNo)
            field(UiKey.TEXT_CURRENT_SEMESTER, a?.currentSemester, b?.currentSemester)
            field(UiKey.BACKUP_STUDY_EARNED_CREDITS, a?.earnedCredits, b?.earnedCredits)
            field(UiKey.BACKUP_STUDY_REQUIRED_CREDITS, a?.requiredCredits, b?.requiredCredits)
            field(UiKey.BACKUP_STUDY_FEES_LOADED, a?.feesLoaded, b?.feesLoaded, ::loaded)
            field(UiKey.BACKUP_STUDY_SCHOLARSHIPS_LOADED, a?.scholarshipsLoaded, b?.scholarshipsLoaded, ::loaded)
            field(UiKey.BACKUP_STUDY_UPDATED, a?.updatedAtEpochMillis, b?.updatedAtEpochMillis, ::updated)
            linkage(UiKey.BACKUP_STUDY_PERIOD_LINK, a?.periodId, b?.periodId)
            // All changed list records get their own detailed group below.
            field(UiKey.TEXT_SUBJECTS, a?.courses?.size, b?.courses?.size)
            field(UiKey.STUDIES_PAYMENTS, a?.fees?.size, b?.fees?.size)
            field(UiKey.STUDIES_SCHOLARSHIPS, a?.scholarships?.size, b?.scholarships?.size)
            sourcedField(UiKey.BACKUP_STUDY_COURSE_ORDER, a?.courses?.map { it.id }, b?.courses?.map { it.id },
                { ids -> orderedNames(ids, a?.courses.orEmpty(), BackupCourse::id) { "${it.code} · ${it.title}" } },
                { ids -> orderedNames(ids, b?.courses.orEmpty(), BackupCourse::id) { "${it.code} · ${it.title}" } })
            sourcedField(UiKey.BACKUP_STUDY_FEE_ORDER, a?.fees?.map { it.id }, b?.fees?.map { it.id },
                { ids -> orderedNames(ids, a?.fees.orEmpty(), BackupStudentFee::id, BackupStudentFee::title) },
                { ids -> orderedNames(ids, b?.fees.orEmpty(), BackupStudentFee::id, BackupStudentFee::title) })
            sourcedField(UiKey.BACKUP_STUDY_SCHOLARSHIP_ORDER, a?.scholarships?.map { it.id }, b?.scholarships?.map { it.id },
                { ids -> orderedNames(ids, a?.scholarships.orEmpty(), BackupScholarshipPayment::id, BackupScholarshipPayment::title) },
                { ids -> orderedNames(ids, b?.scholarships.orEmpty(), BackupScholarshipPayment::id, BackupScholarshipPayment::title) })
        }
        changedRecords(a?.courses.orEmpty(), b?.courses.orEmpty(), BackupCourse::id).forEach { (old, new) ->
            val course = (new ?: old)!!
            group("${(b ?: a)!!.title} · ${course.code} · ${course.title}", true) { courseFields(old, new) }
        }
        changedRecords(a?.fees.orEmpty(), b?.fees.orEmpty(), BackupStudentFee::id).forEach { (old, new) ->
            group("${(b ?: a)!!.title} · ${t(UiKey.STUDIES_PAYMENTS)} · ${(new ?: old)!!.title}", true) {
                field(UiKey.BACKUP_STUDY_SAVED_RECORD, old?.let { true }, new?.let { true }, ::yesNo)
                field(UiKey.COMMON_NAME, old?.title, new?.title)
                field(UiKey.TEXT_AMOUNT, old?.let { money(it.amountMinor, it.currency) }, new?.let { money(it.amountMinor, it.currency) })
                field(UiKey.STUDIES_PAID, old?.paidMinor?.let { money(it, old.currency) }, new?.paidMinor?.let { money(it, new.currency) })
                field(UiKey.STUDIES_STATUS, old?.status, new?.status, ::feeStatus)
                field(UiKey.STUDIES_DUE, old?.dueEpochDay, new?.dueEpochDay, ::day)
                field(UiKey.STUDIES_ISSUED, old?.issuedEpochDay, new?.issuedEpochDay, ::day)
                linkage(UiKey.STUDIES_UNIVERSITY_RECORD, old?.recordUrl, new?.recordUrl)
            }
        }
        changedRecords(a?.scholarships.orEmpty(), b?.scholarships.orEmpty(), BackupScholarshipPayment::id).forEach { (old, new) ->
            group("${(b ?: a)!!.title} · ${t(UiKey.STUDIES_SCHOLARSHIPS)} · ${(new ?: old)!!.title}", true) {
                field(UiKey.BACKUP_STUDY_SAVED_RECORD, old?.let { true }, new?.let { true }, ::yesNo)
                field(UiKey.COMMON_NAME, old?.title, new?.title)
                field(UiKey.TEXT_AMOUNT, old?.let { money(it.amountMinor, it.currency) }, new?.let { money(it.amountMinor, it.currency) })
                field(UiKey.BACKUP_STUDY_PAID_ON, old?.paidEpochDay, new?.paidEpochDay, ::day)
                field(UiKey.STUDIES_YEARS, old?.periodLabel, new?.periodLabel)
            }
        }
    }
    // List order and opaque linkage can differ even when all visible fields are equal.
    if (isEmpty()) add(StudyChangeGroup(t(UiKey.STUDIES_TITLE), StudyFields().apply { fallback() }.changes))
}

private fun StudyFields.courseFields(a: BackupCourse?, b: BackupCourse?) {
    field(UiKey.BACKUP_STUDY_SAVED_RECORD, a?.let { true }, b?.let { true }, ::yesNo)
    field(UiKey.BACKUP_STUDY_COURSE_CODE, a?.code, b?.code)
    field(UiKey.COMMON_NAME, a?.title, b?.title)
    field(UiKey.TEXT_ENGLISH, a?.titleEn, b?.titleEn)
    field(UiKey.TEXT_SLOVEN_INA, a?.titleSk, b?.titleSk)
    field(UiKey.TEXT_CREDITS_2A6B24AD, a?.credits, b?.credits)
    field(UiKey.TEXT_SEMESTER_A83EF1C1, a?.semester, b?.semester)
    field(UiKey.STUDIES_STATUS, a?.status, b?.status) { status -> t(when (status) {
        "fulfilled" -> UiKey.TEXT_WORK_COMPLETED
        "enrolled" -> UiKey.TEXT_ACTIVE_SUBJECT
        "planned" -> UiKey.TEXT_SCHEDULED
        "not_enrolled" -> UiKey.TEXT_NOT_RECORDED
        else -> UiKey.TEXT_NOT_NEEDED
    }) }
    field(UiKey.WORK_TYPE, a?.requirementType, b?.requirementType) { type -> t(when (type) {
        "COMPULSORY" -> UiKey.BACKUP_STUDY_COMPULSORY
        "SEMI_COMPULSORY" -> UiKey.BACKUP_STUDY_SEMI_COMPULSORY
        else -> UiKey.BACKUP_STUDY_ELECTIVE
    }) }
    field(UiKey.TEXT_ASSESSMENT_7808EEEB, a?.gradingType, b?.gradingType) { type -> t(when (type) {
        "EXAM_LETTER" -> UiKey.TEXT_EXAMINATION_ASSESSMENT
        "CONTINUOUS_LETTER" -> UiKey.TEXT_ASSESSMENT_DURING_STUDY
        else -> UiKey.TEXT_PASS_FAIL
    }) }
    field(UiKey.TEXT_RESULT_KNOWN, a?.passFailResult, b?.passFailResult) { t(if (it == "PASSED") UiKey.TEXT_PASSED else UiKey.TEXT_FAILED) }
    field(UiKey.TEXT_FINAL_GRADE, a?.officialGrade, b?.officialGrade)
    field(UiKey.BACKUP_STUDY_FULFILLED_ON, a?.fulfilledOnEpochDay, b?.fulfilledOnEpochDay, ::day)
    field(UiKey.TEXT_COURSE_ICONS, a?.iconKey, b?.iconKey) { key -> CourseIcons.find(key)?.let { t(it.label) } ?: missing() }
    field(UiKey.SUBJECT_COLOR, a?.let { it.colorKey.orEmpty() }, b?.let { it.colorKey.orEmpty() }) { key ->
        CourseColor.fromKey(key)?.let { tf(UiKey.SUBJECT_COLOR_OPTION, it.ordinal + 1) } ?: t(UiKey.SUBJECT_COLOR_AUTOMATIC)
    }
    linkage(UiKey.TEXT_COURSE_SYLLABUS, a?.syllabusUrl, b?.syllabusUrl)
}

private fun <T> changedRecords(phone: List<T>, file: List<T>, id: (T) -> String): List<Pair<T?, T?>> {
    val a = phone.associateBy(id)
    val b = file.associateBy(id)
    return (a.keys + b.keys).mapNotNull { key -> if (a[key] == b[key]) null else a[key] to b[key] }
}

private fun studyName(source: BackupStudentHub?, id: String): String =
    source?.studies.orEmpty().firstOrNull { it.id == id }?.let {
        listOfNotNull(it.title, it.degree, it.periodLabel).joinToString(" · ")
    } ?: missing()
private fun <T> orderedNames(ids: List<String>, records: List<T>, key: (T) -> String, name: (T) -> String): String {
    val byId = records.associateBy(key)
    return ids.mapNotNull { byId[it]?.let(name) }.joinToString(" · ").ifBlank { missing() }
}
private fun missing() = t(UiKey.TEXT_UNSPECIFIED)
private fun yesNo(value: Boolean) = t(if (value) UiKey.BACKUP_STUDY_YES else UiKey.BACKUP_STUDY_NO)
private fun loaded(value: Boolean) = t(if (value) UiKey.BACKUP_STUDY_LOADED else UiKey.BACKUP_STUDY_NOT_LOADED)
private fun updated(value: Long): String = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
    .withLocale(TaktI18n.locale).format(Instant.ofEpochMilli(value).atZone(ZoneId.systemDefault()))
private fun day(value: Long): String = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    .withLocale(TaktI18n.locale).format(LocalDate.ofEpochDay(value))
private fun money(value: Long, currency: String): String = NumberFormat.getCurrencyInstance(TaktI18n.locale).apply {
    this.currency = Currency.getInstance(currency)
}.format(BigDecimal.valueOf(value, 2))
private fun feeStatus(status: String): String = t(when (status) {
    "PAID" -> UiKey.STUDIES_PAID
    "UNPAID" -> UiKey.STUDIES_UNPAID
    "PARTIAL" -> UiKey.STUDIES_PARTIAL
    "CANCELLED" -> UiKey.STUDIES_CANCELLED
    else -> UiKey.STUDIES_UNKNOWN_BALANCE
})
