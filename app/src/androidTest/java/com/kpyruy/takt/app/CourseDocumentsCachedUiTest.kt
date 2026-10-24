package com.kpyruy.takt.app

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.kpyruy.takt.core.data.*
import com.kpyruy.takt.core.model.*
import com.kpyruy.takt.core.ui.theme.TaktTheme
import com.kpyruy.takt.core.ui.i18n.TaktI18n
import com.kpyruy.takt.feature.subjects.CourseDocumentsPanel
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.OutputStream

class CourseDocumentsCachedUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun currentSavedCheckNeverDownloadsAndChangedRevisionWaitsForExplicitUpdate() {
        val data = (compose.activity.application as TaktApplication).dataContainer
        assertTrue(data.documentStore.isConnected)
        val course = Course("revision-test", "REV" + java.util.UUID.randomUUID().toString().take(8), "Versioned documents", 5, 1, CourseStatus.ENROLLED)
        val document = CourseDocumentFile("90841", "90842", "Lecture", "text/plain", "original.txt", revision = "version-1", downloadedRevision = "version-1")
        var catalog = CourseDocumentListing(listOf(document), true)
        var remote = document
        var streams = 0
        val repository = object : CourseDocumentsRepository {
            override suspend fun cachedDocuments(course: Course, activeCourseCodes: Set<String>) = catalog
            override suspend fun documents(course: Course, activeCourseCodes: Set<String>, refresh: Boolean) = CourseDocumentListing(listOf(remote), true)
            override suspend fun rememberDownloaded(course: Course, document: CourseDocumentFile, fileName: String) {
                catalog = CourseDocumentListing(listOf(document.copy(downloadedFileName = fileName)), true)
            }
            override suspend fun download(document: CourseDocumentFile, output: OutputStream): DownloadedCourseDocument {
                streams++; val bytes = remote.revision!!.toByteArray(); output.write(bytes)
                return DownloadedCourseDocument("original.txt", "text/plain", bytes.size.toLong())
            }
        }
        val downloads = CourseDocumentDownloads(compose.activity, data.documentStore, 123, "6".repeat(64))
        val source = java.io.File(compose.activity.cacheDir, "revision-${System.nanoTime()}.txt").apply { writeText("version-1") }
        val uri = runBlocking { data.documentStore.saveDownloadedDocument(course.code, source, "original.txt", "text/plain") }
        try {
            compose.activityRule.scenario.onActivity { activity -> activity.setContent {
                TaktI18n.use(AppLanguage.ENGLISH)
                TaktTheme { Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                    CourseDocumentsPanel(course, setOf(course.code), repository, downloads, true,
                        { _, reply -> reply(DeviceAuthenticationResult.CANCELLED) }, {})
                } }
            } }
            compose.onNodeWithTag("course-documents").performClick()
            compose.waitUntil(5_000) { compose.onAllNodesWithTag("documents-loading").fetchSemanticsNodes().isEmpty() }
            compose.onNodeWithTag("document-download-90842").assertContentDescriptionEquals("Saved").performClick()
            compose.waitForIdle(); assertEquals("A current saved file needs no byte request", 0, streams)
            remote = document.copy(revision = "version-2")
            compose.onNodeWithContentDescription("Update").performClick()
            compose.waitUntil(5_000) { compose.onAllNodesWithTag("documents-loading").fetchSemanticsNodes().isEmpty() }
            compose.onNodeWithTag("document-download-90842").assertContentDescriptionEquals("Download")
            assertEquals("Discovering an update only reads metadata", 0, streams)
            compose.onNodeWithText("Updated: Lecture", substring = true).assertExists()
            val dismiss = SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsActions.Dismiss)
            compose.onAllNodes(dismiss and hasAnyAncestor(hasTestTag("documents-updates"))).fetchSemanticsNodes().firstOrNull()?.let {
                compose.onNode(dismiss and hasAnyAncestor(hasTestTag("documents-updates")))
                    .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.Dismiss) { action -> action() }
            }
            compose.onNodeWithTag("document-download-90842").performClick()
            compose.waitUntil(5_000) { streams == 1 && compose.onAllNodesWithContentDescription("Saved").fetchSemanticsNodes().isNotEmpty() }
            assertEquals("version-2", compose.activity.contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() })
            compose.onNodeWithTag("document-download-90842").performClick()
            compose.waitForIdle(); assertEquals(1, streams)
        } finally {
            source.delete(); android.provider.DocumentsContract.deleteDocument(compose.activity.contentResolver, uri)
        }
    }
    @Test fun savedLinksAppearTogetherWithLocalFilesAndRemainReadyOnReopen() {
        val data = (compose.activity.application as TaktApplication).dataContainer
        assertTrue(data.documentStore.isConnected)
        val course = Course("instant-links", "LINK" + java.util.UUID.randomUUID().toString().take(8), "Saved links", 5, 1, CourseStatus.ENROLLED)
        val document = CourseDocumentFile("90831", "90832", "Linked lecture", "text/plain", "original.txt", downloadedFileName = "original.txt")
        val cached = CourseDocumentListing(listOf(document), true)
        var metadataGate = CompletableDeferred<Unit>()
        val metadataStarted = CompletableDeferred<Unit>()
        val networkGate = CompletableDeferred<Unit>()
        val repository = object : CourseDocumentsRepository {
            override suspend fun cachedDocuments(course: Course, activeCourseCodes: Set<String>): CourseDocumentListing {
                metadataStarted.complete(Unit); metadataGate.await(); return cached
            }
            override suspend fun documents(course: Course, activeCourseCodes: Set<String>, refresh: Boolean): CourseDocumentListing {
                networkGate.await(); return cached
            }
            override suspend fun download(document: CourseDocumentFile, output: OutputStream): DownloadedCourseDocument = error("No download before background refresh completes")
        }
        val downloads = CourseDocumentDownloads(compose.activity, data.documentStore, 123, "7".repeat(64))
        val source = java.io.File(compose.activity.cacheDir, "links-${System.nanoTime()}.txt").apply { writeText("saved") }
        val uri = runBlocking { data.documentStore.saveDownloadedDocument(course.code, source, "original.txt", "text/plain") }
        try {
            compose.activityRule.scenario.onActivity { activity -> activity.setContent {
                TaktI18n.use(AppLanguage.ENGLISH)
                TaktTheme { Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                    CourseDocumentsPanel(course, setOf(course.code), repository, downloads, true,
                        { _, reply -> reply(DeviceAuthenticationResult.CANCELLED) }, {})
                } }
            } }
            compose.onNodeWithTag("course-documents").performClick()
            compose.waitUntil(5_000) { metadataStarted.isCompleted }
            compose.waitForIdle()
            compose.onNodeWithText("original.txt").assertDoesNotExist()
            metadataGate.complete(Unit)
            compose.waitUntil(5_000) { compose.onAllNodesWithTag("course-document-90832").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("document-download-90832").assertContentDescriptionEquals("Saved")
            compose.onNodeWithText("${course.code} · Files: 1").assertIsDisplayed()
            metadataGate = CompletableDeferred()
            compose.onNodeWithContentDescription("Close").performClick()
            compose.onNodeWithTag("course-documents").performClick()
            compose.onNodeWithTag("course-document-90832").assertIsDisplayed()
            compose.onNodeWithText("original.txt").assertDoesNotExist()
            compose.onNodeWithTag("document-download-90832").assertContentDescriptionEquals("Saved")
            captureStudiesScreenshot("documents-instant-saved-links.png")
        } finally {
            metadataGate.complete(Unit); networkGate.complete(Unit); source.delete()
            android.provider.DocumentsContract.deleteDocument(compose.activity.contentResolver, uri)
        }
    }

    @Test fun originalFilenameMergesLocalAndUisRowsAndManualRefreshDoesNotDuplicateFiles() {
        val data = (compose.activity.application as TaktApplication).dataContainer
        assertTrue(data.documentStore.isConnected)
        val course = Course("merged-test", "MERGE" + java.util.UUID.randomUUID().toString().take(8), "Merged documents", 5, 1, CourseStatus.ENROLLED)
        val document = CourseDocumentFile("90821", "90822", "Lecture", "text/plain", "Lecture.txt")
        var body = "old content"
        var streams = 0
        var resolvedNames = 0
        val repository = object : CourseDocumentsRepository {
            override val cacheOwnerKey = "8".repeat(64)
            override suspend fun cachedDocuments(course: Course, activeCourseCodes: Set<String>) = CourseDocumentListing(listOf(document), true)
            override suspend fun documents(course: Course, activeCourseCodes: Set<String>, refresh: Boolean) = CourseDocumentListing(listOf(document), true)
            override suspend fun resolveDocument(course: Course, document: CourseDocumentFile): CourseDocumentFile {
                resolvedNames++
                return document.copy(fileName = "server-original.txt")
            }
            override suspend fun download(document: CourseDocumentFile, output: OutputStream): DownloadedCourseDocument {
                streams++; output.write(body.toByteArray())
                return DownloadedCourseDocument("server-original.txt", "text/plain", body.length.toLong())
            }
        }
        val downloads = CourseDocumentDownloads(compose.activity, data.documentStore, 123, repository.cacheOwnerKey)
        val source = java.io.File(compose.activity.cacheDir, "merge-${System.nanoTime()}.txt").apply { writeText(body) }
        val uri = runBlocking { data.documentStore.saveDownloadedDocument(course.code, source, "server-original.txt", "text/plain") }
        try {
            compose.activityRule.scenario.onActivity { activity -> activity.setContent {
                TaktI18n.use(AppLanguage.ENGLISH)
                TaktTheme { Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                    CourseDocumentsPanel(course, setOf(course.code), repository, downloads, true,
                        { _, reply -> reply(DeviceAuthenticationResult.CANCELLED) }, {})
                } }
            } }
            compose.onNodeWithTag("course-documents").performClick()
            compose.waitUntil(8_000) { resolvedNames == 1 && compose.onAllNodesWithTag("documents-loading").fetchSemanticsNodes().isEmpty() }
            assertEquals(0, streams)
            compose.onNodeWithText("server-original.txt").assertDoesNotExist()
            compose.onAllNodesWithText("Lecture").assertCountEquals(1)
            compose.onNodeWithText("${course.code} · Files: 1").assertIsDisplayed()
            compose.onNodeWithTag("document-download-90822").assertContentDescriptionEquals("Saved")
            captureStudiesScreenshot("documents-merged-local-and-uis.png")
            compose.onNodeWithTag("document-download-90822").performClick()
            compose.waitForIdle()
            assertEquals("A saved current attachment is not downloaded again", 0, streams)
            assertEquals(listOf("server-original.txt"), runBlocking { data.documentStore.downloadedDocuments(course.code).map { it.fileName } })
        } finally {
            source.delete()
            android.provider.DocumentsContract.deleteDocument(compose.activity.contentResolver, uri)
        }
    }

    @Test fun cachedListAndLocalOpenWorkDuringRefreshAndChangesAreNamed() {
        val data = (compose.activity.application as TaktApplication).dataContainer
        val course = Course("cached-test", "CACHED", "Cached documents", 5, 1, CourseStatus.ENROLLED)
        val file = CourseDocumentFile("90811", "90812", "Saved lecture", "application/pdf", "saved-lecture.pdf")
        val old = CourseDocumentListing(listOf(file),true)
        val release = CompletableDeferred<CourseDocumentListing>()
        val started = CompletableDeferred<Unit>()
        var streams = 0
        val repository = object : CourseDocumentsRepository {
            override val cacheOwnerKey = "c".repeat(64)
            override suspend fun cachedDocuments(course: Course, activeCourseCodes: Set<String>) = old
            override suspend fun documents(course: Course, activeCourseCodes: Set<String>, refresh: Boolean): CourseDocumentListing {
                started.complete(Unit);return release.await()
            }
            override suspend fun download(document: CourseDocumentFile, output: OutputStream): DownloadedCourseDocument {
                streams++;output.write("%PDF-1.7\nlocal".toByteArray());return DownloadedCourseDocument("saved-lecture.pdf","application/pdf",14)
            }
        }
        val downloads = CourseDocumentDownloads(compose.activity,data.documentStore,repository.accountGeneration.value,repository.cacheOwnerKey)
        runBlocking { downloads.cached(course,file,repository,refresh=true) }
        var opened:android.content.Intent? = null
        val monitor = object:android.app.Instrumentation.ActivityMonitor() {
            override fun onStartActivity(intent:android.content.Intent):android.app.Instrumentation.ActivityResult? {
                if(intent.action==android.content.Intent.ACTION_VIEW && intent.type=="application/pdf") {
                    opened=android.content.Intent(intent);return android.app.Instrumentation.ActivityResult(0,null)
                };return null
            }
        }
        val instrumentation=InstrumentationRegistry.getInstrumentation();instrumentation.addMonitor(monitor)
        try {
            compose.activityRule.scenario.onActivity { it.setContent {
                TaktI18n.use(AppLanguage.ENGLISH)
                TaktTheme { Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                    CourseDocumentsPanel(course,setOf(course.code),repository,downloads,true,{_,reply->reply(DeviceAuthenticationResult.CANCELLED)},{})
                } }
            } }
            compose.onNodeWithTag("course-documents").performClick()
            compose.waitUntil(5_000){started.isCompleted}
            compose.onNodeWithTag("course-document-90812").assertIsDisplayed().performClick()
            compose.waitUntil(5_000){opened!=null}
            assertEquals(1,streams)
            assertEquals("content",opened!!.data!!.scheme)
            release.complete(old.copy(files=listOf(file.copy(title="Updated lecture"),file.copy(documentId="90813",title="New handout",fileName="new.pdf"))))
            compose.waitUntil(8_000){compose.onAllNodesWithText("Added: New handout",substring=true).fetchSemanticsNodes().isNotEmpty()}
            compose.onNodeWithText("Updated: Updated lecture",substring=true).assertIsDisplayed()
            compose.onNodeWithText("Details").performClick()
            compose.onNodeWithText("Document changes").assertIsDisplayed()
            compose.onNodeWithText("Added: New handout").assertIsDisplayed()
            captureStudiesScreenshot("documents-cached-updates.png")
        } finally { release.complete(old);instrumentation.removeMonitor(monitor) }
    }
}
