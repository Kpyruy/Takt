package com.kpyruy.takt.app

import android.content.Context
import android.content.ContextWrapper
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.kpyruy.takt.core.data.*
import com.kpyruy.takt.core.data.uis.UisResult
import com.kpyruy.takt.core.database.TaktDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Cookie
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.Assert.*
import org.junit.Test
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/** Real restore, settings and authenticated import boundary; only HTTPS responses are synthetic. */
class UisRestoredSemesterTest {
    @Test fun restoreBeforeUisImportKeepsDetectedSemesterWhenResultsFail() = runBlocking {
        verifyPostLogin(alreadyImported = false)
    }

    @Test fun reconnectRefreshesSemesterWithoutRepeatingTheCompletedImport() = runBlocking {
        verifyPostLogin(alreadyImported = true)
    }

    private suspend fun verifyPostLogin(alreadyImported: Boolean) {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val prefix = "restored-semester-${System.nanoTime()}"
        val context = object : ContextWrapper(app) {
            override fun getApplicationContext(): Context = this
            override fun getSharedPreferences(name: String, mode: Int) = app.getSharedPreferences("$prefix-$name", mode)
        }
        val database = Room.inMemoryDatabaseBuilder(context, TaktDatabase::class.java).build()
        val settings = SharedPreferencesAppSettingsRepository(context, prefix)
        val backup = RoomBackupRepository(database, settings)
        val studyPlan = RoomStudyPlanRepository(database, settings)
        val preferences = context.getSharedPreferences("takt_university_account", 0)
        val server = MockWebServer()
        val certificate = HeldCertificate.Builder().commonName("localhost").addSubjectAlternativeName("localhost").build()
        val serverTls = HandshakeCertificates.Builder().heldCertificate(certificate).build()
        val clientTls = HandshakeCertificates.Builder().addTrustedCertificate(certificate.certificate).build()
        server.useHttps(serverTls.sslSocketFactory(), false)
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when {
                request.path.orEmpty().contains("studijni_povinnosti.pl") -> MockResponse().setBody("""
                    <a href='/auth/system/logout.pl'>Logout</a>
                    <form name=formular><input name=obdobi value=741><select name=studium>
                    <option value=101 selected>Study [term 1, year 2]</option></select></form>
                    <table id=tmtab_1><tr><td colspan=6>5th semester</td></tr>
                    <tr><td><a href='syllabus.pl?predmet=500'>RETAKE</a></td><td>Retake</td>
                    <td>Exm</td><td>5</td><td>1x</td><td>ENROLLED</td></tr></table>
                """.trimIndent())
                else -> MockResponse().setResponseCode(503)
            }
        }
        server.start()
        val account = UniversityAccountRepository(context, studyPlan, settingsRepository = settings, backupCoordinator = backup)
        try {
            // This is a fresh-device restore: the portable backup has no locally detected UIS semester.
            backup.importJson(BackupPayloadCodec.encode(BackupPayload(
                courses = listOf(BackupCourse("restored", "RETAKE", "Retake", 5, 5, "enrolled", "COMPULSORY")),
                settings = BackupSettings(currentSemester = 5))))
            assertNull(settings.settings.value.uisCurrentSemester)
            preferences.edit().putBoolean("uis_initial_import_done", alreadyImported).commit()
            val clientType = Class.forName("com.kpyruy.takt.core.data.uis.UisClient")
            val client = clientType.getDeclaredConstructor(HttpUrl::class.java).newInstance(server.url("/"))
            val transportField = clientType.getDeclaredField("http").apply { isAccessible = true }
            val transport = transportField.get(client) as OkHttpClient
            transport.cookieJar.saveFromResponse(server.url("/"), listOf(Cookie.Builder().name("UISAuth")
                .value("fixture").domain("localhost").path("/").build()))
            transportField.set(client, transport.newBuilder().sslSocketFactory(clientTls.sslSocketFactory(), clientTls.trustManager).build())
            account.session.javaClass.getDeclaredField("client").apply { isAccessible = true }.set(account.session, client)
            @Suppress("UNCHECKED_CAST")
            (account.session.javaClass.getDeclaredField("mutableState").apply { isAccessible = true }
                .get(account.session) as MutableStateFlow<UisResult>).value = UisResult.CONNECTED
            // Enter the exact post-authentication branch used by signIn/submitCode, without device credentials.
            suspendCoroutine<Unit> { continuation ->
                val method = account.javaClass.getDeclaredMethod("importAfterLogin", Boolean::class.javaPrimitiveType, Continuation::class.java)
                    .apply { isAccessible = true }
                if (method.invoke(account, false, continuation) !== COROUTINE_SUSPENDED) continuation.resume(Unit)
            }
            assertEquals("UIS term/year must be recorded independently of results/import completion", 3, settings.settings.value.uisCurrentSemester)
            assertEquals(3, settings.settings.value.effectiveCurrentSemester(studyPlan.observeCourses().first()))
            assertNull(settings.settings.value.currentSemester)
            assertFalse(account.importProgress.value.running)
            assertEquals(if (alreadyImported) 2 else 3, server.requestCount)
            assertEquals("Restored courses must survive a failed or already completed import", listOf("restored"), studyPlan.observeCourses().first().map { it.id })
        } finally {
            account.session.disconnect()
            server.shutdown()
            database.close()
            preferences.edit().clear().commit()
        }
    }
}
