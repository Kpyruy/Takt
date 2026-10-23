package com.kpyruy.takt.core.model

import org.junit.Assert.*
import org.junit.Test

class StudentHubModelsTest {
    private fun study(fees: List<StudentFee> = emptyList(), loaded: Boolean = true) = StudentStudy(
        "101", "Programme", active = true, fees = fees, feesLoaded = loaded)
    @Test fun outstandingUsesMinorUnitsAndKeepsCurrenciesSeparate() {
        val data = study(listOf(
            StudentFee("one", "101", "ISIC", 2200, "EUR", 500, StudentFeeStatus.PARTIAL),
            StudentFee("two", "101", "Fee", 125, "EUR", status = StudentFeeStatus.UNPAID),
            StudentFee("three", "101", "Other", 1000, "CZK", status = StudentFeeStatus.UNPAID)))
        assertEquals(mapOf("EUR" to 1825L, "CZK" to 1000L), data.outstandingFees())
    }
    @Test fun unavailableAndUnrecognizedPaymentsAreNotZeroDebt() {
        assertNull(study(loaded = false).outstandingFees())
        assertNull(study(listOf(StudentFee("unknown", "101", "Unknown", 2200))).outstandingFees())
        assertEquals(emptyMap<String, Long>(), study().outstandingFees())
    }
    @Test fun paidCancelledAndOverpaidFeesDoNotBecomeNegativeDebt() {
        val data = study(listOf(StudentFee("paid", "101", "ISIC", 2200, status = StudentFeeStatus.PAID),
            StudentFee("cancelled", "101", "Cancelled", 5000, status = StudentFeeStatus.CANCELLED),
            StudentFee("overpaid", "101", "Other", 100, paidMinor = 200)))
        assertEquals(mapOf("EUR" to 0L), data.outstandingFees())
    }
    @Test fun viewingPreviousStudyDoesNotChangeCurrentStudyOrMergeFees() {
        val current = study(listOf(StudentFee("fee", "101", "ISIC", 2200, status = StudentFeeStatus.UNPAID)))
        val previous = StudentStudy("202", "Old", active = false, feesLoaded = true)
        val source = StudentHubSnapshot(studies = listOf(current, previous), currentStudyId = "101", viewedStudyId = "101")
        val viewed = source.withViewedStudy("202")
        assertEquals("101", viewed.currentStudyId)
        assertEquals(previous, viewed.viewedStudy())
        assertEquals(emptyMap<String, Long>(), viewed.viewedStudy()!!.outstandingFees())
        assertEquals(mapOf("EUR" to 2200L), source.viewedStudy()!!.outstandingFees())
    }
    @Test fun hidingOtherStudiesReturnsToPrimaryEvenWhenBothAreEnrolled() {
        val source = StudentHubSnapshot(studies = listOf(study(), StudentStudy("202", "Other", active = true)),
            currentStudyId = "101", viewedStudyId = "202", showPreviousStudies = true)
        val hidden = source.withPreviousStudies(false)
        assertEquals("101", hidden.viewedStudyId)
        assertEquals(listOf("101"), hidden.visibleStudies().map { it.id })
    }
    @Test fun hidingPreviousStudiesReturnsViewingContextToCurrentStudy() {
        val source = StudentHubSnapshot(studies = listOf(study(), StudentStudy("202", "Old", active = false)),
            currentStudyId = "101", viewedStudyId = "202", showPreviousStudies = true)
        assertEquals("101", source.withPreviousStudies(false).viewedStudyId)
        assertEquals(listOf("101"), source.withPreviousStudies(false).visibleStudies().map { it.id })
    }
}
