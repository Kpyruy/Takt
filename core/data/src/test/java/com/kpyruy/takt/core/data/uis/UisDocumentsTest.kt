package com.kpyruy.takt.core.data.uis

import com.kpyruy.takt.core.model.CourseDocumentListing
import com.kpyruy.takt.core.model.CourseDocumentFile
import com.kpyruy.takt.core.model.CourseDocumentFolder
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.launch

class UisDocumentsTest {
    private val server = MockWebServer().apply { start() }
    private val client = UisClient(server.url("/"))
    private val authenticated = """<a href="/system/logout.pl">Logout</a>"""
    @After fun close() { client.close(); server.shutdown() }
    private fun page(body: String) = MockResponse().setBody(authenticated + body)
    private fun connect() {
        server.enqueue(MockResponse().setBody("""<form method="post" action="/system/login.pl"><input name="credential_0"><input name="credential_1" type="password"></form>"""))
        server.enqueue(MockResponse().setBody("""{"need2FA":"false"}"""))
        server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", "/auth/").addHeader("Set-Cookie", "UISAuth=fake; Path=/"))
        server.enqueue(page(""))
        assertEquals(UisResult.CONNECTED, client.login("student", "password"))
        repeat(4) { server.takeRequest() }
    }
    private fun resource(name: String) = javaClass.getResource("/documents/$name")!!.readText()

    @Test fun documentReadPageProvidesActualAttachmentNameWithoutKeepingViewTokens() {
        val document = CourseDocumentFile("10", "20", "Lecture title", "application/pdf", "Lecture title.pdf")
        val parsed = UisDocumentsParser.documentDetails("""
            <table><tr><td><b>Name:</b></td><td>Lecture title</td></tr>
            <tr><td><b>Attachment:</b></td><td>lecture_original.pdf [ <span data-mime="pdf"></span>
            document PDF, 1.81 MiB]<input type=submit name=download value=Download></td></tr></table>
        """.trimIndent(), document)
        assertEquals("lecture_original.pdf", parsed.fileName)
        assertEquals(document.documentId, parsed.documentId)
        assertEquals(document.title, parsed.title)
        assertTrue(runCatching { UisDocumentsParser.documentDetails("<title>Login</title>", document) }.isFailure)
    }

    @Test fun documentMetadataUsesOnlyTheExpectedReadPageAndIdentity() {
        connect()
        server.enqueue(page("""<table><tr><td>Name:</td><td>Lecture</td></tr>
            <tr><td>Attachment:</td><td>original.pdf [ <span data-mime=pdf></span> document PDF]</td></tr></table>"""))
        val file = client.readCourseDocumentDetails(CourseDocumentFile("10", "20", "Lecture", "application/pdf"))
        assertEquals("original.pdf", file.fileName)
        assertEquals("/auth/dok_server/dokumenty_cteni.pl?id=10;dok=20;on=0;lang=en", server.takeRequest().path)
        server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", "/auth/dok_server/dokumenty_cteni.pl?id=10;dok=99"))
        assertTrue(runCatching { client.readCourseDocumentDetails(file) }.isFailure)
    }

    @Test fun folderReadLinkKeepsItsSignatureOnlyInEphemeralMetadata() {
        val origin = server.url("/auth/dok_server/")
        val url = "dokumenty_cteni.pl?id=10;on=0;dok=20;serializace=synthetic-token"
        val folder = UisDocumentsParser.folder("""<table><tr><th>Name</th><th>Attachment</th><th>Display</th></tr>
            <tr><td>Lecture</td><td><a href="slozka.pl?id=10;download=20"><span data-mime=pdf></span></a></td>
            <td><a href="$url">Read</a></td></tr></table>""", UisDocumentsParser.Folder("10", "DOC", "DOC"), origin)
        assertEquals("synthetic-token", folder.readLinks.getValue("20").query!!.substringAfter("serializace="))
        assertFalse(folder.files.single().toString().contains("synthetic-token"))
        assertNull(UisDocumentsParser.readLink(url.replace("dok=20", "dok=21"), "10", "20", origin))
        assertNull(UisDocumentsParser.readLink("https://elsewhere.example/auth/dok_server/$url", "10", "20", origin))
    }

