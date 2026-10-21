package com.kpyruy.takt.app

import com.kpyruy.takt.core.model.UiKey
import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.kpyruy.takt.core.data.CourseTeachingRepository
import com.kpyruy.takt.core.model.*
import com.kpyruy.takt.core.ui.components.TaktGlassBackground
import com.kpyruy.takt.core.ui.i18n.TaktI18n
import com.kpyruy.takt.core.ui.theme.TaktTheme
import com.kpyruy.takt.feature.subjects.SubjectDetailScreen
import com.kpyruy.takt.feature.subjects.teacherEmailIntent
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class TeacherDirectoryUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val data get() = (compose.activity.application as TaktApplication).dataContainer
    private val team = listOf(
        CourseTeacher("71659", "Ing. Martin Bartoň, PhD.", listOf("examiner", "instructor", "lecturer")),
        CourseTeacher("116374", "Ing. Lucia Ďurišová-Rubanská", listOf("instructor")),
        CourseTeacher("111582", "Ing. Lukáš Gendiar", listOf("instructor")),
        CourseTeacher("2758", "prof. Mgr. Róbert Vrábeľ, PhD.", listOf("person responsible for course")),
    )

    private fun showSubject(failProfileOnce: Boolean = false, replay: CourseTeachingRepository? = null) {
        runBlocking {
            data.seedLiquidGlassReview()
            data.database.courseDao().insertAll(data.database.courseDao().getAllSnapshot().map {
                if (it.id == "glass-programming") it.copy(code = "TPAR_6B", title = "Technické prostriedky automatického riadenia", iconKey = "DeveloperBoard", syllabusUrl = "https://is.stuba.sk/auth/katalog/syllabus.pl?predmet=441254") else it
            })
            data.settingsRepository.setLanguage(AppLanguage.SLOVAK)
            data.gradeRepository.observeItems("glass-programming").first().forEach {
                data.gradeRepository.upsertItem(it.copy(title = "Priebežný test"))
            }
            data.studyContentRepository.observeTasks("glass-programming").first().forEach {
                data.studyContentRepository.upsertTask(it.copy(title = "Laboratórna úloha"))
            }
            data.studyContentRepository.upsertNote(CourseNote("teacher-note", "glass-programming", "This note belongs only in Notes", "Preview must stay hidden", 0))
        }
        var fail = failProfileOnce
        val directory = object : CourseTeachingRepository {
            override suspend fun teachers(course: Course, refresh: Boolean) = team
            override suspend fun profile(teacher: CourseTeacher, language: AppLanguage, refresh: Boolean): TeacherProfile {
                if (fail) { fail = false; throw java.io.IOException("offline") }
                return TeacherProfile(teacher.personId, teacher.name,
                    "Odborný asistent CSc.,PhD. · UIAM MTF STU",
                    universityEmail = "martin.barton@stuba.sk", systemEmail = "xbarton@is.stuba.sk",
                    officeAddress = "UIAM MTF, Ulica Jána Bottu 2781/25, 91724 Trnava",
                    officeNumber = "T02 4.108", officePhone = "+421 906 068 422",
                    mailboxLastChecked = "10/15/2026 07:39")
            }
        }
        compose.activityRule.scenario.onActivity { activity ->
            TaktI18n.use(AppLanguage.SLOVAK)
            activity.setContent {
                TaktTheme(AppSettings(language = AppLanguage.SLOVAK, themeMode = AppThemeMode.DARK, themeFamily = ThemeFamily.BLUE)) {
                    TaktGlassBackground {
                        SubjectDetailScreen(data.studyPlanRepository, data.gradeRepository, data.scheduleRepository,
                            data.settingsRepository, data.studyContentRepository, data.examRepository, data.documentStore,
                            uisManaged = true, courseId = "glass-programming", onBack = {}, teachingRepository = replay ?: directory)
                    }
                }
            }
        }
        compose.waitUntil(8_000) { compose.onAllNodesWithTag("course-teachers").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun overviewHasAllFourTeachersInsteadOfNotePreviewsAndProfileShowsClickableContacts() {
        showSubject()
        compose.onNodeWithText("This note belongs only in Notes").assertDoesNotExist()
        for (teacher in team) compose.onNodeWithTag("course-teacher-${teacher.personId}").performScrollTo().assertIsDisplayed()
        capture("teachers-sk-list.png")
        compose.onNodeWithTag("course-teacher-71659").performScrollTo().performClick()
        compose.onNodeWithTag("teacher-email-university").performScrollTo().assertHasClickAction()
        compose.onNodeWithText("martin.barton@stuba.sk").assertIsDisplayed()
        compose.onNodeWithTag("teacher-email-uis").assertHasClickAction()
        compose.onNodeWithText("xbarton@is.stuba.sk").assertIsDisplayed()
        capture("teachers-sk-profile-top.png")
        compose.onNodeWithTag("teacher-office").performScrollTo()
        compose.onNodeWithText("T02 4.108").assertIsDisplayed()
        compose.onNodeWithTag("teacher-mailbox-checked").performScrollTo()
        compose.onNodeWithText("15.10.2026 07:39").assertIsDisplayed()
        compose.onNodeWithText("Neuvedené v UIS").assertExists()
        capture("teachers-sk-profile-office.png")
        val intent = teacherEmailIntent("martin.barton@stuba.sk")
        assertEquals(Intent.ACTION_SENDTO, intent.action)
        assertEquals("mailto", intent.data!!.scheme)
        assertEquals("martin.barton@stuba.sk", intent.data!!.schemeSpecificPart)
        val uisIntent = teacherEmailIntent("xbarton@is.stuba.sk", "71659")
        assertEquals(Intent.ACTION_VIEW, uisIntent.action)
        assertEquals("https://is.stuba.sk/auth/posta/nova_zprava.pl?uzivatel=71659", uisIntent.dataString)
        val universityIntent = teacherEmailIntent("martin.barton@stuba.sk", "71659")
        assertEquals("martin.barton@stuba.sk", universityIntent.data!!.schemeSpecificPart)
        // ModalBottomSheet owns its dialog window, so dismiss through its actual control.
        compose.onNodeWithContentDescription(com.kpyruy.takt.core.ui.i18n.t(UiKey.ACTION_CLOSE)).performScrollTo().performClick()
        compose.onNodeWithTag("teacher-profile").assertDoesNotExist()
        compose.onNodeWithText("Poznámky").performClick()
        compose.onNodeWithText("This note belongs only in Notes").assertIsDisplayed()
    }

    @Test fun aFailedProfileCanBeRetriedWithoutLosingTheTeachingTeam() {
        showSubject(failProfileOnce = true)
        compose.onNodeWithTag("course-teacher-71659").performScrollTo().performClick()
        compose.onNodeWithTag("teacher-retry").performScrollTo().performClick()
        compose.onNodeWithTag("teacher-email-university").performScrollTo().assertIsDisplayed()
    }

    @Test fun actualClientParserAndCacheResolveCurrentTeamAndSlovakOfficeBeforeRendering() {
        val server = okhttp3.mockwebserver.MockWebServer()
        val requests = java.util.Collections.synchronizedList(mutableListOf<String>())
        val assets = InstrumentationRegistry.getInstrumentation().context.assets
        fun fixture(name: String) = assets.open(name).bufferedReader().use { it.readText() }
        server.dispatcher = object : okhttp3.mockwebserver.Dispatcher() {
            override fun dispatch(request: okhttp3.mockwebserver.RecordedRequest): okhttp3.mockwebserver.MockResponse {
                val path = request.path.orEmpty(); requests.add(path)
                val html = when {
                    path == "/auth/studijni/studijni_povinnosti.pl?lang=en" -> """
                        <form name="formular"><input name="obdobi" value="741">
                        <select name="studium"><option value="210634" selected>Study</option></select></form>
                        <table id="tmtab_1"><tr><td colspan="6">3rd semester</td></tr><tr>
                        <td><a href="/auth/katalog/syllabus.pl?predmet=441154">TPAR_6B</a></td>
                        <td>Hardware</td><td>Exm</td><td>5</td><td>1x</td><td>ENROLLED</td></tr></table>"""
                    path.startsWith("/auth/student/pruchod_studiem.pl?") -> """
                        <table><tr><th>Code</th><th>Course</th><th>Result</th><th>Entered</th></tr>
                        <tr><td>TPAR_6B</td><td><a href="/auth/katalog/syllabus.pl?predmet=441254;lang=en">Hardware</a></td>
                        <td></td><td></td></tr></table>"""
                    path == "/auth/katalog/syllabus.pl?predmet=441254;lang=en" -> fixture("syllabus.html")
                    path == "/auth/lide/clovek.pl?id=2758;zalozka=1;lang=sk" -> fixture("vrabel-sk.html")
                    path.startsWith("/auth/lide/foto.pl?") -> return okhttp3.mockwebserver.MockResponse().setResponseCode(404)
                    else -> return okhttp3.mockwebserver.MockResponse().setResponseCode(400)
                }
                return okhttp3.mockwebserver.MockResponse().setBody("<a href='/system/logout.pl'>Logout</a><a href='/auth/student/moje_studium.pl'>Study</a>" + html)
            }
        }
        val certificate = okhttp3.tls.HeldCertificate.Builder().commonName("localhost")
            .addSubjectAlternativeName("localhost").build()
        val serverCertificates = okhttp3.tls.HandshakeCertificates.Builder().heldCertificate(certificate).build()
        val clientCertificates = okhttp3.tls.HandshakeCertificates.Builder().addTrustedCertificate(certificate.certificate).build()
        server.useHttps(serverCertificates.sslSocketFactory(), false)
        server.start()
        // Internal production types are reflected only in the test, without adding a product debug API.
        val clientType = Class.forName("com.kpyruy.takt.core.data.uis.UisClient")
        val client = clientType.getDeclaredConstructor(okhttp3.HttpUrl::class.java).newInstance(server.url("/"))
        // Trust only this test's local HTTPS certificate, leaving the product transport policy intact.
        val transport = clientType.getDeclaredField("http").apply { isAccessible = true }
        val existingTransport = transport.get(client) as okhttp3.OkHttpClient
        existingTransport.cookieJar.saveFromResponse(server.url("/"), listOf(okhttp3.Cookie.Builder()
            .name("UISAuth").value("local-fixture").domain(server.url("/").host).path("/").build()))
        transport.set(client, existingTransport.newBuilder()
            .sslSocketFactory(clientCertificates.sslSocketFactory(), clientCertificates.trustManager).build())
        val loadTeam: suspend (Course, Boolean) -> List<CourseTeacher> = { course, refresh ->
            @Suppress("UNCHECKED_CAST")
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                clientType.getDeclaredMethod("readCourseTeachers", String::class.java, String::class.java,
                    String::class.java, Boolean::class.javaPrimitiveType)
                    .invoke(client, "441154", course.code, "210634", refresh) as List<CourseTeacher>
            }
        }
        val loadProfile: suspend (String, AppLanguage) -> TeacherProfile = { id, language ->
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                clientType.getDeclaredMethod("readTeacherProfile", String::class.java, AppLanguage::class.java)
                    .invoke(client, id, language) as TeacherProfile
            }
        }
        val loadPhoto: suspend (String) -> ByteArray? = { null }
        val repositoryType = Class.forName("com.kpyruy.takt.core.data.CachedCourseTeachingRepository")
        val repository = repositoryType.declaredConstructors.single { it.parameterCount == 4 }
            .newInstance(loadTeam, loadProfile, loadPhoto, com.kpyruy.takt.core.data.TeachingStore()) as CourseTeachingRepository
        try {
            showSubject(replay = repository)
            compose.waitUntil(8_000) { compose.onAllNodesWithTag("course-teacher-71659").fetchSemanticsNodes().isNotEmpty() }
            for (teacher in team) compose.onNodeWithTag("course-teacher-${teacher.personId}").performScrollTo().assertIsDisplayed()
            capture("corrected-current-team.png")
            compose.onNodeWithTag("course-teacher-2758").performClick()
            compose.onNodeWithTag("teacher-email-uis").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("vrabelr@is.stuba.sk").assertIsDisplayed()
            compose.onNodeWithTag("teacher-office").performScrollTo()
            compose.onNodeWithText("T02 3.110").assertIsDisplayed()
            compose.onNodeWithTag("teacher-hours").performScrollTo()
            compose.onNodeWithText("štvrtok 8.00-10.00 (prípadne iný termín po dohode)").assertIsDisplayed()
            capture("corrected-vrabel-office.png")
            assertTrue(requests.contains("/auth/katalog/syllabus.pl?predmet=441254;lang=en"))
            assertFalse(requests.any { it.contains("predmet=441154") })
        } finally { clientType.getDeclaredMethod("close").invoke(client); server.shutdown() }
    }

    private fun capture(name: String) {
        compose.waitForIdle(); android.os.SystemClock.sleep(80)
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        compose.activity.openFileOutput(name, 0).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
