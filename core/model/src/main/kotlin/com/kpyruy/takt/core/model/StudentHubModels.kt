package com.kpyruy.takt.core.model

import java.time.LocalDate

/** University-provided identity; display preferences never mutate this record. */
data class StudentIdentity(val personId: String? = null, val name: String,
    val university: String? = null, val studentNumber: String? = null, val faculty: String? = null)
enum class StudentFeeStatus { UNPAID, PARTIAL, PAID, CANCELLED, UNKNOWN }
data class StudentFee(val id: String, val studyId: String, val title: String, val amountMinor: Long,
    val currency: String = "EUR", val paidMinor: Long? = null, val status: StudentFeeStatus = StudentFeeStatus.UNKNOWN,
    val dueDate: LocalDate? = null, val issuedDate: LocalDate? = null, val recordUrl: String? = null)
data class ScholarshipPayment(val id: String, val studyId: String, val title: String, val amountMinor: Long,
    val currency: String = "EUR", val paidOn: LocalDate? = null, val periodLabel: String? = null)
data class StudentStudy(val id: String, val title: String, val university: String? = null,
    val degree: String? = null, val periodLabel: String? = null, val active: Boolean,
    val currentSemester: Int? = null, val earnedCredits: Int? = null, val requiredCredits: Int? = null,
    val courses: List<Course> = emptyList(), val fees: List<StudentFee> = emptyList(),
    val scholarships: List<ScholarshipPayment> = emptyList(), val feesLoaded: Boolean = false,
    val scholarshipsLoaded: Boolean = false, val updatedAtEpochMillis: Long? = null, val periodId: String? = null)
data class StudentHubSnapshot(val identity: StudentIdentity? = null, val studies: List<StudentStudy> = emptyList(),
    val currentStudyId: String? = null, val viewedStudyId: String? = null,
    val displayName: String = "", val showPreviousStudies: Boolean = false, val ownerKey: String? = null)
enum class StudentHubProblem { SIGN_IN_REQUIRED, SECOND_FACTOR_REQUIRED, UNAVAILABLE, UNRECOGNIZED_RESPONSE, SAVE_FAILED }
data class StudentHubState(val snapshot: StudentHubSnapshot? = null, val loading: Boolean = false,
    val problem: StudentHubProblem? = null)

fun StudentStudy.outstandingFees(): Map<String, Long>? {
    if (!feesLoaded) return null
    val totals = linkedMapOf<String, Long>()
    for (fee in fees) {
        val outstanding = when {
            fee.status in setOf(StudentFeeStatus.PAID, StudentFeeStatus.CANCELLED) -> 0L
            fee.paidMinor != null -> (fee.amountMinor - fee.paidMinor).coerceAtLeast(0L)
            fee.status == StudentFeeStatus.UNPAID -> fee.amountMinor
            else -> return null
        }
        totals[fee.currency] = Math.addExact(totals[fee.currency] ?: 0L, outstanding)
    }
    return totals
}
fun StudentHubSnapshot.viewedStudy(): StudentStudy? = studies.firstOrNull { it.id == viewedStudyId }
    ?: studies.firstOrNull { it.id == currentStudyId } ?: studies.firstOrNull()
fun StudentHubSnapshot.withViewedStudy(id: String): StudentHubSnapshot {
    require(studies.any { it.id == id })
    return copy(viewedStudyId = id)
}
fun StudentHubSnapshot.withPreviousStudies(show: Boolean): StudentHubSnapshot = copy(
    showPreviousStudies = show,
    viewedStudyId = if (!show && viewedStudy()?.id != currentStudyId) currentStudyId else viewedStudyId,
)
fun StudentHubSnapshot.visibleStudies(): List<StudentStudy> = studies.filter { showPreviousStudies || it.id == currentStudyId }
