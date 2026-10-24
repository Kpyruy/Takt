package com.kpyruy.takt.app

import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.kpyruy.takt.core.data.*
import com.kpyruy.takt.core.model.*
import com.kpyruy.takt.core.ui.components.TaktGlassBackground
import com.kpyruy.takt.core.ui.i18n.TaktI18n
import com.kpyruy.takt.core.ui.theme.TaktTheme
import com.kpyruy.takt.feature.settings.SettingsScreen
import com.kpyruy.takt.feature.subjects.SubjectDetailScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import okhttp3.mockwebserver.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.OutputStream

/** Opt-in, emulator-only fixture: full production parsing/cache/transport followed by real SAF storage. */
class CourseDocumentsUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val data get() = (compose.activity.application as TaktApplication).dataContainer
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    @Test fun listOnlyActiveCoursesRequireDocumentsFolderThenSaveExactPdfAndRefreshItsContents() {
        runBlocking {
            data.seedLiquidGlassReview()
            data.settingsRepository.setLanguage(AppLanguage.SLOVAK)
            data.database.courseDao().insertAll(data.database.courseDao().getAllSnapshot().map {
                if (it.id == "glass-physics") it.copy(syllabusUrl = "https://is.stuba.sk/auth/katalog/syllabus.pl?predmet=441250") else it
            })
        }
        val server = MockWebServer()
        val requests = java.util.Collections.synchronizedList(mutableListOf<String>())
        val downloads = java.util.concurrent.atomic.AtomicInteger()
        val assets = instrumentation.context.assets
        val pdf = assets.open("course-document.pdf").use { it.readBytes() }
        // Failed earlier runs can leave this exact synthetic attachment in the review tree.
        // Start the download-count scenario without an already-saved fixture to refresh.
        runBlocking {
            if (data.documentStore.isConnected) data.documentStore.downloadedDocuments("FYZI_6B")
                .filter { it.fileName == "lecture.pdf" }.forEach { local ->
                    val bytes = compose.activity.contentResolver.openInputStream(local.uri)!!.use { it.readBytes() }
                    if (bytes.contentEquals(pdf) || bytes.contentEquals(pdf + "\n% revision 2\n".toByteArray()))
                        android.provider.DocumentsContract.deleteDocument(compose.activity.contentResolver, local.uri)
                }
        }
        var revision = 1
        fun currentBytes() = if (revision == 1) pdf else pdf + "\n% revision 2\n".toByteArray()
        fun fixture(name: String) = assets.open(name).bufferedReader().use { it.readText() }
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path.orEmpty(); requests.add(path)
                if (path == "/auth/dok_server/slozka.pl?id=287891;download=319089")
                    return MockResponse().setResponseCode(302).setHeader("Location", server.url("/auth/dok_server/./slozka.pl?id=287891;download=319089;z=1"))
                if (path == "/auth/dok_server/slozka.pl?id=287891;download=319089;z=1") {
                    downloads.incrementAndGet()
                    return MockResponse().setHeader("Content-Type", "application/pdf")
                        .setHeader("Content-Disposition", "attachment; filename=lecture.pdf")
                        .setBody(okio.Buffer().write(currentBytes()))
                }
                val html = when (path) {
                    "/auth/dok_server/?lang=en" -> fixture("root.html")
                    "/auth/dok_server/slozka.pl?ds=1;id=287891;lang=en" -> fixture("physics.html")
                    else -> return MockResponse().setResponseCode(400)
                }
                return MockResponse().setBody("<a href='/system/logout.pl'>Logout</a>" + html)
            }
        }
        val cert = okhttp3.tls.HeldCertificate.Builder().commonName("localhost").addSubjectAlternativeName("localhost").build()
        val serverCerts = okhttp3.tls.HandshakeCertificates.Builder().heldCertificate(cert).build()
        val clientCerts = okhttp3.tls.HandshakeCertificates.Builder().addTrustedCertificate(cert.certificate).build()
        server.useHttps(serverCerts.sslSocketFactory(), false); server.start()
        val clientType = Class.forName("com.kpyruy.takt.core.data.uis.UisClient")
        val client = clientType.getDeclaredConstructor(okhttp3.HttpUrl::class.java).newInstance(server.url("/"))
        val transportField = clientType.getDeclaredField("http").apply { isAccessible = true }
        val transport = transportField.get(client) as okhttp3.OkHttpClient
        transport.cookieJar.saveFromResponse(server.url("/"), listOf(okhttp3.Cookie.Builder()
            .name("UISAuth").value("local-fixture").domain(server.url("/").host).path("/").build()))
        transportField.set(client, transport.newBuilder().sslSocketFactory(clientCerts.sslSocketFactory(), clientCerts.trustManager).build())
        val load: suspend (Course, Set<String>, Boolean) -> CourseDocumentListing = { course, codes, refresh -> withContext(Dispatchers.IO) {
            clientType.getDeclaredMethod("readCourseDocuments", String::class.java, Set::class.java, Boolean::class.javaPrimitiveType)
                .invoke(client, course.code, codes, refresh) as CourseDocumentListing
        } }
        val stream: suspend (CourseDocumentFile, OutputStream) -> DownloadedCourseDocument = { doc, output -> withContext(Dispatchers.IO) {
            clientType.getDeclaredMethod("downloadCourseDocument", CourseDocumentFile::class.java, OutputStream::class.java)
                .invoke(client, doc, output) as DownloadedCourseDocument
        } }
        val repoType = Class.forName("com.kpyruy.takt.core.data.CachedCourseDocumentsRepository")
        val repository = repoType.declaredConstructors.single { it.parameterCount == 2 }.newInstance(load, stream) as CourseDocumentsRepository
        val settings = AppSettings(language = AppLanguage.SLOVAK, themeFamily = ThemeFamily.BLUE, themeMode = AppThemeMode.DARK,
            cardAppearance = CardAppearance.GLASS)
        fun subject() {
            compose.activityRule.scenario.onActivity { activity -> activity.setContent {
                TaktI18n.use(AppLanguage.SLOVAK)
                TaktTheme(settings) { TaktGlassBackground {
                    SubjectDetailScreen(data.studyPlanRepository, data.gradeRepository, data.scheduleRepository,
                        data.settingsRepository, data.studyContentRepository, data.examRepository, data.documentStore,
                        uisManaged = true, courseId = "glass-physics", onBack = {}, courseDocumentsRepository = repository)
                } }
            } }
            compose.onNodeWithTag("course-documents").performScrollTo().performClick()
            compose.waitUntil(8_000) { compose.onAllNodesWithTag("course-document-319089").fetchSemanticsNodes().isNotEmpty() }
        }
        try {
            // The list must not fetch any attachments automatically, and no fallback location is allowed.
            compose.activity.getSharedPreferences("takt_documents", 0).edit().clear().commit()
            runBlocking { data.documentStore.resume() }
            subject()
            for (id in listOf("319089", "318783", "318631", "318375", "318374", "318252"))
                compose.onNodeWithTag("course-document-$id").performScrollTo().assertIsDisplayed()
            compose.onNodeWithTag("document-download-319089").performScrollTo().performClick()
            compose.onNodeWithTag("documents-folder-required").performScrollTo().assertIsDisplayed()
            assertEquals(0, downloads.get())
            capture("documents-connect-folder.png")
            compose.activityRule.scenario.onActivity { activity -> activity.setContent {
                TaktTheme(settings) { TaktGlassBackground {
                    SettingsScreen(data.settingsRepository, data.backupRepository, data.documentStore,
                        data.studyPlanRepository, data.universityAccountRepository, { _, reply -> reply(DeviceAuthenticationResult.CANCELLED) },
                        (compose.activity.application as TaktApplication).appReleaseRepository, {}, guidedPage = "documents")
                } }
            } }
            compose.onNodeWithText("Pripojiť dokumenty").performScrollTo().performClick()
            val device = UiDevice.getInstance(instrumentation)
            val use = device.wait(Until.findObject(By.text(java.util.regex.Pattern.compile("use this folder", java.util.regex.Pattern.CASE_INSENSITIVE))), 8_000)
            assertNotNull("System folder picker should open Documents", use)
            use.click()
            val allow = device.wait(Until.findObject(By.text(java.util.regex.Pattern.compile("allow", java.util.regex.Pattern.CASE_INSENSITIVE))), 5_000)
            if (allow != null) allow.click()
            compose.waitUntil(10_000) { data.documentStore.isConnected }
            subject()
            compose.onNodeWithTag("document-download-319089").performScrollTo().performClick()
            compose.waitUntil(8_000) { downloads.get() == 1 && compose.onAllNodesWithContentDescription("Uložené").fetchSemanticsNodes().isNotEmpty() }
            compose.waitForIdle()
            assertTrue("The selected Documents tree must remain accessible", data.documentStore.isConnected)
            compose.onNodeWithTag("documents-folder-required").assertDoesNotExist()
            compose.onNodeWithTag("documents-retry").assertDoesNotExist()
            val course = runBlocking { data.studyPlanRepository.observeCourses().first().first { it.code == "FYZI_6B" } }
            val doc = runBlocking { repository.documents(course, setOf("FYZI_6B", "TPAR_6B")).files.first { it.documentId == "319089" } }
            val store = CourseDocumentDownloads(compose.activity, data.documentStore, repository.accountGeneration.value)
            val firstUri = runBlocking { store.saved(course, doc) }!!
            assertArrayEquals(pdf, compose.activity.contentResolver.openInputStream(firstUri)!!.use { it.readBytes() })
            assertTrue(firstUri.toString().contains("FYZI_6B"))
            capture("documents-physics-saved.png")
            compose.waitUntil(8_000) { compose.onAllNodesWithTag("documents-loading").fetchSemanticsNodes().isEmpty() }
            revision = 2
            compose.onNodeWithContentDescription("Aktualizovať").performClick()
            compose.waitUntil(8_000) { requests.count { it == "/auth/dok_server/slozka.pl?ds=1;id=287891;lang=en" } >= 2 }
            try {
                compose.waitUntil(8_000) { downloads.get() == 2 && compose.onAllNodesWithTag("documents-loading").fetchSemanticsNodes().isEmpty() }
            } catch (failure: Exception) {
                throw AssertionError("Refresh downloads=${downloads.get()}, requests=$requests", failure)
            }
            compose.waitForIdle()
            val secondUri = runBlocking { store.saved(course, doc) }!!
            assertEquals(firstUri, secondUri)
            assertArrayEquals(currentBytes(), compose.activity.contentResolver.openInputStream(secondUri)!!.use { it.readBytes() })
            // Refresh updates the same public file rather than keeping a duplicate version.
            assertArrayEquals(currentBytes(), compose.activity.contentResolver.openInputStream(firstUri)!!.use { it.readBytes() })
            var opened: android.content.Intent? = null
            val monitor = object : android.app.Instrumentation.ActivityMonitor() {
                override fun onStartActivity(intent: android.content.Intent): android.app.Instrumentation.ActivityResult? {
                    if (intent.action == android.content.Intent.ACTION_VIEW && intent.type == "application/pdf") {
                        opened = android.content.Intent(intent)
                        return android.app.Instrumentation.ActivityResult(android.app.Activity.RESULT_CANCELED, null)
                    }
                    return null
                }
            }
            instrumentation.addMonitor(monitor)
            try {
                compose.onNodeWithTag("course-document-319089").performScrollTo().performClick()
                compose.waitUntil(5_000) { opened != null }
                assertEquals("content", opened!!.data!!.scheme)
                assertTrue(opened!!.flags and android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
                assertArrayEquals(currentBytes(), compose.activity.contentResolver.openInputStream(opened!!.data!!)!!.use { it.readBytes() })
                assertEquals("Opening again after refresh must reuse the refreshed preview", 2, downloads.get())
            } finally { instrumentation.removeMonitor(monitor) }
            // Teaching metadata updates must reach the actual connected SAF backup without a Room/settings change.
            val savedTeaching = data.teachingStore.snapshot.value
            val teaching = BackupTeaching(
                listOf(BackupTeachingTeam("FYZI_6B", listOf(BackupTeacher("71659", "Review teacher")))),
                listOf(BackupTeacherProfile("71659", "SLOVAK", "Review teacher", consultationHours = "Monday 10:00")),
            )
            val tree = android.net.Uri.parse(compose.activity.getSharedPreferences("takt_documents", 0).getString("tree_uri", null)!!)
            val treeId = android.provider.DocumentsContract.getTreeDocumentId(tree)
            val backupId = treeId + (if (treeId.endsWith(":Documents")) "/Takt/data.json" else "/data.json")
            val backupUri = android.provider.DocumentsContract.buildDocumentUriUsingTree(tree, backupId)
            try {
                data.teachingStore.replace(teaching)
                runBlocking { data.documentStore.saveCurrentToDocuments() }
                data.teachingStore.replace(teaching.copy(profiles = teaching.profiles.map { it.copy(consultationHours = "Tuesday 12:00") }))
                compose.waitUntil(8_000) {
                    runCatching {
                        val raw = compose.activity.contentResolver.openInputStream(backupUri)!!.bufferedReader().use { it.readText() }
                        BackupPayloadCodec.decode(raw).teaching?.profiles?.single()?.consultationHours == "Tuesday 12:00"
                    }.getOrDefault(false)
                }
            } finally { data.teachingStore.replace(savedTeaching) }
            assertTrue(requests.none { it.contains("tree_config_ajax") || it.contains("oznacit") })
            repoType.getDeclaredMethod("clear").invoke(repository)
            compose.waitUntil(5_000) { compose.onAllNodesWithTag("course-documents-sheet").fetchSemanticsNodes().isEmpty() }
            runCatching { android.provider.DocumentsContract.deleteDocument(compose.activity.contentResolver, firstUri) }
            runCatching { android.provider.DocumentsContract.deleteDocument(compose.activity.contentResolver, secondUri) }
        } finally { clientType.getDeclaredMethod("close").invoke(client); server.shutdown() }
    }

    private fun capture(name: String) {
        compose.waitForIdle(); android.os.SystemClock.sleep(100)
        val image = instrumentation.uiAutomation.takeScreenshot()
        compose.activity.openFileOutput(name, 0).use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        image.recycle()
    }
}
