package com.kpyruy.takt.core.data.uis

import com.kpyruy.takt.core.model.CourseRequirementType
import com.kpyruy.takt.core.model.CourseStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class UisStudyPlanParserTest {
    @Test fun readsCreditsSemestersAndCourseStatesFromUisTable() {
        val html = """
            <form name="formular"><input type="hidden" name="obdobi" value="741" />
              <select name="studium"><option value="123" selected>Study</option></select></form>
            <table><tr><td>Credits:</td><td><b>81 obtained</b><b> out of 180 of compulsory</b></td></tr></table>
            <table id="tmtab_1"><tbody>
              <tr><td colspan="6"><b>1st semester WS 2026/2027</b></td></tr>
              <tr class="predmety_vse predmety_vse_pov-test"><td><a href="../katalog/syllabus.pl?predmet=101">CODE_6B</a></td><td>Course</td><td>Exm</td><td>5</td><td>1x</td><td>FULFILLED (date)</td></tr>
              <tr><td colspan="6"><b>2nd semester SS 2026/2027</b></td></tr>
              <tr class="predmety_vse predmety_vse_pv-test"><td><a href="../katalog/syllabus.pl?predmet=102">OTHER_6B</a></td><td>Other</td><td>PassCD</td><td>2</td><td>0x</td><td>NOT ENROLLED</td></tr>
              <tr class="predmety_vse predmety_vse_vyb-test"><td><a href="../katalog/syllabus.pl?predmet=103">THIRD_6B</a></td><td>Third</td><td>GrdCD</td><td>3</td><td>1x</td><td>ENROLLED</td></tr>
            </tbody></table>
        """.trimIndent()

        val result = UisStudyPlanParser.parse(html)

        assertEquals("123", result.studyId)
        assertEquals("741", result.periodId)
        assertEquals(81, result.earnedCredits)
        assertEquals(180, result.requiredCredits)
        assertEquals(3, result.courses.size)
        assertEquals(1, result.courses[0].semester)
        assertEquals(CourseStatus.FULFILLED, result.courses[0].status)
        assertEquals(CourseRequirementType.COMPULSORY, result.courses[0].requirementType)
        assertEquals("101", result.courses[0].subjectId)
        assertEquals(CourseStatus.NOT_ENROLLED, result.courses[1].status)
        assertEquals(CourseRequirementType.SEMI_COMPULSORY, result.courses[1].requirementType)
        assertEquals(CourseStatus.ENROLLED, result.courses[2].status)
        assertEquals(CourseRequirementType.ELECTIVE, result.courses[2].requirementType)
    }

    @Test fun rejectsPageWithoutStudyPlanInsteadOfImportingNothing() {
        runCatching { UisStudyPlanParser.parse("<html><body>Login</body></html>") }
            .onSuccess { throw AssertionError("Parser accepted a login page") }
    }
}
