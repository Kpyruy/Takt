package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class StudentHubBackupTest {
    private val study = StudentStudy("101", "Programme", active = true, earnedCredits = 86,
        requiredCredits = 185, feesLoaded = true, scholarshipsLoaded = true,
        fees = listOf(StudentFee("one", "101", "ISIC", 2200, paidMinor = 0, status = StudentFeeStatus.UNPAID,
            dueDate = LocalDate.of(2026, 10, 31), recordUrl = "https://is.stuba.sk/auth/student/financovani.pl?studium=101;detail=1")),
        scholarships = listOf(ScholarshipPayment("payment", "101", "Scholarship", 20000, paidOn = LocalDate.of(2026, 10, 12))),
        courses = listOf(Course("uis:101:10", "PHYS", "Physics", 5, 1, CourseStatus.FULFILLED)), periodId = "741")
    private val snapshot = StudentHubSnapshot(StudentIdentity("15", "Alex Novak", "STU"), listOf(study), "101", "101", "Alex", true)
    @Test fun roundTripKeepsStudyScopeMoneyDatesAndDisplayPreferences() {
        val backup = BackupPayload(studentHub = snapshot.toBackupStudentHub())
        assertEquals(snapshot, BackupPayloadCodec.decode(BackupPayloadCodec.encode(backup)).studentHub!!.toModel())
    }
    @Test fun duplicateStudiesAndCrossStudyFeesAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { snapshot.copy(studies = listOf(study, study)).toBackupStudentHub().validated() }
        assertThrows(IllegalArgumentException::class.java) { snapshot.copy(studies = listOf(study.copy(fees = study.fees.map { it.copy(studyId = "other") }))).toBackupStudentHub().validated() }
    }
    @Test fun unsafeLinksAndNegativeMoneyAreRejectedBeforeRestore() {
        for (bad in listOf(study.fees[0].copy(amountMinor = -1), study.fees[0].copy(recordUrl = "https://example.org/record"))) {
            assertThrows(IllegalArgumentException::class.java) { snapshot.copy(studies = listOf(study.copy(fees = listOf(bad)))).toBackupStudentHub().validated() }
        }
    }
    @Test fun selectiveImportNeverMergesStudentIdentityOrCourseIdsByCourseCode() {
        val local = BackupPayload(studentHub = snapshot.toBackupStudentHub())
        val incoming = BackupPayload(studentHub = snapshot.copy(displayName = "Other").toBackupStudentHub())
        assertEquals(local.studentHub, BackupMerger.merge(local, incoming, setOf(BackupSection.PROGRESS)).studentHub)
        assertEquals(incoming.studentHub, BackupMerger.merge(local, incoming, setOf(BackupSection.STUDIES)).studentHub)
        assertEquals(setOf(BackupSection.STUDIES), BackupMerger.changedSections(local, incoming))
    }
}
