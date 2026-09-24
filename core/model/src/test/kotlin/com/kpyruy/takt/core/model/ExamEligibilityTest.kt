package com.kpyruy.takt.core.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExamEligibilityTest {
    @Test
    fun eligibilityCountsRequiredTasksAndAssessmentsOnly() {
        val tasks = listOf(
            StudyTask(
                id = "required-done",
                courseId = "course",
                title = "Protocol",
                description = null,
                dueDate = LocalDate.of(2026, 10, 1),
                completed = true,
                requiredForExam = true,
            ),
            StudyTask(
                id = "required-open",
                courseId = "course",
                title = "Presentation",
                description = null,
                dueDate = null,
                completed = false,
                requiredForExam = true,
            ),
            StudyTask(
                id = "optional",
                courseId = "course",
                title = "Extra practice",
                description = null,
                dueDate = null,
                completed = false,
                requiredForExam = false,
            ),
        )
        val assessments = listOf(
            GradeItem(
                id = "lab",
                courseId = "course",
                title = "Lab",
                type = GradeItemType.LAB,
                earnedPoints = 10.0,
                maxPoints = 10.0,
                completed = true,
                requiredForExam = true,
            )
        )

        val result = ExamEligibilityCalculator.calculate(tasks, assessments)

        assertEquals(3, result.requiredCount)
        assertEquals(2, result.completedCount)
        assertFalse(result.eligible)
    }

    @Test
    fun zeroRequirementsMeansEligible() {
        val result = ExamEligibilityCalculator.calculate(emptyList(), emptyList())

        assertTrue(result.eligible)
        assertEquals(0, result.requiredCount)
    }
    @Test
    fun allRequiredWorkCompleteMeansEligible() {
        val tasks = listOf(
            StudyTask(
                id = "task-1",
                courseId = "course",
                title = "Protocol",
                description = null,
                dueDate = null,
                completed = true,
                requiredForExam = true,
            ),
            StudyTask(
                id = "task-2",
                courseId = "course",
                title = "Presentation",
                description = null,
                dueDate = null,
                completed = true,
                requiredForExam = true,
            ),
        )
        val assessments = listOf(
            GradeItem(
                id = "lab",
                courseId = "course",
                title = "Lab",
                type = GradeItemType.LAB,
                earnedPoints = 10.0,
                maxPoints = 10.0,
                completed = true,
                requiredForExam = true,
            )
        )

        val result = ExamEligibilityCalculator.calculate(tasks, assessments)

        assertTrue(result.eligible)
        assertEquals(3, result.requiredCount)
        assertEquals(3, result.completedCount)
    }

    @Test
    fun completedAssessmentNeedsItsMinimumPointsForAdmission() {
        val assessment = GradeItem(
            id = "test",
            courseId = "course",
            title = "Entry test",
            type = GradeItemType.TEST,
            earnedPoints = 9.0,
            maxPoints = 20.0,
            completed = true,
            requiredForExam = true,
            minimumPointsForExam = 10.0,
        )

        val below = ExamEligibilityCalculator.calculate(emptyList(), listOf(assessment))
        assertEquals(1, below.requiredCount)
        assertEquals(0, below.completedCount)
        assertFalse(below.eligible)

        val atMinimum = ExamEligibilityCalculator.calculate(emptyList(), listOf(assessment.copy(earnedPoints = 10.0)))
        assertEquals(1, atMinimum.completedCount)
        assertTrue(atMinimum.eligible)
    }

    @Test
    fun completedTaskNeedsItsMinimumPointsForAdmission() {
        val task = StudyTask(
            id = "homework",
            courseId = "course",
            title = "Homework",
            description = null,
            dueDate = null,
            completed = true,
            requiredForExam = true,
            earnedPoints = 4.0,
            maxPoints = 10.0,
            minimumPointsForExam = 5.0,
        )

        assertFalse(ExamEligibilityCalculator.calculate(listOf(task), emptyList()).eligible)
        assertTrue(ExamEligibilityCalculator.calculate(listOf(task.copy(earnedPoints = 5.0)), emptyList()).eligible)
    }

}