    @Test fun productionClientDiscoversHiddenExactCourseNodesAndSixRealPdfRows() {
        connect()
        server.enqueue(page(resource("root.html")))
        server.enqueue(page(resource("physics.html")))
        val listing = client.readCourseDocuments("FYZI_6B", setOf("FYZI_6B", "TPAR_6B"))
        assertTrue(listing.foldersFound)
        assertEquals(6, listing.files.size)
        assertEquals("Prednáška 4: Základy mechaniky tuhého telesa", listing.files.first().title)
        assertEquals("Prednáška 4: Základy mechaniky tuhého telesa.pdf", listing.files.first().fileName)
        assertEquals("application/pdf", listing.files.first().mimeType)
        assertEquals(listOf("319089", "318783", "318631", "318375", "318374", "318252"), listing.files.map { it.documentId })
        assertEquals("/auth/dok_server/?lang=en", server.takeRequest().path)
        val folder = server.takeRequest()
        assertEquals("GET", folder.method)
        assertEquals("/auth/dok_server/slozka.pl?ds=1;id=287891;lang=en", folder.path)
        assertEquals(6, server.requestCount)
        assertFalse(client.readCourseDocuments("OTHER_6B", setOf("FYZI_6B")).foldersFound)
        assertEquals(6, server.requestCount)
    }

    @Test fun duplicateCourseFoldersKeepTheirPathsAndRejectExternalOrAmbiguousIds() {
        val roots = UisDocumentsParser.courseFolders("""<ul>
            <li><div class="node-container"><div class="node-label"><a href="slozka.pl?id=11">WS 2026</a></div></div><ul style="display:none"><li><div class="node-container"><div class="node-label"><a href="slozka.pl?id=12">FYZI_6B Physics</a></div></div></li></ul></li>
            <li><div class="node-container"><div class="node-label"><a href="slozka.pl?id=13">FYZI_6B Old</a></div></div></li>
            <li><a href="slozka.pl?id=14">FYZI_6B_EXTRA Physics</a></li>
            <li><a href="https://elsewhere.example/auth/dok_server/slozka.pl?id=15">FYZI_6B Physics</a></li>
            <li><a href="slozka.pl?id=16;id=17">FYZI_6B Physics</a></li>
            </ul>""", setOf("FYZI_6B"))
        assertEquals(listOf("12", "13"), roots.map { it.id })
        assertTrue(roots.first().path.contains("WS 2026"))
    }

    @Test fun followsOnlyDocumentSubfolderTablesAndPaginationThenBoundsTraversal() {
        connect()
        server.enqueue(page("""<a href="slozka.pl?id=10">FYZI_6B Physics</a>"""))
        server.enqueue(page("""<nav><a href="slozka.pl?id=999">Faculty</a></nav>
            <table><tr><th>Name</th><th>Enter folder</th></tr><tr><td>Lectures</td><td><a href="slozka.pl?id=20">Enter</a></td></tr></table>
            <a rel="next" href="slozka.pl?id=10;strana=2">Next</a>"""))
        server.enqueue(page("""<table><tr><th>Name</th><th>Attachment</th></tr><tr><td>Lecture</td><td><a href="slozka.pl?id=20;download=30"><span data-mime="pdf"></span></a></td></tr></table>"""))
        server.enqueue(page("""<table><tr><th>Name</th><th>Attachment</th></tr><tr><td>Second page</td><td><a href="slozka.pl?id=10;download=31"><span data-mime="pdf"></span></a></td></tr></table>"""))
        val listing = client.readCourseDocuments("FYZI_6B", setOf("FYZI_6B"))
        assertEquals(setOf("30", "31"), listing.files.map { it.documentId }.toSet())
        repeat(4) { assertFalse(server.takeRequest().path!!.contains("999")) }
        assertFalse(listing.truncated)
    }

