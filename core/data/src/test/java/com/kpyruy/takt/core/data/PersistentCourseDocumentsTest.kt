package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream

class PersistentCourseDocumentsTest {
    private val owner = "a".repeat(64)
    private val course = Course("uis:1:2", "DOC", "Documents", 5, 1, CourseStatus.ENROLLED)
    private val file = CourseDocumentFile("10", "20", "Lecture", "application/pdf", fileName = "lecture.pdf", downloadedFileName = "actual-name.pdf")
    private val listing = CourseDocumentListing(listOf(file), true, roots = listOf(CourseDocumentFolder("10", "DOC Documents", "Documents")))
    @Test fun downloadedRevisionSurvivesRefreshAndBackupUntilTheUpdateIsSaved() = runBlocking {
        val store = InMemoryCourseDocumentsStore()
        val saved = file.copy(revision = "version-1", downloadedRevision = "version-1")
        store.write(owner, course.code, listing.copy(files = listOf(saved)))
        val repo = CachedCourseDocumentsRepository({ _, _, _ -> listing.copy(files = listOf(file.copy(revision = "version-2"))) },
            { _, _ -> error("A metadata refresh must not download the attachment") }, store, { owner })
        val changed = repo.documents(course, setOf(course.code), true).files.single()
        assertEquals("version-1", changed.downloadedRevision)
        assertTrue(changed.hasDownloadUpdate)
        val portable = BackupDocumentCourse(owner, course.code, store.read(owner, course.code)!!.toBackupListing())
        assertEquals("version-1", DocumentCatalogCodec.decode(DocumentCatalogCodec.encode(listOf(portable))).single().listing.files.single().downloadedRevision)
        repo.rememberDownloaded(course, changed.copy(downloadedRevision = changed.contentRevision), "actual-name.pdf")
        assertFalse(store.read(owner, course.code)!!.files.single().hasDownloadUpdate)
        val localMarkerOnly = listing.copy(files = listOf(saved.copy(downloadedRevision = "another-local-marker")))
        assertTrue(CourseDocumentChanges.between(listing.copy(files = listOf(saved)), localMarkerOnly).isEmpty)
    }
    @Test fun actualAttachmentNameSurvivesRefreshAndPortableCatalogRoundTrip() = runBlocking {
        val store = InMemoryCourseDocumentsStore()
        val guessed = listing.copy(files = listOf(file.copy(fileName = "Lecture.pdf", downloadedFileName = null)))
        store.write(owner, course.code, guessed)
        val repo = CachedCourseDocumentsRepository({ _, _, _ -> guessed }, { _, _ -> error("No byte download") }, store, { owner },
            details = { it.copy(fileName = "original.pdf") })
        val resolved = repo.resolveDocument(course, guessed.files.single())
        assertEquals("original.pdf", resolved.fileName)
        assertEquals("original.pdf", repo.documents(course, setOf(course.code), true).files.single().fileName)
        val portable = BackupDocumentCourse(owner, course.code, store.read(owner, course.code)!!.toBackupListing())
        assertEquals("original.pdf", DocumentCatalogCodec.decode(DocumentCatalogCodec.encode(listOf(portable))).single().listing.files.single().fileName)
    }

