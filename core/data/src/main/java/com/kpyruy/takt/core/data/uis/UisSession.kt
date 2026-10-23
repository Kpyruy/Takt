package com.kpyruy.takt.core.data.uis

import com.kpyruy.takt.core.data.UniversityCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean
import com.kpyruy.takt.core.model.SemesterPeriod

/** Memory-only: neither UIS cookies nor pending two-factor credentials enter backups. */
class UisSession {
    @Volatile private var client: UisClient? = null
    private val operations = Mutex()
    private val mutableState = MutableStateFlow(UisResult.DISCONNECTED)
    val state = mutableState.asStateFlow()
    private val mutableFailure = MutableStateFlow<UisFailure?>(null)
    val failure = mutableFailure.asStateFlow()

    suspend fun login(credentials: UniversityCredentials) = operations.withLock {
        disconnect()
        val current = UisClient()
        client = current
        execute(current) { current.login(credentials.login, credentials.password) }
    }

    suspend fun submitCode(code: String) = operations.withLock {
        val current = client ?: return@withLock
        if (mutableState.value != UisResult.SECOND_FACTOR) return@withLock
        execute(current) { current.submitCode(code) }
    }

    suspend fun check() = operations.withLock {
        val current = client ?: return@withLock
        if (mutableState.value != UisResult.CONNECTED) return@withLock
        execute(current) { current.checkSession() }
    }

    internal suspend fun readStudentHubOverview(): com.kpyruy.takt.core.model.StudentHubSnapshot = operations.withLock {
        val current=client ?: throw com.kpyruy.takt.core.data.TeachingSessionUnavailable()
        check(mutableState.value == UisResult.CONNECTED)
        try { request(current) { current.readStudentHubOverview() } }
        catch (expired: com.kpyruy.takt.core.data.TeachingAuthenticationRequired) { if (client === current) disconnect(); throw expired }
    }
    internal suspend fun readStudentStudy(study: com.kpyruy.takt.core.model.StudentStudy): com.kpyruy.takt.core.model.StudentStudy = operations.withLock {
        val current=client ?: throw com.kpyruy.takt.core.data.TeachingSessionUnavailable()
        check(mutableState.value == UisResult.CONNECTED)
        try { request(current) { current.readStudentStudy(study) } }
        catch (expired: com.kpyruy.takt.core.data.TeachingAuthenticationRequired) { if (client === current) disconnect(); throw expired }
    }

    internal suspend fun readStudyPlan(): UisStudyPlan = operations.withLock {
        val current = client ?: error("UIS session is not connected")
        check(mutableState.value == UisResult.CONNECTED)
        return@withLock request(current) { current.readStudyPlan() }
    }

    internal suspend fun readStudyResults(studyId: String, periodId: String): Map<String, UisStudyResult> = operations.withLock {
        val current = client ?: error("UIS session is not connected")
        check(mutableState.value == UisResult.CONNECTED)
        return@withLock request(current) { current.readStudyResults(studyId, periodId) }
    }

    internal suspend fun readCourseLessons(code: String): List<UisTimetableItem> = operations.withLock {
        val current = client ?: error("UIS session is not connected")
        check(mutableState.value == UisResult.CONNECTED)
        return@withLock request(current) { current.readCourseLessons(code) }
    }

    internal suspend fun readCourseTeachers(course: com.kpyruy.takt.core.model.Course, refresh: Boolean): List<com.kpyruy.takt.core.model.CourseTeacher> = operations.withLock {
        val current = client ?: throw com.kpyruy.takt.core.data.TeachingSessionUnavailable()
        check(mutableState.value == UisResult.CONNECTED)
        return@withLock try { request(current) { current.readCourseTeachers(
            course.syllabusUrl?.let(UisTeachingLinks::subjectId) ?: course.id.substringAfterLast(':'),
            course.code,
            course.id.takeIf { it.matches(Regex("uis:[1-9][0-9]*:[1-9][0-9]*")) }?.split(':')?.get(1),
            refresh) } }
        catch (expired: com.kpyruy.takt.core.data.TeachingAuthenticationRequired) { if (client === current) disconnect(); throw expired }
    }
    internal suspend fun readCourseDocuments(code: String, activeCodes: Set<String>, refresh: Boolean): com.kpyruy.takt.core.model.CourseDocumentListing = operations.withLock {
        val current = client ?: throw com.kpyruy.takt.core.data.TeachingSessionUnavailable()
        check(mutableState.value == UisResult.CONNECTED)
        return@withLock try { request(current) { current.readCourseDocuments(code, activeCodes, refresh) } }
        catch (expired: com.kpyruy.takt.core.data.TeachingAuthenticationRequired) { if (client === current) disconnect(); throw expired }
    }
    internal suspend fun downloadCourseDocument(document: com.kpyruy.takt.core.model.CourseDocumentFile, output: java.io.OutputStream): com.kpyruy.takt.core.model.DownloadedCourseDocument = operations.withLock {
        val current = client ?: throw com.kpyruy.takt.core.data.TeachingSessionUnavailable()
        check(mutableState.value == UisResult.CONNECTED)
        try { request(current) { current.downloadCourseDocument(document, output) } }
        catch (expired: com.kpyruy.takt.core.data.TeachingAuthenticationRequired) { if (client === current) disconnect(); throw expired }
    }