    private fun emptyFolder() = """<table><tr><th>Name</th><th>Attachment</th></tr></table>"""
    private fun fileFolder(folderId: String, documentId: String) = """<table><tr><th>Name</th><th>Attachment</th></tr>
        <tr><td>Lecture</td><td><a href="slozka.pl?id=$folderId;download=$documentId"><span data-mime="pdf"></span></a></td></tr></table>"""

    @Test fun discoveredRootsCanBeReusedAfterClientRestartAndOnRefresh() {
        connect()
        server.enqueue(page("""<a href="slozka.pl?id=10">FYZI_6B Physics</a>"""))
        server.enqueue(page(fileFolder("10", "20")))
        val discovered = client.readCourseDocuments("FYZI_6B", setOf("FYZI_6B"))
        repeat(2) { server.takeRequest() }
        assertEquals(listOf("10"), discovered.roots.map { it.id })
        client.close()
        val restarted = UisClient(server.url("/"))
        try {
            // Establish a fresh authenticated transport, without copying any in-memory folder index.
            server.enqueue(MockResponse().setBody("""<form method="post" action="/system/login.pl"><input name="credential_0"><input name="credential_1" type="password"></form>"""))
            server.enqueue(MockResponse().setBody("""{"need2FA":"false"}"""))
            server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", "/auth/").addHeader("Set-Cookie", "UISAuth=fake; Path=/"))
            server.enqueue(page(""))
            assertEquals(UisResult.CONNECTED, restarted.login("student", "password"))
            repeat(4) { server.takeRequest() }
            for (refresh in listOf(false, true)) {
                server.enqueue(page(fileFolder("10", "21")))
                val loaded = restarted.readCourseDocuments("FYZI_6B", setOf("FYZI_6B"), refresh, discovered.roots)
                assertEquals("21", loaded.files.single().documentId)
                assertEquals(discovered.roots, loaded.roots)
                assertEquals("/auth/dok_server/slozka.pl?ds=1;id=10;lang=en", server.takeRequest().path)
            }
            assertEquals(12, server.requestCount)
        } finally { restarted.close() }
    }

    @Test fun cachedRootFallsBackOnlyWhenGoneUnknownOrRedirectedElsewhere() {
        connect()
        val hint = listOf(CourseDocumentFolder("10", "FYZI_6B Old", "Old semester / FYZI_6B Old"))
        val staleResponses = listOf(
            MockResponse().setResponseCode(404), MockResponse().setResponseCode(410),
            page("<p>The requested folder does not exist.</p>"),
            MockResponse().setResponseCode(302).setHeader("Location", "/auth/dok_server/slozka.pl?id=999"),
            MockResponse().setResponseCode(302).setHeader("Location", "https://elsewhere.example/auth/dok_server/slozka.pl?id=10"),
        )
        for (stale in staleResponses) {
            server.enqueue(stale)
            server.enqueue(page("""<a href="slozka.pl?id=11">FYZI_6B Physics</a>"""))
            server.enqueue(page(fileFolder("11", "22")))
            val loaded = client.readCourseDocuments("FYZI_6B", setOf("FYZI_6B"), true, hint)
            assertEquals("22", loaded.files.single().documentId)
            assertEquals(listOf("11"), loaded.roots.map { it.id })
            assertTrue(server.takeRequest().path!!.contains("id=10"))
            assertEquals("/auth/dok_server/?lang=en", server.takeRequest().path)
            assertTrue(server.takeRequest().path!!.contains("id=11"))
        }
        assertEquals(4 + staleResponses.size * 3, server.requestCount)
    }

    @Test fun recognizedEmptyCachedFolderIsValidAndNeverRescansRoot() {
        connect()
        val roots = listOf(CourseDocumentFolder("10", "FYZI_6B Physics", "Physics"))
        for (body in listOf(emptyFolder(), """<table><tr><th>Názov</th><th>Vstup do zložky</th></tr></table>""")) {
            server.enqueue(page(body))
            val loaded = client.readCourseDocuments("FYZI_6B", setOf("FYZI_6B"), true, roots)
            assertTrue(loaded.foldersFound)
            assertTrue(loaded.files.isEmpty())
            assertEquals(roots, loaded.roots)
            assertTrue(server.takeRequest().path!!.contains("id=10"))
        }
        assertEquals(6, server.requestCount)
    }

