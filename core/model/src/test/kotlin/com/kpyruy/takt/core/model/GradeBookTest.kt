package com.kpyruy.takt.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class GradeBookTest {
    @Test
    fun examAndTests_areSummedIntoFinalPercentageAndLetter() {
        val items = listOf(
            GradeItem("tests", "course", "Тести", GradeItemType.TEST, earnedPoints = 20.0, maxPoints = 40.0),
            GradeItem("exam", "course", "Екзамен", GradeItemType.EXAM, earnedPoints = 40.0, maxPoints = 60.0),
        )

        val summary = GradeSummary.calculate(items, GradeScale.default())

        assertEquals(60.0, summary.earnedPoints, 0.001)
        assertEquals(100.0, summary.maxPoints, 0.001)
        assertEquals(60.0, summary.percentage, 0.001)
        assertEquals(GradeLetter.E, summary.letter)
    }

    @Test
    fun differentSubjectMaximum_isConvertedToPercentage() {
        val items = listOf(
            GradeItem("tests", "course", "Тести", GradeItemType.TEST, earnedPoints = 28.0, maxPoints = 50.0),
        )

        val summary = GradeSummary.calculate(items, GradeScale.default())

        assertEquals(56.0, summary.percentage, 0.001)
        assertEquals(GradeLetter.E, summary.letter)
    }

    @Test
    fun fiftyFivePercent_isFx() {
        val items = listOf(
            GradeItem("all", "course", "Разом", GradeItemType.OTHER, earnedPoints = 55.0, maxPoints = 100.0),
        )

        assertEquals(
            GradeLetter.FX,
            GradeSummary.calculate(items, GradeScale.default()).letter,
        )
    }

    @Test
    fun emptyGradeBook_hasNoLetter() {
        val summary = GradeSummary.calculate(emptyList(), GradeScale.default())

        assertEquals(0.0, summary.earnedPoints, 0.001)
        assertEquals(0.0, summary.maxPoints, 0.001)
        assertEquals(null, summary.letter)
    }
}