    internal suspend fun readTeacherProfile(personId: String, language: com.kpyruy.takt.core.model.AppLanguage): com.kpyruy.takt.core.model.TeacherProfile = operations.withLock {
        val current = client ?: throw com.kpyruy.takt.core.data.TeachingSessionUnavailable()
        check(mutableState.value == UisResult.CONNECTED)
        return@withLock try { request(current) { current.readTeacherProfile(personId, language) } }
        catch (expired: com.kpyruy.takt.core.data.TeachingAuthenticationRequired) { if (client === current) disconnect(); throw expired }
    }
    internal suspend fun readTeacherPhoto(personId: String): ByteArray? = operations.withLock {
        val current = client ?: return@withLock null
        if (mutableState.value != UisResult.CONNECTED) return@withLock null
        return@withLock request(current) { current.readTeacherPhoto(personId) }
    }

    internal suspend fun readStudyContext(studyId: String, periodId: String): Pair<SemesterPeriod, List<com.kpyruy.takt.core.data.uis.UisTimetableItem>> = operations.withLock {
        val current = client ?: error("UIS session is not connected")
        check(mutableState.value == UisResult.CONNECTED)
        return@withLock request(current) {
            val links = current.readStudyLinks(studyId, periodId)
            current.readAcademicCalendar(requireNotNull(links.calendar) { "UIS academic calendar link missing" }) to
                current.readTimetable(requireNotNull(links.timetable) { "UIS personal timetable link missing" })
        }
    }

    internal suspend fun readSelectedContext(
        studyId: String,
        periodId: String,
        calendar: Boolean,
        timetable: Boolean,
    ): Pair<SemesterPeriod?, List<UisTimetableItem>?> = operations.withLock {
        if (!calendar && !timetable) return@withLock null to null
        val current = client ?: error("UIS session is not connected")
        check(mutableState.value == UisResult.CONNECTED)
        return@withLock request(current) {
            val links = current.readStudyLinks(studyId, periodId)
            (if (calendar) current.readAcademicCalendar(
                requireNotNull(links.calendar) { "UIS academic calendar link missing" }) else null) to
                (if (timetable) current.readTimetable(
                    requireNotNull(links.timetable) { "UIS personal timetable link missing" }) else null)
        }
    }

    fun disconnect() {
        client?.close()
        client = null
        mutableFailure.value = null
        mutableState.value = UisResult.DISCONNECTED
    }

    private suspend fun <T> request(current: UisClient, action: () -> T): T = supervisorScope {
        val cancellation = AtomicBoolean(false)
        val worker = async(Dispatchers.IO) { current.withCancellation(cancellation, action) }
        try {
            worker.await()
        } catch (cancelled: CancellationException) {
            // The token also rejects a redirect/follow-up racing with socket cancellation.
            cancellation.set(true)
            current.cancelActiveRequest()
            // The session lock must outlive cookie parsing and writes to download output.
            withContext(NonCancellable) { worker.join() }
            throw cancelled
        }
    }

    private suspend fun execute(current: UisClient, action: () -> UisResult) {
        mutableFailure.value = null
        mutableState.value = UisResult.CONNECTING
        try {
            val result = request(current) { action() }
            if (client === current) {
                mutableFailure.value = current.failure
                mutableState.value = result
                if (result !in listOf(UisResult.CONNECTED, UisResult.SECOND_FACTOR)) {
                    current.close()
                    client = null
                }
            }
        } catch (cancelled: CancellationException) {
            if (client === current) disconnect()
            throw cancelled
        }
    }
}
