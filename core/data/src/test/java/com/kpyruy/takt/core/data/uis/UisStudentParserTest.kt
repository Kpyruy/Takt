package com.kpyruy.takt.core.data.uis

import com.kpyruy.takt.core.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class UisStudentParserTest {
    @Test fun paidScholarshipsSupportRealTdHeadersAndDoNotIncludeSummaryRows() {
        val html=javaClass.getResource("/students/scholarships-paid.html")!!.readText()
        val rows=UisStudentParser.scholarships(html,"101")
        assertEquals(2,rows.size);assertEquals(40000L,rows.sumOf { it.amountMinor })
        assertTrue(rows.all { it.currency=="EUR" && it.studyId=="101" && it.paidOn!=null })
    }
    @Test fun onlyExplicitNoScholarshipsPhraseIsKnownEmpty() {
        assertEquals(emptyList<ScholarshipPayment>(),UisStudentParser.scholarships("<p>No scholarships have been paid to you for the selected study.</p>","101"))
        assertThrows(IllegalArgumentException::class.java) { UisStudentParser.scholarships("<table id=tmtab_1><tr><th>Criterion</th><th>Status</th></tr></table>","101") }
    }
    @Test fun financeHeadersIncludeHiddenColumnsAndManualSumOverridesOriginal() {
        val html="""<table id=table_1><thead><tr><th>Ord.</th><th>Status</th><th>Account number</th><th>Type</th><th>Issue date of decision</th><th>Sum</th><th>Manually</th><th>Settled</th><th>Detail</th></tr></thead><tbody><tr><td>1</td><td><span aria-label="partially paid"></span></td><td>masked</td><td>ISIC renewal</td><td>09/01/2026</td><td>€ 22</td><td>€ 20</td><td>€ 3.08</td><td><a href="/auth/student/financovani.pl?studium=101;fakturace=500;detail=1">View information</a></td></tr></tbody></table>"""
        val f=UisStudentParser.fees(html,"101").single()
        assertEquals(2000L,f.amountMinor);assertEquals(308L,f.paidMinor);assertEquals(LocalDate.of(2026,9,1),f.issuedDate)
        assertNull(f.dueDate);assertTrue(f.recordUrl!!.startsWith("https://is.stuba.sk/"))
    }
    @Test fun moneyIsExactAndRejectsUnknownCurrencyOrPrecision() {
        assertEquals(123456L to "EUR",UisStudentParser.money("€ 1 234,56"))
        assertEquals(308L to "EUR",UisStudentParser.money("€ 3.08"))
        assertThrows(IllegalArgumentException::class.java) { UisStudentParser.money("3.086") }
        assertThrows(IllegalArgumentException::class.java) { UisStudentParser.money("not a price") }
    }
    @Test fun twoEnrolledStudiesRespectExplicitActivePortalRow() {
        val html="""<table><tr><th>Choose</th><th>Faculty</th><th>Study</th><th>Progress of study</th></tr>
            <tr><td><a href="moje_studium.pl?studium=303">Choose</a></td><td>FEI</td><td>Other</td><td>enrolled<a href="studium.pl?studium=303;obdobi=700">Details</a></td></tr>
            <tr><td><span aria-label=Active></span></td><td>MTF</td><td>Current</td><td>enrolled<a href="studium.pl?studium=101;obdobi=741">Details</a></td></tr></table>"""
        assertEquals("101",UisStudentParser.portal(html).currentStudyId)
    }
    @Test fun portalSeparatesCurrentlyEnrolledAndArchivedStudies() {
        val html="""<div id=log>Logged in: Alex Novak</div><table><thead><tr><th>Choose</th><th>Faculty</th><th>Study</th><th>Progress of study</th></tr></thead><tbody><tr><td><span aria-label=Active></span></td><td>MTF</td><td>Automation<br>full-time 3</td><td>enrolled<a href="/auth/student/studium.pl?studium=101;obdobi=741">Details</a></td></tr><tr><td><a href="moje_studium.pl?studium=202">Choose</a></td><td>MTF</td><td>Old programme<br>full-time 4</td><td>study requirements not met<a href="zp_temata.pl?studium=202;obdobi=722">Thesis</a></td></tr></tbody></table>"""
        val hub=UisStudentParser.portal(html)
        assertEquals("101",hub.currentStudyId);assertEquals("Alex Novak",hub.identity!!.name)
        assertEquals(listOf(true,false),hub.studies.map { it.active });assertEquals("722",hub.studies.last().periodId)
    }
}
