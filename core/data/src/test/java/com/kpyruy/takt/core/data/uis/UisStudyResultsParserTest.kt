package com.kpyruy.takt.core.data.uis

import com.kpyruy.takt.core.model.GradeLetter
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        assertEquals(UisStudyResult(null, LocalDate.of(2026, 6, 15)), results["TEVE1_6B"])
        assertFalse(results.containsKey("FYZI_6B"))
    }
}
