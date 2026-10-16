package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.*
import com.kpyruy.takt.core.data.uis.UisTeachingLinks
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface CourseTeachingRepository {
    suspend fun teachers(course: Course, refresh: Boolean = false): List<CourseTeacher>
    suspend fun profile(teacher: CourseTeacher, language: AppLanguage, refresh: Boolean = false): TeacherProfile
    suspend fun photo(personId: String): ByteArray? = null
}

class TeachingAuthenticationRequired : IllegalStateException("UIS authentication required")
class TeachingSessionUnavailable : IllegalStateException("UIS session needs attention")

/** Bounded, account-scoped, memory-only cache; no contacts/cookies enter study backups. */
internal class CachedCourseTeachingRepository(
    private val loadTeachers: suspend (String) -> List<CourseTeacher>,
    private val loadProfile: suspend (String, AppLanguage) -> TeacherProfile,
    private val loadPhoto: suspend (String) -> ByteArray? = { null },
) : CourseTeachingRepository {
    private val mutex = Mutex()
    private val monitor = Any()
    private var generation = 0L
    private val teams = linkedMapOf<String, List<CourseTeacher>>()
    private val people = linkedMapOf<String, TeacherProfile>()
    private val photos = linkedMapOf<String, ByteArray?>()

    fun clear() = synchronized(monitor) { generation++; teams.clear(); people.clear(); photos.clear() }

    override suspend fun teachers(course: Course, refresh: Boolean): List<CourseTeacher> = mutex.withLock {
        val id = course.syllabusUrl?.let(UisTeachingLinks::subjectId)
            ?: course.id.takeIf { it.matches(Regex("uis:[1-9][0-9]*:[1-9][0-9]*")) }?.substringAfterLast(':')
            ?: error("UIS syllabus link is unavailable")
        cached(teams, id, refresh) { loadTeachers(id) }
    }
    override suspend fun profile(teacher: CourseTeacher, language: AppLanguage, refresh: Boolean): TeacherProfile = mutex.withLock {
        require(UisTeachingLinks.validId(teacher.personId))
        cached(people, "${teacher.personId}:${language.name}", refresh) { loadProfile(teacher.personId, language) }
    }
    override suspend fun photo(personId: String): ByteArray? = mutex.withLock {
        require(UisTeachingLinks.validId(personId))
        cached(photos, personId, false, limit = 8) { loadPhoto(personId)?.takeIf { it.size <= 350_000 } }
    }

    private suspend fun <T> cached(cache: LinkedHashMap<String, T>, key: String, refresh: Boolean, limit: Int = 64, load: suspend () -> T): T {
        val revision = synchronized(monitor) {
            if (!refresh && cache.containsKey(key)) {
                @Suppress("UNCHECKED_CAST") val hit = cache.remove(key) as T
                cache[key] = hit
                return hit
            }
            generation
        }
        val value = load()
        synchronized(monitor) {
            check(revision == generation) { "UIS account changed while loading contacts" }
            cache.remove(key); cache[key] = value
            while (cache.size > limit) cache.remove(cache.keys.first())
        }
        return value
    }
}
