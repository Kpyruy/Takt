package com.kpyruy.takt.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AcademicRulesTest {
    @Test
    fun defaultScale_matchesApprovedBoundaries() {
        val scale = GradeScale.default()
        assertEquals(GradeLetter.A, scale.gradeFor(100.0))
        assertEquals(GradeLetter.A, scale.gradeFor(92.0))
        assertEquals(GradeLetter.B, scale.gradeFor(91.0))
        assertEquals(GradeLetter.C, scale.gradeFor(74.0))
        assertEquals(GradeLetter.D, scale.gradeFor(65.0))
        assertEquals(GradeLetter.E, scale.gradeFor(56.0))
        assertEquals(GradeLetter.FX, scale.gradeFor(55.0))
        assertEquals(GradeLetter.FX, scale.gradeFor(0.0))
    }

    @Test
    fun week39_isOddWeek() {
        assertEquals(WeekParity.ODD, WeekParity.fromIsoWeek(39))
        assertEquals(WeekParity.EVEN, WeekParity.fromIsoWeek(40))
    }
}
