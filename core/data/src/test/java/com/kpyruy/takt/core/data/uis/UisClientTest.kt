package com.kpyruy.takt.core.data.uis

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.After
import org.junit.Test

class UisClientTest {
    private val server = MockWebServer().apply { start() }
    private val client = UisClient(server.url("/"))
    @After fun close() { server.shutdown() }
    private val login = """<form method="post" action="/system/login.pl"><input type="hidden" name="auth_id_hidden" value="17"><input name="credential_0"><input name="credential_1" type="password"><input name="credential_k" disabled></form>"""
    private val authenticated = """<a href="/system/logout.pl">Logout</a><a href="/auth/student/moje_studium.pl">Study</a>"""
    private fun page(body: String) = MockResponse().setBody(body)
    private fun redirect(path: String) = MockResponse().setResponseCode(302).setHeader("Location", path)

    @Test fun discoversPersonalLinksAndRequestsListWithNotes() {
        server.enqueue(page(login)); server.enqueue(page("""{"need2FA":"false"}"""))
        server.enqueue(redirect("/auth/").addHeader("Set-Cookie", "UISAuth=fake; Path=/"))
        server.enqueue(page(authenticated))
        assertEquals(UisResult.CONNECTED, client.login("student", "password"))
        server.enqueue(page("""
            <a href="/system/logout.pl">Logout</a>
            <a href="/auth/student/harmonogram.pl?obdobi=741;fakulta=60">Calendar</a>
            <a href="/auth/katalog/rozvrhy_view.pl?rozvrh_student_obec=1;format=html;rozvrh_student=321">Timetable</a>
        """))
        val links = client.readStudyLinks("123", "741")
        assertTrue(links.timetable.toString().contains("rozvrh_student=321"))
        server.enqueue(page("""
            <a href="/system/logout.pl">Logout</a><table>
            <tr><td></td><td><b>09/21/2026</b> - <b>12/12/2026</b></td><td>Teaching part</td></tr>
            <tr><td></td><td><b>12/14/2026</b> - <b>02/13/2027</b></td><td>Exam period</td></tr>
            </table>
        """))
        assertEquals("2026-09-21", client.readAcademicCalendar(links.calendar,
            java.time.LocalDate.of(2026, 9, 27)).studyStart.toString())
        server.enqueue(page("""
            <a href="/system/logout.pl">Logout</a>
            <form method="post" action="/auth/katalog/rozvrhy_view.pl">
              <input type="hidden" name="rozvrh_student" value="321">
              <input type="checkbox" name="nezvol_all" value="1" checked>
              <input type="checkbox" name="poznamky" value="1" checked>
              <select name="format"><option value="html" selected>HTML</option><option value="list">List</option></select>
            </form>
        """))
        server.enqueue(page("""
            <a href="/system/logout.pl">Logout</a>
            <table id="tmtab_1"><tbody><tr><td>Mon</td><td>13.00</td><td>14.50</td>
            <td><a href="syllabus.pl?predmet=101">Physics</a></td><td>Lecture</td><td>T-231</td>
            <td>Teacher</td><td>group</td><td>25</td></tr></tbody></table>
        """))
        assertEquals(1, client.readTimetable(links.timetable).size)
        repeat(7) { server.takeRequest() }
        val post = server.takeRequest()
        assertEquals("POST", post.method)
        val body = post.body.readUtf8()
        assertTrue(body.contains("format=list"))
        assertTrue(body.contains("nezvol_all=1"))
        assertTrue(body.contains("poznamky=1"))
    }

