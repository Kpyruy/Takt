package com.kpyruy.takt.core.data.uis

import com.kpyruy.takt.core.model.CourseRequirementType
import com.kpyruy.takt.core.model.CourseStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class UisStudyPlanParserTest {
    @Test fun selectedStudyTermParityAndYearDetermineCurrentSemester() {
        val cases = mapOf(
            (1 to 1) to 1,
            (2 to 1) to 2,
            (1 to 2) to 3,
            (3 to 2) to 3,
            (5 to 2) to 3,
            (4 to 2) to 4,
            (5 to 3) to 5,
            (6 to 3) to 6,
        )
        cases.forEach { (termAndYear, semester) ->
            val (term, year) = termAndYear
            assertEquals("term $term, year $year", semester,
                UisStudyPlanParser.parse(planWithStudy("MTF B-PIAR den [term $term, year $year]")).currentSemester)
        }
    }

    @Test fun selectedStudyIsUsedInsteadOfAnotherStudyInSelector() {
        val html = planWithStudy("MTF B-PIAR den [term 1, year 2]")
            .replace("<option value=\"123\" selected>",
                "<option value=\"987\">Other [term 6, year 3]</option><option value=\"123\" selected>")
        assertEquals(3, UisStudyPlanParser.parse(html).currentSemester)
    }

    @Test fun visiblePlanHeadingIsUsedWhenSelectorHasNoTerm() {
        val html = planWithStudy("Study")
            .replace("<table id=\"tmtab_1\">",
                "<table><tr><td></td><td><b>MTF B-PIAR den [term 5, year 3]</b></td><td></td></tr></table><table id=\"tmtab_1\">")
        assertEquals(5, UisStudyPlanParser.parse(html).currentSemester)
    }

    @Test fun missingOrInvalidStudyTermLeavesSemesterForFallback() {
        assertEquals(null, UisStudyPlanParser.parse(planWithStudy("MTF B-PIAR den")).currentSemester)
        assertEquals(null, UisStudyPlanParser.parse(planWithStudy("MTF B-PIAR den [term 0, year 2]")).currentSemester)
    }

    private fun planWithStudy(studyLabel: String) = """
        <form name="formular"><input name="obdobi" value="741" />
          <select name="studium"><option value="123" selected>$studyLabel</option></select></form>
        <table id="tmtab_1"><tr><td colspan="6">1st semester</td></tr>
          <tr><td><a href="syllabus.pl?predmet=101">CODE_6B</a></td><td>Course</td>
              <td>Exm</td><td>5</td><td>1x</td><td>ENROLLED</td></tr></table>
    """.trimIndent()

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
