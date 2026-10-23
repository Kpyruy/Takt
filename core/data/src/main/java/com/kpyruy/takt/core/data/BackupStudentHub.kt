package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URI
import java.time.LocalDate

@Serializable
data class BackupStudentIdentity(val personId: String? = null, val name: String,
    val university: String? = null, val studentNumber: String? = null, val faculty: String? = null) {
    fun toModel() = StudentIdentity(personId, name, university, studentNumber, faculty)
}
@Serializable
data class BackupStudentFee(val id: String, val studyId: String, val title: String, val amountMinor: Long,
    val currency: String, val paidMinor: Long? = null, val status: String,
    val dueEpochDay: Long? = null, val issuedEpochDay: Long? = null, val recordUrl: String? = null) {
    fun toModel() = StudentFee(id, studyId, title, amountMinor, currency, paidMinor, StudentFeeStatus.valueOf(status),
        dueEpochDay?.let(LocalDate::ofEpochDay), issuedEpochDay?.let(LocalDate::ofEpochDay), recordUrl)
}
@Serializable
data class BackupScholarshipPayment(val id: String, val studyId: String, val title: String,
    val amountMinor: Long, val currency: String, val paidEpochDay: Long? = null, val periodLabel: String? = null) {
    fun toModel() = ScholarshipPayment(id, studyId, title, amountMinor, currency, paidEpochDay?.let(LocalDate::ofEpochDay), periodLabel)
}
@Serializable
data class BackupStudentStudy(val id: String, val title: String, val active: Boolean,
    val university: String? = null, val degree: String? = null, val periodLabel: String? = null,
    val currentSemester: Int? = null, val earnedCredits: Int? = null, val requiredCredits: Int? = null,
    val courses: List<BackupCourse> = emptyList(), val fees: List<BackupStudentFee> = emptyList(),
    val scholarships: List<BackupScholarshipPayment> = emptyList(), val feesLoaded: Boolean = false,
    val scholarshipsLoaded: Boolean = false, val updatedAtEpochMillis: Long? = null, val periodId: String? = null,
    val titleEn: String? = null, val titleSk: String? = null) {
    fun toModel() = StudentStudy(id,title,university,degree,periodLabel,active,currentSemester,earnedCredits,requiredCredits,
        courses.map { Course(it.id,it.code,it.title,it.credits,it.semester,CourseStatus.fromStorage(it.status),
            CourseRequirementType.valueOf(it.requirementType),it.syllabusUrl,CourseGradingType.valueOf(it.gradingType),
            it.passFailResult?.let(PassFailResult::valueOf),it.iconKey,it.titleEn,it.titleSk,it.officialGrade?.let(GradeLetter::valueOf),
            it.fulfilledOnEpochDay?.let(LocalDate::ofEpochDay),it.colorKey) }, fees.map { it.toModel() },scholarships.map { it.toModel() },feesLoaded,
        scholarshipsLoaded,updatedAtEpochMillis,periodId,titleEn,titleSk)
}
@Serializable
data class BackupStudentHub(val identity: BackupStudentIdentity? = null, val studies: List<BackupStudentStudy> = emptyList(),
    val currentStudyId: String? = null, val viewedStudyId: String? = null, val displayName: String = "",
    val showPreviousStudies: Boolean = false, val ownerKey: String? = null) {
    fun toModel() = StudentHubSnapshot(identity?.toModel(),studies.map { it.toModel() },currentStudyId,viewedStudyId,displayName,showPreviousStudies,ownerKey)
    fun validated(): BackupStudentHub {
        require(studies.size <= 32 && studies.map { it.id }.distinct().size == studies.size)
        val ids = studies.map { it.id }.toSet()
        require(currentStudyId == null || currentStudyId in ids)
        require(viewedStudyId == null || viewedStudyId in ids)
        require(displayName.length <= 100 && (ownerKey == null || ownerKey.length <= 128))
        identity?.let { require(it.name.length <= 1000 && listOf(it.personId,it.university,it.studentNumber,it.faculty).all { s -> s == null || s.length <= 2000 }) }
        fun amount(value: Long) { require(value in 0..100_000_000_000L) }
        fun date(value: Long?) { if(value!=null) require(value in LocalDate.of(1900,1,1).toEpochDay()..LocalDate.of(2200,12,31).toEpochDay()) }
        studies.forEach { study ->
            require(study.id.matches(Regex("[A-Za-z0-9:_-]{1,128}")) && study.title.isNotBlank() && study.title.length <= 2000)
            require(listOf(study.university,study.degree,study.periodLabel,study.titleEn,study.titleSk).all { it == null || it.length <= 2000 })
            require(study.courses.size <= 1000 && study.fees.size <= 2000 && study.scholarships.size <= 2000)
            require(study.fees.map { it.id }.distinct().size == study.fees.size)
            require(study.scholarships.map { it.id }.distinct().size == study.scholarships.size)
            require(study.earnedCredits == null || study.earnedCredits in 0..10000)
            require(study.requiredCredits == null || study.requiredCredits in 0..10000)
            require(study.currentSemester == null || study.currentSemester in 1..100)
            require(study.updatedAtEpochMillis == null || study.updatedAtEpochMillis in 0..7_258_118_400_000L)
            require(study.periodId == null || study.periodId.matches(Regex("[1-9][0-9]{0,14}")))
            BackupPayload(courses=study.courses).validated()
            study.courses.filter { it.id.startsWith("uis:") }.forEach { require(it.id.startsWith("uis:${study.id}:")) }
            study.fees.forEach { fee ->
                require(fee.id.isNotBlank() && fee.id.length <= 256 && fee.studyId == study.id && fee.title.isNotBlank() && fee.title.length <= 2000)
                amount(fee.amountMinor);fee.paidMinor?.let(::amount);date(fee.dueEpochDay);date(fee.issuedEpochDay)
                require(fee.currency.matches(Regex("[A-Z]{3}")));java.util.Currency.getInstance(fee.currency)
                StudentFeeStatus.valueOf(fee.status)
                fee.recordUrl?.let { value -> val u=URI(value); require(value.length <= 4000 && u.scheme == "https" && u.host == "is.stuba.sk" && u.userInfo == null && (u.port == -1 || u.port == 443) && u.path == "/auth/student/financovani.pl") }
            }
            study.scholarships.forEach { paid ->
                require(paid.id.isNotBlank() && paid.id.length <= 256 && paid.studyId == study.id && paid.title.isNotBlank() && paid.title.length <= 2000)
                amount(paid.amountMinor);date(paid.paidEpochDay)
                require(paid.currency.matches(Regex("[A-Z]{3}")));java.util.Currency.getInstance(paid.currency)
            }
        }
        require(StudentHubCodec.json.encodeToString(this).toByteArray().size <= 4_000_000)
        return this
    }
}
internal object StudentHubCodec {
    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    fun encode(value: StudentHubSnapshot) = json.encodeToString(value.toBackupStudentHub().validated())
    fun decode(value: String): StudentHubSnapshot { require(value.toByteArray().size <= 4_000_000); return json.decodeFromString<BackupStudentHub>(value).validated().toModel() }
}
fun StudentHubSnapshot.toBackupStudentHub() = BackupStudentHub(identity?.let { BackupStudentIdentity(it.personId,it.name,it.university,it.studentNumber,it.faculty) },
    studies.map { s -> BackupStudentStudy(s.id,s.title,s.active,s.university,s.degree,s.periodLabel,s.currentSemester,s.earnedCredits,s.requiredCredits,
        s.courses.map { c -> BackupCourse(c.id,c.code,c.title,c.credits,c.semester,c.status.storageValue,c.requirementType.name,c.syllabusUrl,
            c.gradingType.name,c.passFailResult?.name,c.iconKey,c.titleEn,c.titleSk,c.officialGrade?.name,c.fulfilledOn?.toEpochDay(),c.colorKey) },
        s.fees.map { BackupStudentFee(it.id,it.studyId,it.title,it.amountMinor,it.currency,it.paidMinor,it.status.name,it.dueDate?.toEpochDay(),it.issuedDate?.toEpochDay(),it.recordUrl) },
        s.scholarships.map { BackupScholarshipPayment(it.id,it.studyId,it.title,it.amountMinor,it.currency,it.paidOn?.toEpochDay(),it.periodLabel) },
        s.feesLoaded,s.scholarshipsLoaded,s.updatedAtEpochMillis,s.periodId,s.titleEn,s.titleSk) },currentStudyId,viewedStudyId,displayName,showPreviousStudies,ownerKey)
