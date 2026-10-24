package com.kpyruy.takt.app

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.kpyruy.takt.core.data.*
import com.kpyruy.takt.core.model.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.OutputStream
import java.util.UUID

/** Run after CourseDocumentsUiTest has granted the disposable emulator's Documents tree. */
class CourseDocumentDownloadsCacheTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun returningToAnEarlierFilenamePublishesADistinctPreviewVersion(): Unit = runBlocking {
        val data=(compose.activity.application as TaktApplication).dataContainer
        val course=Course("preview-versions","PREVIEW","Preview",5,1,CourseStatus.ENROLLED)
        val doc=CourseDocumentFile("99011","99012","Preview","text/plain","a.txt")
        var name="a.txt"
        val repository=object:CourseDocumentsRepository {
            override suspend fun documents(course:Course,activeCourseCodes:Set<String>,refresh:Boolean)=CourseDocumentListing(listOf(doc),true)
            override suspend fun download(document:CourseDocumentFile,output:OutputStream):DownloadedCourseDocument {
                output.write(name.toByteArray());return DownloadedCourseDocument(name,"text/plain",name.length.toLong())
            }
        }
        val cache=CourseDocumentDownloads(compose.activity,data.documentStore,123,"e".repeat(64))
        val first=cache.cached(course,doc,repository,true)
        name="b.txt";cache.cached(course,doc,repository,true)
        name="a.txt";val current=cache.cached(course,doc,repository,true)
        assertNotEquals(first.file,current.file)
        assertEquals(current.uri,cache.available(course,doc)!!.uri)
        assertEquals("a.txt",current.file.readText())
    }

    @Test fun savedCopiesOpenDuringRefreshRecoverFromFolderAndKeepOldBytesOnFailure() = runBlocking {
        val data=(compose.activity.application as TaktApplication).dataContainer
        assertTrue("The fixture must connect Documents first",data.documentStore.isConnected)
        val code="CACHE"+UUID.randomUUID().toString().take(8)
        val course=Course("download-cache",code,"Downloads",5,1,CourseStatus.ENROLLED)
        val owner="f".repeat(64)
        val doc=CourseDocumentFile("99001","99002","Report","text/plain","report.txt")
        var body="old content";var fail=false;var gate:CompletableDeferred<Unit>?=null
        val started=CompletableDeferred<Unit>()
        val repository=object:CourseDocumentsRepository {
            override suspend fun documents(course:Course,activeCourseCodes:Set<String>,refresh:Boolean)=CourseDocumentListing(listOf(doc),true)
            override suspend fun download(document:CourseDocumentFile,output:OutputStream):DownloadedCourseDocument {
                gate?.let { started.complete(Unit);it.await() }
                if(fail)throw java.io.IOException("Synthetic download failure")
                output.write(body.toByteArray());return DownloadedCourseDocument("report.txt","text/plain",body.length.toLong())
            }
        }
        val cache=CourseDocumentDownloads(compose.activity,data.documentStore,123,owner)
        val resolver=compose.activity.contentResolver
        try {
            val first=cache.save(course,doc,cache.cached(course,doc,repository,true))
            val recreated=CourseDocumentDownloads(compose.activity,data.documentStore,999,owner)
            assertEquals(first,recreated.available(course,doc)!!.uri)
            assertFalse("Restored files with unchanged bytes must not be reported as modified",recreated.refreshSaved(course,doc,repository))
            body="updated content";gate=CompletableDeferred()
            val refresh=async(Dispatchers.IO){recreated.refreshSaved(course,doc,repository)};started.await()
            assertEquals(first,withTimeout(1_000){recreated.available(course,doc)!!.uri})
            assertEquals("old content",resolver.openInputStream(first)!!.bufferedReader().use{it.readText()})
            gate!!.complete(Unit);assertTrue(refresh.await());gate=null
            val latest=recreated.available(course,doc)!!
            assertNotEquals(first,latest.uri)
            assertEquals(body,resolver.openInputStream(latest.uri)!!.bufferedReader().use{it.readText()})
            assertEquals("An already-opening viewer retains a grace period", "old content",resolver.openInputStream(first)!!.bufferedReader().use{it.readText()})
            fail=true;assertTrue(runCatching{recreated.refreshSaved(course,doc,repository)}.isFailure)
            assertEquals(latest.uri,recreated.available(course,doc)!!.uri)
            // Simulate restored metadata: no saved-URI preference, only the portable actual filename.
            val prefs=compose.activity.getSharedPreferences("takt_course_downloads",0)
            prefs.edit().apply { prefs.all.keys.filter{it.contains(owner)}.forEach{remove(it)} }.commit()
            val restored=doc.copy(downloadedFileName=latest.fileName)
            assertEquals(latest.uri,CourseDocumentDownloads(compose.activity,data.documentStore,432,owner).available(course,restored)!!.uri)
        } finally {
            gate?.complete(Unit)
            data.documentStore.downloadedDocuments(code).forEach { runCatching{android.provider.DocumentsContract.deleteDocument(resolver,it.uri)} }
        }
    }
}
