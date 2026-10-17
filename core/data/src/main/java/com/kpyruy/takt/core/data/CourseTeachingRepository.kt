package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.*
import com.kpyruy.takt.core.data.uis.UisTeachingLinks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface CourseTeachingRepository {
    val changes: Flow<Unit> get() = emptyFlow()
    fun savedTeachers(course: Course): List<CourseTeacher>? = null
    fun savedProfile(teacher: CourseTeacher, language: AppLanguage): TeacherProfile? = null
    suspend fun teachers(course: Course, refresh: Boolean = false): List<CourseTeacher>
    suspend fun profile(teacher: CourseTeacher, language: AppLanguage, refresh: Boolean = false): TeacherProfile
    suspend fun photo(personId: String): ByteArray? = null
}

class TeachingAuthenticationRequired : IllegalStateException("UIS authentication required")
class TeachingSessionUnavailable : IllegalStateException("UIS session needs attention")

/** Durable teams and contacts; portraits remain bounded and memory-only. */
internal class CachedCourseTeachingRepository(
    private val loadTeachers: suspend (Course, Boolean) -> List<CourseTeacher>,
    private val loadProfile: suspend (String, AppLanguage) -> TeacherProfile,
    private val loadPhoto: suspend (String) -> ByteArray? = { null },
    private val store: TeachingStore = TeachingStore(),
) : CourseTeachingRepository {
    private val mutex = Mutex()
    private val monitor = Any()
    private var generation = 0L
    override val changes: Flow<Unit> get() = store.snapshot.map { Unit }
    override fun savedTeachers(course: Course): List<CourseTeacher>? = store.snapshot.value.teams
        .firstOrNull { teachingCode(it.courseCode) == teachingCode(course.code) }?.teachers?.map { it.toModel() }
    override fun savedProfile(teacher: CourseTeacher, language: AppLanguage): TeacherProfile? = store.snapshot.value.profiles
        .firstOrNull { it.personId == teacher.personId && it.language == language.name }?.toModel()
    private val photos = linkedMapOf<String, ByteArray?>()

    fun clear() = synchronized(monitor) { generation++; store.replace(BackupTeaching()); photos.clear() }

    override suspend fun teachers(course: Course, refresh: Boolean): List<CourseTeacher> {
        val revision = store.generation()
        return withContext(Dispatchers.IO) { mutex.withLock {
            check(revision == store.generation()) { "Teaching data changed while waiting" }
            course.syllabusUrl?.let(UisTeachingLinks::subjectId)
                ?: course.id.takeIf { it.matches(Regex("uis:[1-9][0-9]*:[1-9][0-9]*")) }?.substringAfterLast(':')
                ?: error("UIS syllabus link is unavailable")
            if (!refresh) savedTeachers(course)?.let { return@withLock it }
            val value = loadTeachers(course, refresh)
            currentCoroutineContext().ensureActive()
            store.update(revision) { snapshot ->
                val team = BackupTeachingTeam(teachingCode(course.code), value.map { it.toBackup() })
                if (snapshot.teams.firstOrNull { teachingCode(it.courseCode) == team.courseCode } == team) snapshot
                else snapshot.copy(teams = (snapshot.teams.filterNot { teachingCode(it.courseCode) == team.courseCode } + team).takeLast(128))
            }
            value
        } }
    }
    override suspend fun profile(teacher: CourseTeacher, language: AppLanguage, refresh: Boolean): TeacherProfile {
        val revision = store.generation()
        return withContext(Dispatchers.IO) { mutex.withLock {
            check(revision == store.generation()) { "Teaching data changed while waiting" }
            require(UisTeachingLinks.validId(teacher.personId))
            if (!refresh) savedProfile(teacher, language)?.let { return@withLock it }
            val value = loadProfile(teacher.personId, language)
            currentCoroutineContext().ensureActive()
            store.update(revision) { snapshot ->
                val profile = value.toBackup(language)
                if (snapshot.profiles.firstOrNull { it.personId == teacher.personId && it.language == language.name } == profile) snapshot
                else snapshot.copy(profiles = (snapshot.profiles.filterNot {
                    it.personId == teacher.personId && it.language == language.name
                } + profile).takeLast(256))
            }
            value
        } }
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
