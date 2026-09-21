package com.kpyruy.takt.core.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GradeProjectionTest {
    private val scale = GradeScale.default()

    private fun item(
        id: String,
        earned: Double,
        max: Double,
        completed: Boolean,
        type: GradeItemType = GradeItemType.HOMEWORK,
        requiredForExam: Boolean = false,
    ) = GradeItem(
        id = id,
        courseId = "course",
        title = id,
        type = type,
        earnedPoints = earned,
        maxPoints = max,
        recordedAtEpochMillis = 0L,
        dueDate = LocalDate.of(2026, 10, 1),
        completed = completed,
        requiredForExam = requiredForExam,
    )

    @Test
    fun completedZeroPointTasksReduceMaximumPossibleGrade() {
        val items = listOf(
            item("task-1", 0.0, 10.0, completed = true),
            item("task-2", 0.0, 10.0, completed = true),
            item("task-3", 0.0, 10.0, completed = true),
            item("exam", 0.0, 70.0, completed = false, type = GradeItemType.EXAM),
        )

        val projection = GradeProjection.calculate(items, scale)

        assertEquals(70.0, projection.maximumPossiblePoints, 0.001)
        assertEquals(GradeLetter.FX, projection.minimumPossibleLetter)
        assertEquals(GradeLetter.D, projection.maximumPossibleLetter)
        assertNull(projection.examPointsNeeded[GradeLetter.A])
        assertNull(projection.examPointsNeeded[GradeLetter.B])
        assertNull(projection.examPointsNeeded[GradeLetter.C])
        assertEquals(65.0, projection.examPointsNeeded[GradeLetter.D]!!, 0.001)
    }

    @Test
    fun securedCoursePointsLowerRequiredExamScore() {
        val items = listOf(
            item("coursework", 30.0, 30.0, completed = true),
            item("exam", 0.0, 70.0, completed = false, type = GradeItemType.EXAM),
        )

        val projection = GradeProjection.calculate(items, scale)

        assertEquals(30.0, projection.securedPoints, 0.001)
        assertEquals(62.0, projection.examPointsNeeded[GradeLetter.A]!!, 0.001)
        assertEquals(53.0, projection.examPointsNeeded[GradeLetter.B]!!, 0.001)
    }

    @Test
    fun pendingNonExamWorkCanStillImproveExamRequirements() {
        val items = listOf(
            item("done", 10.0, 10.0, completed = true),
            item("pending", 0.0, 20.0, completed = false),
            item("exam", 0.0, 70.0, completed = false, type = GradeItemType.EXAM),
        )

        val projection = GradeProjection.calculate(items, scale)

        assertEquals(62.0, projection.examPointsNeeded[GradeLetter.A]!!, 0.001)
    }

    @Test
    fun completedExamCollapsesPossibleRangeToActualLetter() {
        val items = listOf(
            item("coursework", 30.0, 30.0, completed = true),
            item("exam", 60.0, 70.0, completed = true, type = GradeItemType.EXAM),
        )

        val projection = GradeProjection.calculate(items, scale)

        assertEquals(GradeLetter.B, projection.minimumPossibleLetter)
        assertEquals(GradeLetter.B, projection.maximumPossibleLetter)
        assertEquals(0.0, projection.examRemainingPoints, 0.001)
        assertNull(projection.examPointsNeeded[GradeLetter.A])
    }
}
