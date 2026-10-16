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
    @Test fun accountChangeInvalidatesAnInflightListingAndClearsPreviousEntries() = runBlocking {
        val started = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>()
        val repository = CachedCourseDocumentsRepository({ _, _, _ -> started.complete(Unit); release.await(); CourseDocumentListing(emptyList(), true) },
            { _, _ -> error("no download") })
        val loading = async { runCatching { repository.documents(course, setOf(course.code)) } }
        started.await(); repository.clear(); release.complete(Unit)
        assertTrue(loading.await().isFailure)
    }
}
