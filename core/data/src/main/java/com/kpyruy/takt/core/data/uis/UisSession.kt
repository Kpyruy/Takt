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
import java.util.concurrent.atomic.AtomicBoolean
import java.time.LocalDate
import com.kpyruy.takt.core.model.SemesterPeriod

/** Memory-only: neither UIS cookies nor pending two-factor credentials enter backups. */
class UisSession {
    private var client: UisClient? = null
    private val mutableState = MutableStateFlow(UisResult.DISCONNECTED)
    val state = mutableState.asStateFlow()
    private val mutableFailure = MutableStateFlow<UisFailure?>(null)
    val failure = mutableFailure.asStateFlow()

    suspend fun login(credentials: UniversityCredentials) {
        disconnect()
        val current = UisClient()
        client = current
        execute(current) { current.login(credentials.login, credentials.password) }
    }

    suspend fun submitCode(code: String) {
        val current = client ?: return
        if (mutableState.value != UisResult.SECOND_FACTOR) return
        execute(current) { current.submitCode(code) }
    }

    suspend fun check() {
        val current = client ?: return
        if (mutableState.value != UisResult.CONNECTED) return
        execute(current) { current.checkSession() }
    }

    internal suspend fun readStudyPlan(): UisStudyPlan {
        val current = client ?: error("UIS session is not connected")
        check(mutableState.value == UisResult.CONNECTED)
        return withContext(Dispatchers.IO) { current.readStudyPlan() }
    }

    internal suspend fun readStudyResults(studyId: String, periodId: String): Map<String, UisStudyResult> {
        val current = client ?: error("UIS session is not connected")
        check(mutableState.value == UisResult.CONNECTED)
        return withContext(Dispatchers.IO) { current.readStudyResults(studyId, periodId) }
    }

    internal suspend fun readCourseLessons(code: String): List<UisTimetableItem> {
        val current = client ?: error("UIS session is not connected")
        check(mutableState.value == UisResult.CONNECTED)
        return withContext(Dispatchers.IO) { current.readCourseLessons(code) }
    }

    internal suspend fun readCourseTeachers(course: com.kpyruy.takt.core.model.Course, refresh: Boolean): List<com.kpyruy.takt.core.model.CourseTeacher> {
        val current = client ?: throw com.kpyruy.takt.core.data.TeachingSessionUnavailable()
        check(mutableState.value == UisResult.CONNECTED)
        return try { withContext(Dispatchers.IO) { current.readCourseTeachers(
            course.syllabusUrl?.let(UisTeachingLinks::subjectId) ?: course.id.substringAfterLast(':'),
            course.code,
            course.id.takeIf { it.matches(Regex("uis:[1-9][0-9]*:[1-9][0-9]*")) }?.split(':')?.get(1),
            refresh) } }
        catch (expired: com.kpyruy.takt.core.data.TeachingAuthenticationRequired) { if (client === current) disconnect(); throw expired }
    }
    internal suspend fun readCourseDocuments(code: String, activeCodes: Set<String>, refresh: Boolean): com.kpyruy.takt.core.model.CourseDocumentListing {
        val current = client ?: throw com.kpyruy.takt.core.data.TeachingSessionUnavailable()
        check(mutableState.value == UisResult.CONNECTED)
        return try { withContext(Dispatchers.IO) { current.readCourseDocuments(code, activeCodes, refresh) } }
        catch (expired: com.kpyruy.takt.core.data.TeachingAuthenticationRequired) { if (client === current) disconnect(); throw expired }
    }
    internal suspend fun downloadCourseDocument(document: com.kpyruy.takt.core.model.CourseDocumentFile, output: java.io.OutputStream): com.kpyruy.takt.core.model.DownloadedCourseDocument {
        val current = client ?: throw com.kpyruy.takt.core.data.TeachingSessionUnavailable()
        check(mutableState.value == UisResult.CONNECTED)
        return supervisorScope {
            val transferCancelled = AtomicBoolean(false)
            val worker = async(Dispatchers.IO) { current.downloadCourseDocument(document, output, transferCancelled) }
            try {
                worker.await()
            } catch (cancelled: CancellationException) {
                transferCancelled.set(true)
                current.cancelActiveRequest()
                // Keep the caller's account/repository locks until no worker can write to its output.
                withContext(NonCancellable) { worker.join() }
                throw cancelled
            } catch (expired: com.kpyruy.takt.core.data.TeachingAuthenticationRequired) {
                if (client === current) disconnect()
                throw expired
            }
        }
    }

    internal suspend fun readTeacherProfile(personId: String, language: com.kpyruy.takt.core.model.AppLanguage): com.kpyruy.takt.core.model.TeacherProfile {
        val current = client ?: throw com.kpyruy.takt.core.data.TeachingSessionUnavailable()
        check(mutableState.value == UisResult.CONNECTED)
        return try { withContext(Dispatchers.IO) { current.readTeacherProfile(personId, language) } }
        catch (expired: com.kpyruy.takt.core.data.TeachingAuthenticationRequired) { if (client === current) disconnect(); throw expired }
    }
    internal suspend fun readTeacherPhoto(personId: String): ByteArray? {
        val current = client ?: return null
        if (mutableState.value != UisResult.CONNECTED) return null
        return withContext(Dispatchers.IO) { current.readTeacherPhoto(personId) }
    }

    internal suspend fun readStudyContext(studyId: String, periodId: String): Pair<SemesterPeriod, List<com.kpyruy.takt.core.data.uis.UisTimetableItem>> {
        val current = client ?: error("UIS session is not connected")
        check(mutableState.value == UisResult.CONNECTED)
        return withContext(Dispatchers.IO) {
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
    ): Pair<SemesterPeriod?, List<UisTimetableItem>?> {
        if (!calendar && !timetable) return null to null
        val current = client ?: error("UIS session is not connected")
        check(mutableState.value == UisResult.CONNECTED)
        return withContext(Dispatchers.IO) {
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

    private suspend fun execute(current: UisClient, action: () -> UisResult) {
        mutableFailure.value = null
        mutableState.value = UisResult.CONNECTING
        try {
            val result = withContext(Dispatchers.IO) { action() }
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