    @Test fun discoveryRetainsEverySemesterRootAndReusesEachOfThem() {
        connect()
        server.enqueue(page("""<a href="slozka.pl?id=10">FYZI_6B Current</a><a href="slozka.pl?id=11">FYZI_6B Previous</a>"""))
        server.enqueue(page(fileFolder("10", "20")))
        server.enqueue(page(fileFolder("11", "21")))
        val discovered = client.readCourseDocuments("FYZI_6B", setOf("FYZI_6B"))
        assertEquals(listOf("10", "11"), discovered.roots.map { it.id })
        assertNull(discovered.folderUrl)
        repeat(3) { server.takeRequest() }
        server.enqueue(page(emptyFolder()))
        server.enqueue(page(emptyFolder()))
        val refreshed = client.readCourseDocuments("FYZI_6B", setOf("FYZI_6B"), true, discovered.roots)
        assertEquals(discovered.roots, refreshed.roots)
        assertTrue(server.takeRequest().path!!.contains("id=10"))
        assertTrue(server.takeRequest().path!!.contains("id=11"))
        assertEquals(9, server.requestCount)
    }

    @Test fun cachedRootPreservesSubfolderPaginationAndRejectsUnrelatedNavigation() {
        connect()
        server.enqueue(page("""<nav><a href="slozka.pl?id=999">Faculty</a></nav>
            <table><tr><th>Name</th><th>Enter folder</th></tr><tr><td>Lectures</td><td><a href="slozka.pl?id=11">Enter</a></td></tr></table>
            <a href="slozka.pl?id=10;strana=2">Next</a>"""))
        server.enqueue(page(fileFolder("11", "20")))
        server.enqueue(page(fileFolder("10", "21")))
        val listing = client.readCourseDocuments("FYZI_6B", setOf("FYZI_6B"), true,
            listOf(CourseDocumentFolder("10", "FYZI_6B Physics", "Physics")))
        assertEquals(setOf("20", "21"), listing.files.map { it.documentId }.toSet())
        assertFalse(listing.truncated)
        repeat(3) { assertFalse(server.takeRequest().path!!.contains("999")) }
        assertEquals(7, server.requestCount)
    }

    @Test fun cachedAuthenticationAndHttpFailuresPropagateWithoutRescanning() {
        connect()
        val roots = listOf(CourseDocumentFolder("10", "FYZI_6B Physics", "Physics"))
        server.enqueue(MockResponse().setBody("<form><input name=credential_1></form>"))
        assertTrue(runCatching { client.readCourseDocuments("FYZI_6B", setOf("FYZI_6B"), false, roots) }
            .exceptionOrNull() is com.kpyruy.takt.core.data.TeachingAuthenticationRequired)
        server.takeRequest()
        for (statusCode in listOf(401, 403, 500)) {
            server.enqueue(MockResponse().setResponseCode(statusCode))
            assertTrue(runCatching { client.readCourseDocuments("FYZI_6B", setOf("FYZI_6B"), false, roots) }.isFailure)
            assertTrue(server.takeRequest().path!!.contains("id=10"))
        }
        assertEquals(8, server.requestCount)
    }

