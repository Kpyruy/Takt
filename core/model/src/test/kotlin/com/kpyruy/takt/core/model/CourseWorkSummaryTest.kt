package com.kpyruy.takt.core.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseWorkSummaryTest {
    @Test fun legacyScoredTaskAndMatchingAssessmentCountOnce() {
        val date = LocalDate.of(2026, 9, 28)
        val task = StudyTask("task", "course", "Lab report", null, date, true,
            earnedPoints = 7.0, maxPoints = 10.0)
        val assessment = GradeItem("grade", "course", "Lab report", GradeItemType.LAB,
            7.0, 10.0, dueDate = date)

        val items = CourseWork.scoredItems(listOf(task), listOf(assessment))

        assertEquals(listOf("grade"), items.map { it.id })
        assertEquals(7.0, GradeSummary.calculate(items, GradeScale.default()).earnedPoints, 0.0)
    }

    @Test fun differentAssessmentDoesNotHideLegacyScoredTask() {
        val task = StudyTask("task", "course", "Lab report", null, null, true,
            earnedPoints = 7.0, maxPoints = 10.0)
        val assessment = GradeItem("grade", "course", "Lab report", GradeItemType.LAB, 3.0, 5.0)

        assertEquals(2, CourseWork.scoredItems(listOf(task), listOf(assessment)).size)
    }

    @Test fun unfinishedTestPreventsAllWorkSubmitted() {
        val task = StudyTask("task", "course", "Homework", null, null, true)
        val test = GradeItem("test", "course", "Weekly test", GradeItemType.TEST,
            0.0, 10.0, completed = false)

        assertFalse(CourseWork.allSubmitted(listOf(task), listOf(test)))
    }

    @Test fun matchingRequiredLegacyTaskAndAssessmentCountOnceForAdmission() {
        val date = LocalDate.of(2026, 9, 28)
        val task = StudyTask("task", "course", "Lab report", null, date, true,
            requiredForExam = true, earnedPoints = 7.0, maxPoints = 10.0)
        val assessment = GradeItem("grade", "course", "Lab report", GradeItemType.LAB,
            7.0, 10.0, dueDate = date, completed = true, requiredForExam = true)

        val admission = ExamEligibilityCalculator.calculate(listOf(task), listOf(assessment))

        assertEquals(1, admission.requiredCount)
        assertTrue(admission.eligible)
    }

    @Test fun completedAssessmentSupersedesMatchingLegacyTaskStatus() {
        val task = StudyTask("task", "course", "Lab report", null, null, false,
            maxPoints = 10.0)
        val assessment = GradeItem("grade", "course", "Lab report", GradeItemType.LAB,
            7.0, 10.0, completed = true)

        assertTrue(CourseWork.allSubmitted(listOf(task), listOf(assessment)))
    }

    @Test fun oneAssessmentSupersedesOnlyOneMatchingLegacyTask() {
        val tasks = listOf("a", "b").map { id ->
            StudyTask(id, "course", "Report", null, null, true,
                earnedPoints = 4.0, maxPoints = 10.0)
        }
        val assessment = GradeItem("grade", "course", "Report", GradeItemType.LAB, 4.0, 10.0)

        assertEquals(2, CourseWork.scoredItems(tasks, listOf(assessment)).size)
    }
}
