package com.kpyruy.takt.core.data.uis

import com.kpyruy.takt.core.data.CachedCourseTeachingRepository
import com.kpyruy.takt.core.model.*
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.*
import org.junit.Assert.*
import org.junit.Test

class UisTeachingFlowTest {
    @Test fun coldSessionResolvesCurrentPeriodAndRefreshBypassesBothCaches() = runBlocking {
        val server = MockWebServer()
        val paths = mutableListOf<String>()
        var current = "441254"
        val plan = """<form name="formular"><input name="obdobi" value="741">
            <select name="studium"><option value="210634" selected>Study</option></select></form>
            <table id="tmtab_1"><tr><td colspan="6">3rd semester</td></tr><tr>
            <td><a href="/auth/katalog/syllabus.pl?predmet=441154">TPAR_6B</a></td>
            <td>Hardware</td><td>Exm</td><td>5</td><td>1x</td><td>ENROLLED</td></tr></table>"""
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path!!; synchronized(paths) { paths.add(path) }
                val html = when {
                    path.contains("studijni_povinnosti") -> plan
                    path.contains("pruchod_studiem") -> """<table><tr><th>Code</th><th>Course</th></tr>
                        <tr><td>TPAR_6B</td><td><a href="/auth/katalog/syllabus.pl?predmet=$current;lang=en">Hardware</a></td></tr></table>"""
                    path.contains("predmet=$current;") -> javaClass.getResource("/teaching/syllabus.html")!!.readText()
                    else -> return MockResponse().setResponseCode(400)
                }
                return MockResponse().setBody("<a href='/system/logout.pl'>Logout</a><a href='/auth/student/moje_studium.pl'>Study</a>" + html)
            }
        }
        server.start()
        val client = UisClient(server.url("/"))
        val http = client.javaClass.getDeclaredField("http").apply { isAccessible = true }.get(client) as okhttp3.OkHttpClient
        http.cookieJar.saveFromResponse(server.url("/"), listOf(okhttp3.Cookie.Builder()
            .name("UISAuth").value("local-fixture").domain(server.url("/").host).path("/").build()))
        val course = Course("uis:210634:441154", "TPAR_6B", "Hardware", 5, 3, CourseStatus.ENROLLED,
            syllabusUrl = "https://is.stuba.sk/auth/katalog/syllabus.pl?predmet=441154")
        val repository = CachedCourseTeachingRepository({ c, refresh ->
            client.readCourseTeachers("441154", c.code, "210634", refresh)
        }, { id, _ -> TeacherProfile(id, "Unused") })
        try {
            assertEquals(listOf("71659", "116374", "111582", "2758"), repository.teachers(course).map { it.personId })
            assertEquals(3, paths.size)
            repository.teachers(course)
            assertEquals(3, paths.size)
            current = "441255"
            repository.teachers(course, refresh = true)
            assertEquals(6, paths.size)
            assertEquals("/auth/katalog/syllabus.pl?predmet=441255;lang=en", paths.last())
            assertTrue(paths.none { it.contains("predmet=441154") })
            assertEquals("uis:210634:441154", course.id)
        } finally { client.close(); server.shutdown() }
    }

    @Test fun progressReadsOnlyCourseRowsAndPrefersCurrentPeriodToRecognizedCourseRows() {
        val html = """<a href="/auth/katalog/syllabus.pl?predmet=99">Navigation</a>
            <table><tr><th>Code</th><th>Course</th></tr><tr><td>TPAR_6B</td>
            <td><a href="/auth/katalog/syllabus.pl?predmet=441254">Hardware</a></td></tr>
            <tr><td>OTHER_6B</td><td><a href="https://foreign.test/auth/katalog/syllabus.pl?predmet=5">Wrong</a></td></tr></table>
            <table><tr><th>Code</th><th>Course</th></tr><tr><td>TPAR_6B</td>
            <td><a href="/auth/katalog/syllabus.pl?predmet=441154">Recognized</a></td></tr></table>"""
        assertEquals(mapOf("TPAR_6B" to "441254"), UisTeachingParser.progressSyllabi(html))
    }
}
