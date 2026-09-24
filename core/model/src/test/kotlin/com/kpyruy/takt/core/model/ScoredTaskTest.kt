package com.kpyruy.takt.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoredTaskTest {
    @Test fun scoredTaskAddsItsPointsToSemesterTotalAfterCompletion() {
        val task = StudyTask("task", "course", "Lab", null, null, true,
            requiredForExam = true, earnedPoints = 4.0, maxPoints = 10.0, minimumPointsForExam = 5.0)
        val item = task.asScoredGradeItem()!!

        assertEquals(4.0, GradeSummary.calculate(listOf(item), GradeScale.default()).earnedPoints, 0.0)
        assertFalse(task.meetsAdmissionRequirement)
        assertTrue(item.completed)

        val pending = task.copy(completed = false).asScoredGradeItem()!!
        assertEquals(0.0, pending.earnedPoints, 0.0)
        assertFalse(pending.completed)
        assertEquals(10.0, GradeProjection.calculate(listOf(pending), GradeScale.default()).maximumPossiblePoints, 0.0)
    }
}