    @Test fun cachedNetworkFailurePropagatesWithoutRootSearch() {
        connect()
        server.enqueue(MockResponse().setBody(authenticated + "x".repeat(8192)).setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY))
        val failure = runCatching { client.readCourseDocuments("FYZI_6B", setOf("FYZI_6B"), false,
            listOf(CourseDocumentFolder("10", "FYZI_6B Physics", "Physics"))) }.exceptionOrNull()
        assertTrue("Expected IO failure, got $failure", failure is java.io.IOException)
        assertEquals(5, server.requestCount)
    }

    @Test fun folderMetadataRejectsInvalidIdsBeforeTheyCanBecomeRequests() {
        for (id in listOf("", "0", "-1", "10;delete=1", "https://elsewhere.example")) {
            assertTrue(runCatching { CourseDocumentFolder(id, "Physics", "Physics") }.isFailure)
        }
    }

    @Test fun listingRevisionDetectsAnEditWithoutDownloadingAnUnsavedFile() {
        fun parsed(date: String) = UisDocumentsParser.folder("""<table><tr><th>Name</th><th>Modifications</th><th>Attachment</th></tr>
            <tr><td>Lecture</td><td>$date</td><td><a href="slozka.pl?id=10;download=20"><span data-mime="pdf"></span></a></td></tr></table>""",
            UisDocumentsParser.Folder("10","Physics","Physics")).files.single()
        val before = parsed("20.10.2026"); val after = parsed("21.10.2026")
        assertNotEquals(before.revision,after.revision)
        val changes = com.kpyruy.takt.core.model.CourseDocumentChanges.between(
            CourseDocumentListing(listOf(before),true),CourseDocumentListing(listOf(after),true))
        assertEquals(listOf(after),changes.updated)
        assertEquals(0,server.requestCount)
    }

    @Test fun followsTheSuppliedSameAttachmentRedirectAndStreamsTheActualWordDocument() {
        connect()
        val bytes = javaClass.getResourceAsStream("/documents/Laboratorne_cvicenie.doc")!!.readBytes()
        assertEquals(39936, bytes.size)
        server.enqueue(MockResponse().setResponseCode(302).setHeader("Location",
            server.url("/auth/dok_server/./slozka.pl?id=288920;download=318624;z=1")))
        server.enqueue(MockResponse().setHeader("Content-Type", "application/msword")
            .setHeader("Content-Disposition", "attachment; filename=\"Laboratorne_cvicenie.doc\"")
            .setBody(okio.Buffer().write(bytes)))
        val output = ByteArrayOutputStream()
        val actual = client.downloadCourseDocument(CourseDocumentFile("288920", "318624", "Laboratórne cvičenie", "application/msword"), output)
        assertArrayEquals(bytes, output.toByteArray())
        assertEquals("Laboratorne_cvicenie.doc", actual.fileName)
        assertEquals("application/msword", actual.mimeType)
        assertEquals(39936L, actual.byteCount)
        assertEquals("/auth/dok_server/slozka.pl?id=288920;download=318624", server.takeRequest().path)
        assertEquals("/auth/dok_server/slozka.pl?id=288920;download=318624;z=1", server.takeRequest().path)
    }

    @Test fun redirectFlagCannotEnableFolderActionsOrUnknownValues() {
        val base = server.url("/auth/dok_server/")
        for (query in listOf("id=10;z=1", "id=10;download=20;z=0", "id=10;download=20;z=2",
            "id=10;download=20;z=1;z=1", "id=10;download=20;z=1;delete=1")) {
            assertNull(query, UisDocumentsParser.link("slozka.pl?$query", base))
        }
    }

    @Test fun streamsBinaryAndUsesUtf8DispositionFilename() {
        connect()
        server.enqueue(MockResponse().setHeader("Content-Type", "application/pdf")
            .setHeader("Content-Disposition", "attachment; filename*=UTF-8''predn%C3%A1%C5%A1ka.pdf")
            .setBody("%PDF-1.7\nexample"))
        val out = ByteArrayOutputStream()
        val result = client.downloadCourseDocument(CourseDocumentFile("10", "20", "Lecture", "application/pdf"), out)
        assertEquals("prednáška.pdf", result.fileName)
        assertEquals("%PDF-1.7\nexample", out.toString("UTF-8"))
        assertEquals(16L, result.byteCount)
        assertEquals("/auth/dok_server/slozka.pl?id=10;download=20", server.takeRequest().path)
    }

    @Test fun rejectsLoginHtmlAndUnsafeRedirectBeforeWritingAnyBytes() {
        connect()
        val document = CourseDocumentFile("10", "20", "Lecture", "application/pdf")
        server.enqueue(MockResponse().setHeader("Content-Type", "application/pdf").setBody("<!DOCTYPE html><html><input name=credential_1></html>"))
        val out = ByteArrayOutputStream()
        assertTrue(runCatching { client.downloadCourseDocument(document, out) }.isFailure)
        assertEquals(0, out.size())
        server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", "https://elsewhere.example/file.pdf"))
        assertTrue(runCatching { client.downloadCourseDocument(document, out) }.isFailure)
        assertEquals(0, out.size())
    }

    @Test fun traversalCapReportsTruncationAndNeverRequestsTheNextPage() {
        connect()
        server.enqueue(page("""<a href="slozka.pl?id=10">FYZI_6B Physics</a>"""))
        repeat(40) { n -> server.enqueue(page("""<a rel="next" href="slozka.pl?id=10;strana=${n + 2}">Next</a>""")) }
        val result = client.readCourseDocuments("FYZI_6B", setOf("FYZI_6B"))
        assertTrue(result.truncated)
        assertEquals(45, server.requestCount)
    }

    @Test fun oversizedDocumentFailsBeforeAnyBytesReachTheDestination() {
        connect()
        server.enqueue(MockResponse().setBody("%PDF-1.7\n" + "x".repeat(600))
            .setHeader("Content-Type", "application/pdf").setHeader("Content-Length", 60L * 1024 * 1024))
        val output = ByteArrayOutputStream()
        val error = runCatching { client.downloadCourseDocument(CourseDocumentFile("10", "20", "Lecture", "application/pdf"), output) }.exceptionOrNull()
        assertTrue(error?.message.orEmpty().contains("50 MiB"))
        assertEquals(0, output.size())
    }

    @Test fun serverRedirectCannotTurnAFolderListingIntoAnUnrelatedFacultyCrawl() {
        connect()
        server.enqueue(page("""<a href="slozka.pl?id=10">FYZI_6B Physics</a>"""))
        server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", "/auth/dok_server/slozka.pl?id=999"))
        assertTrue(runCatching { client.readCourseDocuments("FYZI_6B", setOf("FYZI_6B")) }.isFailure)
        assertEquals(6, server.requestCount)
    }

    @Test fun alreadyCancelledTransferCannotIssueARequestOrWriteBytes() {
        connect()
        val output = ByteArrayOutputStream()
        val cancelled = java.util.concurrent.atomic.AtomicBoolean(true)
        val failure = runCatching { client.downloadCourseDocument(
            CourseDocumentFile("10", "20", "Lecture", "application/pdf"), output, cancelled) }.exceptionOrNull()
        assertTrue(failure is java.io.InterruptedIOException)
        assertEquals(4, server.requestCount)
        assertEquals(0, output.size())
    }

    @Test fun cancellingSessionDownloadInterruptsTheBlockedRequestAndKeepsSessionUsable() = kotlinx.coroutines.runBlocking {
        connect()
        val session = UisSession()
        UisSession::class.java.getDeclaredField("client").apply { isAccessible = true }.set(session, client)
        @Suppress("UNCHECKED_CAST")
        val state = UisSession::class.java.getDeclaredField("mutableState").apply { isAccessible = true }
            .get(session) as kotlinx.coroutines.flow.MutableStateFlow<UisResult>
        state.value = UisResult.CONNECTED
        server.enqueue(MockResponse().setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.NO_RESPONSE))
        val output = ByteArrayOutputStream()
        val job = launch(kotlinx.coroutines.Dispatchers.Default) {
            session.downloadCourseDocument(CourseDocumentFile("10", "20", "Lecture", "application/pdf"), output)
        }
        assertNotNull(server.takeRequest(3, java.util.concurrent.TimeUnit.SECONDS))
        job.cancel()
        kotlinx.coroutines.withTimeout(2_000) { job.join() }
        assertEquals(UisResult.CONNECTED, session.state.value)
        assertEquals(0, output.size())
        server.enqueue(page(""))
        assertEquals(UisResult.CONNECTED, client.checkSession())
    }

    @Test fun expiredRootIsAuthenticationFailureRatherThanEmptyDocuments() {
        connect()
        server.enqueue(MockResponse().setBody("<form><input name=credential_1></form>"))
        assertTrue(runCatching { client.readCourseDocuments("FYZI_6B", setOf("FYZI_6B")) }.exceptionOrNull() is com.kpyruy.takt.core.data.TeachingAuthenticationRequired)
    }
}
