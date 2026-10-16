package com.kpyruy.takt.core.data.uis

import org.junit.Assert.*
import org.junit.Test

class UisTeachingParserTest {
    private fun fixture(name: String) = javaClass.getResource("/teaching/$name")!!.readText()

    @Test fun readsEveryTeacherAndTheirRolesWithoutPageNavigationOrEditors() {
        val teachers = UisTeachingParser.teachers(fixture("syllabus.html"))
        assertEquals(listOf("71659", "116374", "111582", "2758"), teachers.map { it.personId })
        assertEquals("Ing. Martin Bartoň, PhD.", teachers.first().name)
        assertEquals(listOf("examiner", "instructor", "lecturer"), teachers.first().roles)
        assertTrue(teachers.last().roles.contains("person responsible for course"))
        assertEquals(listOf("slovak"), teachers.first().languages)
        assertEquals("https://is.stuba.sk/auth/lide/clovek.pl?id=71659", teachers.first().profileUrl)
    }

    @Test fun contactsNormalizeMailObfuscationAndKeepOfficeAndMailboxTimestamp() {
        val profile = UisTeachingParser.profile(fixture("person.html"), "71659")
        assertEquals("Ing. Martin Bartoň, PhD.", profile.name)
        assertEquals("martin.barton@stuba.sk", profile.universityEmail)
        assertEquals("xbarton@is.stuba.sk", profile.systemEmail)
        assertEquals("T02 4.108", profile.officeNumber)
        assertEquals("UIAM MTF, Ulica Jána Bottu 2781/25, 91724 Trnava", profile.officeAddress)
        assertEquals("+421 906 068 422", profile.officePhone)
        assertEquals("10/15/2026 07:39", profile.mailboxLastChecked)
        assertNull(profile.consultationHours)
    }

    @Test fun acceptsSlovakContactLabelsAndMultilineConsultationHours() {
        val profile = UisTeachingParser.profile("""
            <h1>Jana Nová</h1><p>Identifikačné číslo: 123</p>
            <table><tr><td>Univerzitný e-mail:</td><td>jana [at] stuba.sk</td></tr>
            <tr><td>Číslo kancelárie:</td><td>T02 4.110</td></tr>
            <tr><td>Konzultačné hodiny:</td><td>Utorok 10:00–11:00<br>Štvrtok po dohode</td></tr></table>
        """, "123")
        assertEquals("jana@stuba.sk", profile.universityEmail)
        assertEquals("T02 4.110", profile.officeNumber)
        assertEquals("Utorok 10:00–11:00\nŠtvrtok po dohode", profile.consultationHours)
    }

    @Test fun noConsultationHoursAreInventedFromCourseWorkload() {
        val html = "<h1>Example</h1><p>Identification number: 123</p><p>Consultations: 13 hours</p>"
        assertNull(UisTeachingParser.profile(html, "123").consultationHours)
    }

    @Test fun rejectsLoginAndWrongPersonInsteadOfPresentingThemAsEmptyContacts() {
        for (html in listOf("<input name=credential_1>", "<h1>Other person</h1><p>Identification number: 999</p>")) {
            assertTrue(runCatching { UisTeachingParser.profile(html, "71659") }.isFailure)
        }
    }

    @Test fun doesNotFollowExternalOrCredentialBearingTeacherLinks() {
        val html = """<table><tr><td>Name of lecturer(s):</td></tr><tr><td>
            <a href="https://evil.test/auth/lide/clovek.pl?id=7">Fake</a> (lecturer)<br>
            <a href="https://someone@is.stuba.sk/auth/lide/clovek.pl?id=8">Fake 2</a> (lecturer)<br>
            <a href="/auth/lide/clovek.pl?id=9">Real</a> (instructor)
            </td></tr></table>"""
        assertEquals(listOf("9"), UisTeachingParser.teachers(html).map { it.personId })
    }
}