    @Test fun metadataResponseCannotSwitchTheDocumentIdentity() = runBlocking {
        val store = InMemoryCourseDocumentsStore(); store.write(owner, course.code, listing)
        val repo = CachedCourseDocumentsRepository({ _, _, _ -> listing }, { _, _ -> error("No") }, store, { owner },
            details = { it.copy(documentId = "99") })
        assertTrue(runCatching { repo.resolveDocument(course, file) }.isFailure)
        assertEquals(listing, store.read(owner, course.code))
    }
    @Test fun restartReadsTheCatalogWithoutNetworkThenRefreshUsesKnownRoots() = runBlocking {
        val store = InMemoryCourseDocumentsStore(); store.write(owner, course.code, listing)
        var loads = 0
        val repo = CachedCourseDocumentsRepository({ _,_,_-> error("Discovery must be skipped") }, { _,_-> error("No download") },
            store, {owner}, { _,_,_,roots -> loads++; assertEquals(listing.roots, roots); listing.copy(files=listOf(file.copy(downloadedFileName=null))) })
        assertEquals(listing, repo.cachedDocuments(course,setOf(course.code)))
        assertEquals(0,loads)
        assertEquals("actual-name.pdf",repo.documents(course,setOf(course.code),true).files.single().downloadedFileName)
        assertEquals(1,loads)
    }
    @Test fun localCatalogAndAllowedDownloadDoNotWaitForAListingRefresh(): Unit = runBlocking {
        val store=InMemoryCourseDocumentsStore();store.write(owner,course.code,listing)
        val started=CompletableDeferred<Unit>();val release=CompletableDeferred<Unit>()
        val repo=CachedCourseDocumentsRepository({_,_,_->started.complete(Unit);release.await();listing},
            {_,out->out.write(7);DownloadedCourseDocument("x.pdf","application/pdf",1)},store,{owner})
        repo.cachedDocuments(course,setOf(course.code))
        val refresh=async {repo.documents(course,setOf(course.code),true)};started.await()
        assertEquals(listing,withTimeout(500){repo.cachedDocuments(course,setOf(course.code))})
        assertEquals(1,withTimeout(500){repo.download(file,ByteArrayOutputStream()).byteCount}.toInt())
        release.complete(Unit);refresh.await()
    }
    @Test fun accountSwitchCannotPublishAnInflightCatalogOrReadAnotherOwner() = runBlocking {
        val store=InMemoryCourseDocumentsStore(); var activeOwner=owner
        val started=CompletableDeferred<Unit>(); val release=CompletableDeferred<Unit>()
        val repo=CachedCourseDocumentsRepository({_,_,_->started.complete(Unit);release.await();listing},{_,_->error("No")},store,{activeOwner})
        val refresh=async {runCatching {repo.documents(course,setOf(course.code),true)}}
        started.await();repo.clear();activeOwner="b".repeat(64);release.complete(Unit)
        assertTrue(refresh.await().isFailure)
        assertNull(store.read(owner,course.code));assertNull(repo.cachedDocuments(course,setOf(course.code)))
    }
    @Test fun restoredCatalogSupersedesMemoryAndCannotBeOverwrittenByStaleRefresh() = runBlocking {
        var persisted:CourseDocumentListing?=listing
        val store=object:CourseDocumentsStore {
            override suspend fun read(ownerKey:String,courseCode:String)=persisted
            override suspend fun write(ownerKey:String,courseCode:String,listing:CourseDocumentListing){persisted=listing}
        }
        val started=CompletableDeferred<Unit>();val release=CompletableDeferred<Unit>()
        val repo=CachedCourseDocumentsRepository({_,_,_->started.complete(Unit);release.await();listing},{_,_->error("No")},store,{owner})
        repo.cachedDocuments(course,setOf(course.code))
        val refresh=async {runCatching {repo.documents(course,setOf(course.code),true)}};started.await()
        val restored=listing.copy(files=listOf(file.copy(title="Restored lecture")));persisted=restored
        assertEquals(restored,repo.cachedDocuments(course,setOf(course.code)))
        release.complete(Unit);assertTrue(refresh.await().isFailure);assertEquals(restored,persisted)
    }
    @Test fun savedFilenameUpdateMergesWithTheCatalogRestoredBeforeItsWriteLease() = runBlocking {
        val store = InMemoryCourseDocumentsStore(); store.write(owner, course.code, listing)
        val restored = listing.copy(files = listOf(file.copy(title = "Restored title")))
        val repo = CachedCourseDocumentsRepository({_,_,_->listing},{_,_->error("No download")}, store, {owner},
            stableWrite = { action -> store.write(owner, course.code, restored); action() })
        repo.rememberDownloaded(course, file, "new-copy.pdf")
        val actual = store.read(owner, course.code)!!.files.single()
        assertEquals("Restored title", actual.title)
        assertEquals("new-copy.pdf", actual.downloadedFileName)
    }

    @Test fun portableCatalogRoundTripsAndOnlyMaterialsImportsIt() {
        val entry=BackupDocumentCourse(owner,course.code,listing.toBackupListing())
        val original=BackupPayload(courses=listOf(BackupCourse(course.id,course.code,course.title,5,1,"enrolled","COMPULSORY")),courseDocuments=listOf(entry))
        val round=BackupPayloadCodec.decode(BackupPayloadCodec.encode(original))
        assertEquals(original,round);assertEquals(listing,round.courseDocuments!!.single().listing.toModel())
        val empty=original.copy(courseDocuments=null)
        assertEquals(listOf(entry),BackupMerger.merge(empty,round,setOf(BackupSection.MATERIALS)).courseDocuments)
        assertNull(BackupMerger.merge(empty,round,setOf(BackupSection.PROGRESS)).courseDocuments)
        assertEquals(setOf(BackupSection.MATERIALS),BackupMerger.changedSections(empty,round))
        assertNull(BackupPayloadCodec.decode("""{"version":11,"courses":[]}""").courseDocuments)
        assertThrows(IllegalArgumentException::class.java){DocumentCatalogCodec.encode(listOf(entry.copy(ownerKey="credentials")))}
        assertThrows(IllegalArgumentException::class.java){DocumentCatalogCodec.encode(listOf(entry.copy(listing=entry.listing.copy(folderUrl="https://elsewhere.example/"))))}
    }
    @Test fun changesDistinguishMetadataBytesAndLocalNames() {
        assertTrue(CourseDocumentChanges.between(listing, listing.copy(files = listOf(file.copy(downloadedFileName = "different.pdf")))).isEmpty)
        val fresh=listing.copy(files=listOf(file.copy(title="Updated"),file.copy(documentId="21",title="Added")))
        val changed=CourseDocumentChanges.between(listing,fresh)
        assertEquals(listOf("Added"),changed.added.map{it.title});assertEquals(listOf("Updated"),changed.updated.map{it.title})
        assertEquals(listOf(file),CourseDocumentChanges.between(listing,listing,setOf("10:20")).updated)
        assertEquals(listOf(file),CourseDocumentChanges.between(listing,listing.copy(files=emptyList())).removed)
    }
}
