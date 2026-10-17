package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class CourseDocumentsRepositoryTest {
    private val course = Course("uis:123:441254", "TPAR_6B", "Hardware", 5, 3, CourseStatus.ENROLLED)
    @Test fun inactiveCourseNeverLoadsAndCachedListingsRespectRefresh() = runBlocking {
        var calls = 0
        val repository = CachedCourseDocumentsRepository({ _, _, _ -> calls++; CourseDocumentListing(emptyList(), true) },
            { _, _ -> error("download must be deliberate") })
        assertFalse(repository.documents(course, emptySet()).foldersFound)
        assertFalse(repository.documents(course.copy(status = CourseStatus.PLANNED), setOf(course.code)).foldersFound)
        repository.documents(course, setOf(course.code)); repository.documents(course, setOf(course.code))
        assertEquals(1, calls)
        repository.documents(course, setOf(course.code), true)
        assertEquals(2, calls)
    }
    @Test fun onlyExplicitlyListedDocumentsCanReachTheDownloadStreamAndRemovalRevokesThem() = runBlocking {
        var downloads = 0
        val attachment = CourseDocumentFile("10", "20", "Lecture", "application/pdf")
        val repository = CachedCourseDocumentsRepository({ _, _, _ -> CourseDocumentListing(listOf(attachment), true) },
            { _, output -> downloads++; output.write(7); DownloadedCourseDocument("Lecture.pdf", "application/pdf", 1) })
        val output = java.io.ByteArrayOutputStream()
        assertTrue(runCatching { repository.download(attachment, output) }.isFailure)
        repository.documents(course, setOf(course.code))
        assertEquals(1L, repository.download(attachment, output).byteCount)
        assertTrue(runCatching { repository.download(attachment.copy(documentId = "21"), output) }.isFailure)
        repository.clear()
        assertTrue(runCatching { repository.download(attachment, output) }.isFailure)
        assertEquals(1, downloads)
        assertEquals(listOf(7.toByte()), output.toByteArray().toList())
    }

    @Test fun accountGenerationsAreUniqueAndClearInvalidatesPreviewKeysImmediately() {
        fun repository() = CachedCourseDocumentsRepository({ _, _, _ -> CourseDocumentListing(emptyList(), true) },
            { _, _ -> error("no download") })
        val first = repository()
        val second = repository()
        val initial = first.accountGeneration.value
        assertTrue(initial > 0)
        assertNotEquals(initial, second.accountGeneration.value)
        first.clear()
        assertTrue(first.accountGeneration.value > initial)
        assertNotEquals(first.accountGeneration.value, second.accountGeneration.value)
        val fake = object : CourseDocumentsRepository {
            override suspend fun documents(course: Course, activeCourseCodes: Set<String>, refresh: Boolean) = CourseDocumentListing(emptyList(), false)
            override suspend fun download(document: CourseDocumentFile, output: java.io.OutputStream): DownloadedCourseDocument = error("no download")
        }
        assertEquals(0L, fake.accountGeneration.value)
    }

    @Test fun accountChangeInvalidatesAnInflightListingAndClearsPreviousEntries() = runBlocking {
        val started = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>()
        val repository = CachedCourseDocumentsRepository({ _, _, _ -> started.complete(Unit); release.await(); CourseDocumentListing(emptyList(), true) },
            { _, _ -> error("no download") })
        val loading = async { runCatching { repository.documents(course, setOf(course.code)) } }
        started.await(); repository.clear(); release.complete(Unit)
        assertTrue(loading.await().isFailure)
    }
}
