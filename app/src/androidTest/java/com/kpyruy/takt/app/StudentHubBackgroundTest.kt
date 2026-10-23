package com.kpyruy.takt.app

import androidx.test.platform.app.InstrumentationRegistry
import com.kpyruy.takt.core.data.InMemoryStudentHubStore
import com.kpyruy.takt.core.data.UniversityAccountRepository
import com.kpyruy.takt.core.model.viewedStudy
import com.kpyruy.takt.core.data.uis.UisResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** Real account/session coordination against a private synthetic HTTPS UIS, without opening Studies. */
class StudentHubBackgroundTest {
    @Test fun connectedImportWarmsCacheWithoutWaitingForHubAndCoalescesForegroundEntry() = runBlocking {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val portalRequests = AtomicInteger()
        val server = MockWebServer()
        val certificate = HeldCertificate.Builder().commonName("localhost").addSubjectAlternativeName("localhost").build()
        val serverTls = HandshakeCertificates.Builder().heldCertificate(certificate).build()
        val clientTls = HandshakeCertificates.Builder().addTrustedCertificate(certificate.certificate).build()
        server.useHttps(serverTls.sslSocketFactory(), false)
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val url = request.path.orEmpty()
                val html = when {
                    url.contains("moje_studium.pl") -> {
                        portalRequests.incrementAndGet(); entered.countDown()
                        check(release.await(5, TimeUnit.SECONDS))
                        """<div id=log>Logged in: Synthetic Student</div><table><tr><th>Choose</th><th>Faculty</th><th>Study</th><th>Progress of study</th></tr><tr><td>Active</td><td>MTF</td><td>Automation</td><td>enrolled<a href="studium.pl?studium=101;obdobi=741">Details</a></td></tr></table>"""
                    }
                    url.contains("/studium.pl") && url.contains("lang=sk") -> """<table><tr><td>Študijný program:</td><td>Automatizácia</td></tr></table>"""
                    url.contains("/studium.pl") -> """<table><tr><td>Programme:</td><td>Automation</td></tr><tr><td>Beginning of study:</td><td>09/01/2025</td></tr></table>"""
                    url.contains("studijni_povinnosti.pl") -> """<form name=formular><input name=obdobi value=741><select name=studium><option value=101 selected>Study [term 1, year 2]</option></select></form><table id=tmtab_1><tr><td colspan=6>3rd semester</td></tr><tr><td><a href="/auth/katalog/syllabus.pl?predmet=500">PHYS</a></td><td>Physics</td><td>exam</td><td>5</td><td>1x</td><td>FULFILLED</td></tr></table>"""
                    url.contains("pruchod_studiem.pl") -> """<table><tr><th>Code</th><th>Course name</th><th>Result</th><th>Entered</th></tr><tr><td>PHYS</td><td>Physics</td><td>(A)</td><td></td></tr></table>"""
                    url.contains("financovani.pl") -> """<table><tr><th>Status</th><th>Type</th><th>Sum</th><th>Settled</th></tr><tr><td colspan=4>No suitable data found</td></tr></table>"""
                    url.contains("vypl_stip_student.pl") -> "No scholarships have been paid to you for the selected study."
                    else -> error("Unexpected fixture request")
                }
                return MockResponse().setBody("<a href='/auth/system/logout.pl'>Logout</a>" + html)
            }
        }
        server.start()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = InMemoryStudentHubStore()
        val repository = UniversityAccountRepository(context, studentHubStore = store)
        val clientType = Class.forName("com.kpyruy.takt.core.data.uis.UisClient")
        val client = clientType.getDeclaredConstructor(HttpUrl::class.java).newInstance(server.url("/"))
        val transportField = clientType.getDeclaredField("http").apply { isAccessible = true }
        val transport = transportField.get(client) as OkHttpClient
        transport.cookieJar.saveFromResponse(server.url("/"), listOf(Cookie.Builder().name("UISAuth").value("fixture")
            .domain("localhost").path("/").build()))
        transportField.set(client, transport.newBuilder().sslSocketFactory(clientTls.sslSocketFactory(), clientTls.trustManager).build())
        fun field(owner: Any, name: String) = owner.javaClass.getDeclaredField(name).apply { isAccessible = true }
        field(repository.session, "client").set(repository.session, client)
        @Suppress("UNCHECKED_CAST")
        (field(repository.session, "mutableState").get(repository.session) as MutableStateFlow<UisResult>).value = UisResult.CONNECTED
        @Suppress("UNCHECKED_CAST")
        (field(repository, "activeState").get(repository) as MutableStateFlow<Boolean>).value = true
        val preferences = field(repository, "preferences").get(repository) as android.content.SharedPreferences
        preferences.edit().putString("student_owner_key", "fixture-owner").commit()
        try {
            withTimeout(3_000) { repository.retryImport() }
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            assertNull(store.read()) // Import returned while the independent hub response is still pending.
            repository.refreshStudentHubInBackground()
            assertEquals(1, portalRequests.get())
            release.countDown()
            val loaded = try { withTimeout(10_000) { repository.studentHubRepository.state.first {
                !it.loading && it.snapshot?.viewedStudy()?.scholarshipsLoaded == true
            } }.snapshot!! } catch (error: Exception) {
                throw AssertionError("Hub state=${repository.studentHubRepository.state.value}; requests=${server.requestCount}", error)
            }
            assertEquals("Automatizácia", loaded.studies.single().titleSk)
            assertEquals("Automation", loaded.studies.single().titleEn)
            assertTrue(loaded.studies.single().feesLoaded)
            assertEquals(1, portalRequests.get())
        } finally {
            release.countDown()
            repository.remove()
            server.shutdown()
        }
    }
}
