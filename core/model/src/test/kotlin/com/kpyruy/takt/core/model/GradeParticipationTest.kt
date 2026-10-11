package com.kpyruy.takt.core.model

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class GradeParticipationTest {
    private val today = LocalDate.of(2026, 10, 11)
    private fun test(completed: Boolean = true, points: Double = 15.0, minimum: Double? = 10.0) =
        GradeItem("gate", "course", "Допусковий тест", GradeItemType.TEST, points, 20.0,
            dueDate = today.minusDays(1), completed = completed, requiredForExam = true,
            minimumPointsForExam = minimum, contributesToGrade = false)

    @Test fun admissionOnlyTestDoesNotAddEarnedOrMaximumPoints() {
        val gate = test()
        val exam = GradeItem("exam", "course", "Екзамен", GradeItemType.EXAM, 0.0, 100.0, completed = false)
        val summary = GradeSummary.calculate(listOf(gate, exam), GradeScale.default())
        val projection = GradeProjection.calculate(listOf(gate, exam), GradeScale.default())
        assertEquals(0.0, summary.earnedPoints, .0)
        assertEquals(100.0, summary.maxPoints, .0)
        assertEquals(0.0, projection.securedPoints, .0)
        assertEquals(100.0, projection.totalPoints, .0)
        assertEquals(92.0, projection.examPointsNeeded[GradeLetter.A]!!, .0)
        assertTrue(ExamEligibilityCalculator.calculate(emptyList(), listOf(gate, exam)).eligible)
    }
    @Test fun excludedTestStillBlocksAdmissionBelowItsMinimum() {
        val gate = test(points = 5.0)
        assertFalse(ExamEligibilityCalculator.calculate(emptyList(), listOf(gate)).eligible)
        assertTrue(CourseWork.scoredItems(emptyList(), listOf(gate)).isEmpty())
    }
    @Test fun excludedAssessmentSuppressesMatchingLegacyScoredTaskToo() {
        val gate = test()
        val legacy = StudyTask("legacy", "course", gate.title, null, gate.dueDate, true,
            earnedPoints = 15.0, maxPoints = 20.0)
        assertTrue(CourseWork.scoredItems(listOf(legacy), listOf(gate)).isEmpty())
    }
    @Test fun waitingGradeIsSubmittedButDoesNotFakeKnownMinimumResult() {
        val waiting = test(completed = false, points = 0.0).copy(awaitingResult = true)
        assertTrue(waiting.isSubmitted)
        assertFalse(waiting.meetsAdmissionRequirement)
        assertTrue(CourseWork.allSubmitted(emptyList(), listOf(waiting)))
        assertTrue(HomeWorkPlanner.overdue(emptyList(), listOf(waiting), today, HomeWorkFilter()).isEmpty())
        assertTrue(HomeWorkPlanner.visible(emptyList(), listOf(waiting), today, HomeWorkFilter(period = HomeWorkPeriod.ALL)).isEmpty())
        assertEquals(1, HomeWorkPlanner.visible(emptyList(), listOf(waiting), today,
            HomeWorkFilter(period = HomeWorkPeriod.ALL, includeCompleted = true)).size)
    }
    @Test fun writtenWorkWithoutPointMinimumMeetsItsSubmissionRequirement() {
        assertTrue(test(completed = false, points = 0.0, minimum = null).copy(awaitingResult = true).meetsAdmissionRequirement)
    }
    @Test fun waitingContributingResultRemainsUnknownPotentialNotSecuredPoints() {
        val waiting = test(completed = false, points = 0.0).copy(awaitingResult = true, contributesToGrade = true)
        val projection = GradeProjection.calculate(listOf(waiting), GradeScale.default())
        assertEquals(0.0, projection.securedPoints, .0)
        assertEquals(20.0, projection.maximumPossiblePoints, .0)
        assertFalse(waiting.isOverdue(today))
        assertTrue(waiting.copy(awaitingResult = false).isOverdue(today))
    }
}
