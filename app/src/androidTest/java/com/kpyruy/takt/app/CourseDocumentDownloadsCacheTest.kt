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

    @Test fun repeatedSavesReplaceOneFileAndUnindexedOriginalNamesAreRecovered() = runBlocking {
        val data = (compose.activity.application as TaktApplication).dataContainer
        assertTrue(data.documentStore.isConnected)
        val course = Course("overwrite", "ONE" + UUID.randomUUID().toString().take(8), "One file", 5, 1, CourseStatus.ENROLLED)
        val doc = CourseDocumentFile("99111", "99112", "Lecture", "text/plain", "original.txt")
        val owner = "9".repeat(64)
        var body = "old contents that must be truncated"
        val repository = object : CourseDocumentsRepository {
            override suspend fun documents(course: Course, activeCourseCodes: Set<String>, refresh: Boolean) = CourseDocumentListing(listOf(doc), true)
            override suspend fun download(document: CourseDocumentFile, output: OutputStream): DownloadedCourseDocument {
                output.write(body.toByteArray())
                return DownloadedCourseDocument("original.txt", "text/plain", body.length.toLong())
            }
        }
        val cache = CourseDocumentDownloads(compose.activity, data.documentStore, 123, owner)
        val resolver = compose.activity.contentResolver
        try {
            val first = cache.save(course, doc, cache.cached(course, doc, repository, true))
            val prefs = compose.activity.getSharedPreferences("takt_course_downloads", 0)
            prefs.edit().putLong("retired:$first", 0L).commit()
            assertEquals("An old retirement marker must not remove a reused, currently indexed URI", listOf(first), cache.localFiles(course).map { it.uri })
            val legacy = data.documentStore.saveDownloadedDocument(course.code, cache.cached(course, doc, repository).file,
                "original (1).txt", "text/plain")
            val restored = doc.copy(downloadedFileName = "original (1).txt")
            prefs.edit().apply { prefs.all.keys.filter { it.contains(owner) }.forEach { remove(it) } }.commit()
            val recreated = CourseDocumentDownloads(compose.activity, data.documentStore, 456, owner)
            assertEquals("Recover a previously indexed legacy copy", legacy, recreated.saved(course, restored))
            assertEquals("Consolidate unchanged legacy copies into the original filename", first,
                recreated.save(course, restored, recreated.cached(course, doc, repository, true)))
            assertEquals(listOf("original.txt"), data.documentStore.downloadedDocuments(course.code).map { it.fileName })
            prefs.edit().apply { prefs.all.keys.filter { it.contains(owner) }.forEach { remove(it) } }.commit()
            assertEquals("Recognize an existing original filename without the old index", first, recreated.saved(course, doc))
            body = "new"
            val latest = recreated.save(course, doc, recreated.cached(course, doc, repository, true))
            assertEquals(first, latest)
            assertEquals(body, resolver.openInputStream(latest)!!.bufferedReader().use { it.readText() })
            assertEquals(listOf("original.txt"), data.documentStore.downloadedDocuments(course.code).map { it.fileName })
        } finally {
            data.documentStore.downloadedDocuments(course.code).forEach { android.provider.DocumentsContract.deleteDocument(resolver, it.uri) }
        }
    }
    @Test fun interruptedOverwriteRecoversOldBytesBeforeListingFiles() = runBlocking {
        val data = (compose.activity.application as TaktApplication).dataContainer
        assertTrue(data.documentStore.isConnected)
        val code = "UNDO" + UUID.randomUUID().toString().take(8)
        val source = java.io.File(compose.activity.cacheDir, "undo-source-${UUID.randomUUID()}.txt").apply { writeText("original contents") }
        val uri = data.documentStore.saveDownloadedDocument(code, source, "report.txt", "text/plain")
        val undo = java.io.File(compose.activity.filesDir, "course-document-undo.bin")
        val prefs = compose.activity.getSharedPreferences("takt_documents", 0)
        try {
            // Durable state left by process death after truncation but before publication completes.
            undo.writeText("original contents")
            prefs.edit().putString("download_undo_uri", uri.toString()).commit()
            compose.activity.contentResolver.openOutputStream(uri, "wt")!!.use { it.write("partial".toByteArray()) }
            assertEquals(listOf("report.txt"), data.documentStore.downloadedDocuments(code).map { it.fileName })
            assertEquals("original contents", compose.activity.contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() })
            assertFalse(prefs.contains("download_undo_uri"))
            assertFalse(undo.exists())
        } finally {
            prefs.edit().remove("download_undo_uri").commit(); undo.delete(); source.delete()
            android.provider.DocumentsContract.deleteDocument(compose.activity.contentResolver, uri)
        }
    }

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
            assertEquals(first,latest.uri)
            assertEquals(body,resolver.openInputStream(latest.uri)!!.bufferedReader().use{it.readText()})
            assertEquals("The same public file now contains the updated version", body,resolver.openInputStream(first)!!.bufferedReader().use{it.readText()})
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
