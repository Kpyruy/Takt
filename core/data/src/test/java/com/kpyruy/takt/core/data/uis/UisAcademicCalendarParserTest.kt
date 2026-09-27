package com.kpyruy.takt.core.data.uis

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class UisAcademicCalendarParserTest {
    @Test fun mergesExamRowsAcrossChristmasHoliday() {
        val html = """
            <table>
              <tr><td></td><td><b>09/21/2026</b> 00:00 - <b>12/12/2026</b> 23:59</td><td>Teaching part</td></tr>
              <tr><td></td><td><b>12/14/2026</b> 00:00 - <b>12/19/2026</b> 23:59</td><td>Exam period</td></tr>
              <tr><td></td><td><b>12/21/2026</b> 00:00 - <b>01/02/2027</b> 23:59</td><td>Christmas holiday</td></tr>
              <tr><td></td><td><b>01/04/2027</b> 00:00 - <b>02/13/2027</b> 23:59</td><td>Exam period</td></tr>
              <tr><td></td><td><b>02/15/2027</b> 00:00 - <b>05/15/2027</b> 23:59</td><td>Teaching part (summer)</td></tr>
            </table>
        """.trimIndent()
        val result = UisAcademicCalendarParser.parse(html, LocalDate.of(2026, 9, 27))
        assertEquals(LocalDate.of(2026, 9, 21), result.studyStart)
        assertEquals(LocalDate.of(2026, 12, 12), result.studyEnd)
        assertEquals(LocalDate.of(2026, 12, 14), result.examStart)
        assertEquals(LocalDate.of(2027, 2, 13), result.examEnd)
        val duringExams = UisAcademicCalendarParser.parse(html, LocalDate.of(2027, 1, 10))
        assertEquals(result, duringExams)
    }
}
