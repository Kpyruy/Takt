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
