package com.kpyruy.takt.core.data.uis

import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class UisSessionCancellationTest {
    private val server = MockWebServer().apply { start() }
    private val authenticated = "<a href='/system/logout.pl'>Logout</a>"
    private val session = UisSession()

    @After fun close() { session.disconnect(); server.shutdown() }

    @Suppress("UNCHECKED_CAST")
    private fun connect() {
        val client = UisClient(server.url("/"))
        server.enqueue(MockResponse().setBody("<form method='post' action='/system/login.pl'><input name='credential_1'></form>"))
        server.enqueue(MockResponse().setBody("""{"need2FA":"false"}"""))
        server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", "/auth/")
            .setHeader("Set-Cookie", "UISAuth=synthetic; Path=/"))
        server.enqueue(MockResponse().setBody(authenticated))
        assertEquals(UisResult.CONNECTED, client.login("synthetic", "synthetic"))
        repeat(4) { server.takeRequest() }
        UisSession::class.java.getDeclaredField("client").apply { isAccessible = true }.set(session, client)
        val state = UisSession::class.java.getDeclaredField("mutableState").apply { isAccessible = true }
            .get(session) as MutableStateFlow<UisResult>
        state.value = UisResult.CONNECTED
    }

    @Test fun cancellingSessionCheckClosesSocketBeforeReturningAndDiscardsSession() = runBlocking {
        connect()
        server.enqueue(MockResponse().setBody(authenticated).setBodyDelay(2, TimeUnit.SECONDS))
        val check = launch(Dispatchers.Default) { session.check() }
        assertNotNull(withContext(Dispatchers.IO) { server.takeRequest(1, TimeUnit.SECONDS) })
        val elapsed = measureTimeMillis { check.cancelAndJoin() }
        assertTrue("Cancellation waited $elapsed ms for the blocking response", elapsed < 1_000)
        assertEquals(UisResult.DISCONNECTED, session.state.value)
    }

    @Test fun cancellingStudyPlanStopsBeforeItsSlovakFollowUpRequest() = runBlocking {
        connect()
        val plan = authenticated + """<form name='formular'><input name='obdobi' value='741'>
            <select name='studium'><option value='123' selected>Study</option></select></form>
            <table id='tmtab_1'><tr><td colspan='6'>1st semester</td></tr>
            <tr><td><a href='/auth/katalog/syllabus.pl?predmet=101'>CODE_6B</a></td><td>Course</td>
            <td>exam</td><td>5</td><td>1x</td><td>ENROLLED</td></tr></table>"""
        server.enqueue(MockResponse().setBody(plan).setBodyDelay(2, TimeUnit.SECONDS))
        server.enqueue(MockResponse().setBody(plan))
        val read = launch(Dispatchers.Default) { session.readStudyPlan() }
        assertNotNull(withContext(Dispatchers.IO) { server.takeRequest(1, TimeUnit.SECONDS) })
        val elapsed = measureTimeMillis { read.cancelAndJoin() }
        assertTrue("Cancellation waited $elapsed ms for the blocking response", elapsed < 1_000)
        assertNull(withContext(Dispatchers.IO) { server.takeRequest(150, TimeUnit.MILLISECONDS) })
        assertEquals(UisResult.CONNECTED, session.state.value)
        session.check()
        assertEquals(UisResult.CONNECTED, session.state.value)
    }

    @Test fun sessionReadsAreSerializedWhileAnExistingRequestOwnsTheCookieJar() = runBlocking {
        connect()
        server.enqueue(MockResponse().setBody(authenticated).setBodyDelay(500, TimeUnit.MILLISECONDS))
        server.enqueue(MockResponse().setBody(authenticated))
        val first = launch(Dispatchers.Default) { session.check() }
        assertNotNull(withContext(Dispatchers.IO) { server.takeRequest(1, TimeUnit.SECONDS) })
        val second = launch(Dispatchers.Default) { session.check() }
        assertNull(withContext(Dispatchers.IO) { server.takeRequest(150, TimeUnit.MILLISECONDS) })
        first.join(); second.join()
        assertNotNull(server.takeRequest(1, TimeUnit.SECONDS))
        assertEquals(UisResult.CONNECTED, session.state.value)
    }
}
