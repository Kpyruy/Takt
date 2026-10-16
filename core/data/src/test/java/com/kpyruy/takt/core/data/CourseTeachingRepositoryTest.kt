package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class CourseTeachingRepositoryTest {
    private val course = Course("uis:123:441254", "TPAR_6B", "Hardware", 5, 3, CourseStatus.ENROLLED,
        syllabusUrl = "https://is.stuba.sk/auth/katalog/syllabus.pl?predmet=441254")
    private val teacher = CourseTeacher("71659", "Ing. Martin Bartoň, PhD.")

    @Test fun repeatedViewsAreCachedRefreshIsExplicitAndFailureKeepsOldData() = runBlocking {
        var calls = 0
        var fail = false
        val repository = CachedCourseTeachingRepository({ id ->
            assertEquals("441254", id); calls++
            if (fail) error("offline")
            listOf(teacher)
        }, { id, _ -> TeacherProfile(id, teacher.name) })
        assertEquals(listOf(teacher), repository.teachers(course))
        assertEquals(listOf(teacher), repository.teachers(course))
        assertEquals(1, calls)
        fail = true
        assertTrue(runCatching { repository.teachers(course, true) }.isFailure)
        assertEquals(listOf(teacher), repository.teachers(course))
        assertEquals(2, calls)
    }

    @Test fun languageCachesAreSeparateAndLargePortraitsAreNotRetained() = runBlocking {
        var profiles = 0; var photos = 0
        val repository = CachedCourseTeachingRepository({ listOf(teacher) }, { id, _ ->
            profiles++; TeacherProfile(id, teacher.name)
        }, { photos++; ByteArray(350_001) })
        repository.profile(teacher, AppLanguage.SLOVAK)
        repository.profile(teacher, AppLanguage.ENGLISH)
        repository.profile(teacher, AppLanguage.SLOVAK)
        assertEquals(2, profiles)
        assertNull(repository.photo(teacher.personId))
        assertNull(repository.photo(teacher.personId))
        assertEquals(1, photos)
        repository.clear()
        repository.profile(teacher, AppLanguage.SLOVAK)
        assertEquals(3, profiles)
    }

    @Test fun changedAccountCannotReceiveAnOldInFlightCacheEntry() = runBlocking {
        val started = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>()
        val repository = CachedCourseTeachingRepository({ started.complete(Unit); release.await(); listOf(teacher) },
            { id, _ -> TeacherProfile(id, teacher.name) })
        val result = async { runCatching { repository.teachers(course) } }
        started.await(); repository.clear(); release.complete(Unit)
        assertTrue(result.await().isFailure)
    }

    @Test fun foreignLinksNeverReachTheAuthenticatedLoader() = runBlocking {
        var calls = 0
        val repository = CachedCourseTeachingRepository({ calls++; listOf(teacher) }, { id, _ -> TeacherProfile(id, teacher.name) })
        for (url in listOf("https://evil.test/auth/katalog/syllabus.pl?predmet=441254",
            "https://someone@is.stuba.sk/auth/katalog/syllabus.pl?predmet=441254",
            "https://is.stuba.sk/auth/katalog/syllabus.pl?predmet=1;predmet=2")) {
            assertTrue(runCatching { repository.teachers(course.copy(id = "local", syllabusUrl = url)) }.isFailure)
        }
        assertEquals(0, calls)
    }
}