    @Test fun loginKeepsHiddenFieldsAndVerifiesSession() {
        server.enqueue(page(login)); server.enqueue(page("""{"need2FA":"false"}"""))
        server.enqueue(redirect("/auth/?lang=sk").addHeader("Set-Cookie", "UISAuth=fake-test-token; Path=/; HttpOnly"))
        server.enqueue(page(authenticated))
        assertEquals(UisResult.CONNECTED, client.login("student+a", "p&=word"))
        server.takeRequest(); server.takeRequest()
        val post = server.takeRequest()
        assertEquals("POST", post.method)
        assertTrue(post.body.readUtf8().contains("credential_1=p%26%3Dword"))
        assertEquals("UISAuth=fake-test-token", server.takeRequest().getHeader("Cookie"))
    }
    @Test fun authenticatedStudyPlanRequestReusesOnlyTheMemorySession() {
        server.enqueue(page(login)); server.enqueue(page("""{"need2FA":"false"}"""))
        server.enqueue(redirect("/auth/?lang=sk").addHeader("Set-Cookie", "UISAuth=fake-test-token; Path=/; HttpOnly"))
        server.enqueue(page(authenticated))
        assertEquals(UisResult.CONNECTED, client.login("student", "password"))
        server.enqueue(page("""
            <a href="/system/logout.pl">Logout</a>
            <form name="formular"><input name="obdobi" value="741"><select name="studium"><option value="123" selected>Study</option></select></form>
            <table id="tmtab_1"><tr><td colspan="6">1st semester</td></tr>
            <tr><td><a href="../katalog/syllabus.pl?predmet=101">CODE_6B</a></td><td>Course</td><td>Exm</td><td>5</td><td>1x</td><td>ENROLLED</td></tr></table>
        """))
        server.enqueue(page("""
            <a href="/system/logout.pl">Logout</a>
            <table id="tmtab_1"><tr><td><a href="../katalog/syllabus.pl?predmet=101">CODE_6B</a></td>
            <td>Slovenský predmet</td><td>Sk</td><td>5</td><td>1x</td><td>ZAPÍSANÉ</td></tr></table>
        """))

        val plan = client.readStudyPlan()
        assertEquals(1, plan.courses.size)
        assertEquals("Slovenský predmet", plan.courses.single().titleSk)
        repeat(4) { server.takeRequest() }
        val request = server.takeRequest()
        assertEquals("/auth/studijni/studijni_povinnosti.pl?lang=en", request.path)
        assertEquals("UISAuth=fake-test-token", request.getHeader("Cookie"))
    }
    @Test fun wrongPasswordIsNotSuccess() {
        server.enqueue(page(login)); server.enqueue(page("""{"need2FA":"false"}""")); server.enqueue(page(login)); server.enqueue(page("""{"need2FA":"false"}"""))
        assertEquals(UisResult.INVALID_CREDENTIALS, client.login("student", "wrong"))
    }
    @Test fun cookieWithoutAuthenticatedPageIsNotSuccess() {
        server.enqueue(page(login)); server.enqueue(page("""{"need2FA":"false"}""")); server.enqueue(page("maintenance").addHeader("Set-Cookie", "UISAuth=fake; Path=/"))
        assertEquals(UisResult.UNEXPECTED_RESPONSE, client.login("student", "password"))
    }
    @Test fun rejectsExternalFormActionWithoutPostingPassword() {
        server.enqueue(page(login.replace("/system/login.pl", "https://example.org/steal")))
        assertEquals(UisResult.UNEXPECTED_RESPONSE, client.login("student", "password"))
        assertEquals(1, server.requestCount)
    }
    @Test fun rejectsExternalRedirect() {
        server.enqueue(page(login)); server.enqueue(page("""{"need2FA":"false"}""")); server.enqueue(redirect("https://example.org/"))
        assertEquals(UisResult.UNEXPECTED_RESPONSE, client.login("student", "password"))
        assertEquals(3, server.requestCount)
    }
    @Test fun asksForAndSubmitsSecondFactor() {
        server.enqueue(page(login)); server.enqueue(page("""{"need2FA":"false"}"""))
        server.enqueue(page("""<form action="/system/login.pl" method="post"><input type="hidden" name="auth_id_hidden" value="42"><input name="credential_k"></form>"""))
        assertEquals(UisResult.SECOND_FACTOR, client.login("student", "password"))
        server.enqueue(redirect("/auth/").addHeader("Set-Cookie", "UISAuth=fake; Path=/"))
        server.enqueue(page(authenticated))
        assertEquals(UisResult.CONNECTED, client.submitCode("123456"))
        server.takeRequest(); server.takeRequest(); server.takeRequest()
        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("credential_k=123456"))
        assertTrue(body.contains("auth_id_hidden=42"))
        assertFalse(body.contains("password"))
    }
    @Test fun expiredSessionDoesNotReplayCredentials() {
        server.enqueue(page(login)); server.enqueue(page("""{"need2FA":"false"}""")); server.enqueue(redirect("/auth/").addHeader("Set-Cookie", "UISAuth=fake; Path=/")); server.enqueue(page(authenticated))
        assertEquals(UisResult.CONNECTED, client.login("student", "password"))
        server.enqueue(page(login))
        assertEquals(UisResult.EXPIRED, client.checkSession())
        assertEquals(5, server.requestCount)
    }
    @Test fun serverFailureIsNotWrongPassword() {
        server.enqueue(MockResponse().setResponseCode(503))
        assertEquals(UisResult.UNAVAILABLE, client.login("student", "password"))
    }

    @Test fun preflightChallengePreservesAuthIdAndSubmitsCodeOnlyOnConfirmation() {
        server.enqueue(page(login))
        server.enqueue(page("""{"need2FA":"true","authID":123,"type":"totp"}"""))
        assertEquals(UisResult.SECOND_FACTOR, client.login("student", "secret"))
        assertEquals(2, server.requestCount)
        server.enqueue(redirect("/auth/").addHeader("Set-Cookie", "UISAuth=fake; Path=/"))
        server.enqueue(page(authenticated))
        assertEquals(UisResult.CONNECTED, client.submitCode("123 456"))
        server.takeRequest(); server.takeRequest()
        val request = server.takeRequest().body.readUtf8()
        assertTrue(request.contains("auth_id_hidden=123"))
        assertTrue(request.contains("auth_2fa_type=totp"))
        assertTrue(request.contains("credential_k=123456"))
    }
    @Test fun preflightErrorDoesNotSubmitAgain() {
        server.enqueue(page(login))
        server.enqueue(page("""{"error":"system-chyba_prihlaseni"}"""))
        assertEquals(UisResult.INVALID_CREDENTIALS, client.login("student", "wrong"))
        assertEquals(2, server.requestCount)
    }
    @Test fun redirectLoopIsBounded() {
        repeat(6) { server.enqueue(redirect("/system/login.pl")) }
        assertEquals(UisResult.UNEXPECTED_RESPONSE, client.login("student", "password"))
        assertEquals(6, server.requestCount)
    }
    @Test fun sessionCookieCannotAuthenticateLoginPage() {
        server.enqueue(page(login)); server.enqueue(page("""{"need2FA":"false"}"""))
        server.enqueue(redirect("/auth/").addHeader("Set-Cookie", "UISAuth=fake; Path=/"))
        server.enqueue(page(login + authenticated))
        assertEquals(UisResult.INVALID_CREDENTIALS, client.login("student", "password"))
    }

    @Test fun closedClientCannotSendCredentials() {
        server.enqueue(MockResponse().setResponseCode(503))
        client.close()
        assertEquals(UisResult.UNAVAILABLE, client.login("student", "secret"))
        assertEquals(0, server.requestCount)
    }

    @Test fun recognizesLogoutPathFromRealUisHtml() {
        server.enqueue(page(login)); server.enqueue(page("""{"need2FA":"false"}"""))
        server.enqueue(redirect("/auth/?lang=sk").addHeader("Set-Cookie", "UISAuth=fake; Path=/"))
        server.enqueue(page("""<a href="/auth/system/logout.pl?lang=sk" title="Odhlásenie">Exit</a>"""))
        assertEquals(UisResult.CONNECTED, client.login("student", "password"))
    }
    @Test fun reportsHttpCodeAndStageWithoutResponseBody() {
        server.enqueue(MockResponse().setResponseCode(503).setBody("private server data"))
        assertEquals(UisResult.UNAVAILABLE, client.login("student", "secret"))
        assertEquals(UisStage.LOGIN_FORM, client.failure?.stage)
        assertEquals(UisFailureReason.HTTP, client.failure?.reason)
        assertEquals(503, client.failure?.httpStatus)
        assertFalse(client.failure.toString().contains("private"))
    }
    @Test fun malformedPreflightHasSpecificReason() {
        server.enqueue(page(login)); server.enqueue(page("not json"))
        assertEquals(UisResult.UNEXPECTED_RESPONSE, client.login("student", "secret"))
        assertEquals(UisStage.PREFLIGHT, client.failure?.stage)
        assertEquals(UisFailureReason.RESPONSE_FORMAT, client.failure?.reason)
    }
}
