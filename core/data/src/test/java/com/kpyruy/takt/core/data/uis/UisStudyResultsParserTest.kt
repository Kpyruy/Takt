package com.kpyruy.takt.core.data.uis

import com.kpyruy.takt.core.model.GradeLetter
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class UisStudyResultsParserTest {
    @Test fun readsGradeAndFulfilledDateInsteadOfEntryDate() {
        val html = """
            <table id="tmtab_1"><tr><th>Code</th><th>Course</th><th>Result</th><th>Entered</th></tr>
              <tr><td>FYZI_6B</td><td>Physics</td><td></td><td></td></tr></table>
            <table id="tmtab_3"><tr><th>Code</th><th>Course</th><th>Result</th><th>Entered</th><th>Entered by</th></tr>
              <tr><td>ANJA2_6B</td><td>English 2</td><td>good (C)</td><td>09/18/2026 (fulfilled: 11.05.2026)</td><td>Teacher</td></tr>
              <tr><td>MATM2_6B</td><td>Math 2</td><td>excellent (A)</td><td>09/18/2026 (fulfilled: 01.07.2025)</td><td>Teacher</td></tr>
              <tr><td>TEVE1_6B</td><td>PE</td><td>passed</td><td>09/18/2026 (fulfilled: 15.06.2026)</td><td>Teacher</td></tr>
            </table>
        """.trimIndent()
        val results = UisStudyResultsParser.parse(html)
        assertEquals(UisStudyResult(GradeLetter.C, LocalDate.of(2026, 5, 11)), results["ANJA2_6B"])
        assertEquals(UisStudyResult(GradeLetter.A, LocalDate.of(2025, 7, 1)), results["MATM2_6B"])
        assertEquals(UisStudyResult(null, LocalDate.of(2026, 6, 15),
            gradeState = UisResultFieldState.UNRECOGNIZED), results["TEVE1_6B"])
        assertEquals(UisStudyResult(null, null), results["FYZI_6B"])
    }

    @Test fun retainsBlankRowsAsUnconfirmedResults() {
        val result = UisStudyResultsParser.parse(resultTable("", ""))["MATM2_6B"]

        assertEquals(UisStudyResult(null, null), result)
        assertEquals(UisResultFieldState.ABSENT, result?.gradeState)
        assertEquals(UisResultFieldState.ABSENT, result?.fulfilledOnState)
    }

    @Test fun rejectsImpossibleFulfilledDateWithoutDiscardingValidGrade() {
        val result = UisStudyResultsParser.parse(resultTable("good (C)", "fulfilled: 31.02.2026"))["MATM2_6B"]

        assertEquals(GradeLetter.C, result?.grade)
        assertNull(result?.fulfilledOn)
        assertEquals(UisResultFieldState.CONFIRMED, result?.gradeState)
        assertEquals(UisResultFieldState.UNRECOGNIZED, result?.fulfilledOnState)
    }

    @Test fun rejectsInvalidLeapDay() {
        val result = UisStudyResultsParser.parse(resultTable("failed (FX)", "fulfilled: 29.02.2025"))["MATM2_6B"]

        assertEquals(GradeLetter.FX, result?.grade)
        assertNull(result?.fulfilledOn)
    }

    @Test fun rejectsMalformedFulfilledDateInsteadOfAcceptingItsPrefix() {
        val result = UisStudyResultsParser.parse(resultTable("good (C)", "fulfilled: 11.05.2026unexpected"))["MATM2_6B"]

        assertEquals(GradeLetter.C, result?.grade)
        assertNull(result?.fulfilledOn)
    }

    @Test fun entryDateAloneDoesNotConfirmFulfilledDate() {
        val result = UisStudyResultsParser.parse(resultTable("good (C)", "09/18/2026"))["MATM2_6B"]

        assertEquals(UisStudyResult(GradeLetter.C, null), result)
    }

    @Test fun retainsValidGradeWhenDateCellIsMissing() {
        val html = """
            <table><tr><th>Code</th><th>Result</th><th>Entered</th></tr>
            <tr><td>MATM2_6B</td><td>failed (FX)</td></tr></table>
        """.trimIndent()

        assertEquals(UisStudyResult(GradeLetter.FX, null), UisStudyResultsParser.parse(html)["MATM2_6B"])
    }

    @Test fun retainsValidDateWhenGradeIsUnrecognized() {
        val result = UisStudyResultsParser.parse(resultTable("unknown (Z)", "fulfilled: 29.02.2024"))["MATM2_6B"]

        assertNull(result?.grade)
        assertEquals(LocalDate.of(2024, 2, 29), result?.fulfilledOn)
        assertEquals(UisResultFieldState.UNRECOGNIZED, result?.gradeState)
        assertEquals(UisResultFieldState.CONFIRMED, result?.fulfilledOnState)
    }

    @Test fun repeatedRowsKeepIndependentlyConfirmedFields() {
        val html = resultTable("excellent (A)", "fulfilled: 11.05.2026") +
            resultTable("good (C)", "")

        assertEquals(UisStudyResult(GradeLetter.C, LocalDate.of(2026, 5, 11)),
            UisStudyResultsParser.parse(html)["MATM2_6B"])
    }

    @Test fun missingCourseRowDoesNotCreateResult() {
        assertFalse(UisStudyResultsParser.parse(resultTable("excellent (A)", "")).containsKey("POIS_6B"))
    }

    @Test(expected = IllegalStateException::class)
    fun missingResultTableIsAnError() {
        UisStudyResultsParser.parse("<html><body>Temporary unavailable</body></html>")
    }

    @Test(expected = IllegalStateException::class)
    fun changedResultHeadingsAreAnError() {
        UisStudyResultsParser.parse("<table><tr><th>Code</th><th>New result format</th><th>Entered</th></tr></table>")
    }

    private fun resultTable(grade: String, entered: String) = """
        <table><tr><th>Code</th><th>Result</th><th>Entered</th></tr>
        <tr><td>MATM2_6B</td><td>$grade</td><td>$entered</td></tr></table>
    """.trimIndent()
}
