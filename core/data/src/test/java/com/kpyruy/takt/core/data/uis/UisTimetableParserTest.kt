package com.kpyruy.takt.core.data.uis

import com.kpyruy.takt.core.model.LessonType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class UisTimetableParserTest {
    @Test fun readsWeeklyItemsAndDatedBlockFootnotes() {
        val html = """
            <table id="tmtab_1"><tbody>
              <tr><td>Tue</td><td>8.00</td><td>9.50</td><td><a href="syllabus.pl?predmet=101">Physics</a></td><td>Lecture</td><td><a>T-231 (campus)</a></td><td>Teacher</td><td>group</td><td>25</td></tr>
              <tr><td>Fri</td><td>8.00</td><td>12.50</td><td>Block class <sup>(7)</sup></td><td></td><td><a>T-aula (campus)</a></td><td>Teacher</td><td></td><td></td></tr>
            </tbody></table>
            Notes:<table><tr><td>(7)</td><td>Fri 25. 9. 2026 T-aula - <a href="syllabus.pl?predmet=102">Virtual Reality</a>, Teacher</td></tr></table>
        """.trimIndent()
        val items = UisTimetableParser.parse(html)
        assertEquals(2, items.size)
        assertEquals(DayOfWeek.TUESDAY, items[0].day)
        assertEquals(LocalTime.of(8, 0), items[0].start)
        assertEquals("101", items[0].subjectId)
        assertEquals("T-231", items[0].room)
        assertEquals(null, items[0].date)
        assertEquals(LocalDate.of(2026, 9, 25), items[1].date)
        assertEquals("102", items[1].subjectId)
        assertEquals(LessonType.LECTURE, items[1].lessonType)
    }
}
