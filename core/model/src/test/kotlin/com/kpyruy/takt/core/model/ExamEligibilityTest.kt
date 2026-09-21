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
}
