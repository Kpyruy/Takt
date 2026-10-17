package com.kpyruy.takt.core.data

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CancellationException
import com.kpyruy.takt.core.data.uis.UisResult
import com.kpyruy.takt.core.data.uis.UisStudyPlan
import com.kpyruy.takt.core.data.uis.UisStudyResult
import com.kpyruy.takt.core.data.uis.UisTimetableItem
import com.kpyruy.takt.core.model.Course
import com.kpyruy.takt.core.model.CourseStatus
import com.kpyruy.takt.core.model.CourseNameLanguage
import com.kpyruy.takt.core.model.ScheduleRule
import com.kpyruy.takt.core.model.OneOffScheduleEvent
import com.kpyruy.takt.core.model.OneOffScheduleEventType
import com.kpyruy.takt.core.model.ScheduleRecurrence
import com.kpyruy.takt.core.model.SemesterPeriod
import com.kpyruy.takt.core.model.PassFailResult
import java.io.IOException
import java.nio.charset.StandardCharsets.UTF_8
import java.util.UUID
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

class UniversityCredentials(val login: String, val password: String) {
    override fun toString() = "UniversityCredentials([redacted])"
}

/** Credentials stay on this device and are never included in Takt's study-data backup. */
data class UisImportProgress(
    val running: Boolean = false,
    val importedCourses: Int? = null,
    val earnedCredits: Int? = null,
    val requiredCredits: Int? = null,
    val error: String? = null,
)

data class UisSyncActionState(val running: Boolean = false, val message: String? = null, val error: String? = null)

data class UisSyncPreview(
    val courses: List<Course>,
    val localCourses: List<Course>,
    val semester: Int,
    val period: SemesterPeriod?,
    val localPeriod: SemesterPeriod?,
    val rules: List<ScheduleRule>,
    val oneOffEvents: List<OneOffScheduleEvent>,
    val localRules: List<ScheduleRule>,
    val localOneOffEvents: List<OneOffScheduleEvent>,
    val earnedCredits: Int?,
    val requiredCredits: Int?,
    val localEarnedCredits: Int?,
    val localRequiredCredits: Int?,
    val availableSections: Set<UisSyncSection> = UisSyncSection.entries.toSet(),
    val associatedCourseIds: Set<String> = emptySet(),
)

enum class UisAutoSyncOutcome { NOT_DUE, NEEDS_DEVICE_AUTH, SECOND_FACTOR, REVIEW_READY, APPLIED, FAILED }

private data class UisProgressBaseline(
    val statuses: Map<String, String> = emptyMap(),
    val passFailResults: Map<String, String> = emptyMap(),
    val earnedCredits: Int? = null,
    val requiredCredits: Int? = null,
)

class UniversityAccountRepository(
    context: Context,
    private val studyPlanRepository: StudyPlanRepository? = null,
    private val scheduleRepository: ScheduleRepository? = null,
    val settingsRepository: AppSettingsRepository? = null,
    teachingStore: TeachingStore = TeachingStore(),
) {
    val session = com.kpyruy.takt.core.data.uis.UisSession()
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private val activeState = MutableStateFlow(preferences.contains(CIPHERTEXT))
    val hasAccount = activeState.asStateFlow()
    private val mutableImportProgress = MutableStateFlow(UisImportProgress(
        earnedCredits = preferences.getInt(EARNED_CREDITS, -1).takeIf { it >= 0 },
        requiredCredits = preferences.getInt(REQUIRED_CREDITS, -1).takeIf { it >= 0 },
    ))
    val importProgress = mutableImportProgress.asStateFlow()
    private val mutableLastCheckOutcome = MutableStateFlow<UisAutoSyncOutcome?>(null)
    val lastCheckOutcome = mutableLastCheckOutcome.asStateFlow()
    private val mutablePreview = MutableStateFlow<UisSyncPreview?>(null)
    val syncPreview = mutablePreview.asStateFlow()
    private val mutableSyncAction = MutableStateFlow(UisSyncActionState())
    val syncAction = mutableSyncAction.asStateFlow()
    private val actionMutex = Mutex()
    private var retryAction: Pair<String, suspend () -> Unit>? = null
    suspend fun retrySyncAction() { retryAction?.let { performPreviewAction(it.first, it.second) } }

    private suspend fun performPreviewAction(success: String, action: suspend () -> Unit) {
        if (!actionMutex.tryLock()) return
        mutableSyncAction.value = UisSyncActionState(running = true)
        retryAction = success to action
        try {
            action()
            retryAction = null
            mutableSyncAction.value = UisSyncActionState(message = success)
        } catch (cancelled: CancellationException) {
            mutableSyncAction.value = UisSyncActionState()
            throw cancelled
        } catch (error: Exception) {
            mutableSyncAction.value = UisSyncActionState(error = error.message ?: "Не вдалося зберегти зміни UIS. Повтори перевірку.")
        } finally { actionMutex.unlock() }
    }

    /** Call from an unlocked app. The UIS session and credentials never enter a backup. */
    suspend fun loadLessonOptions(code: String): List<UisLessonOption> = autoMutex.withLock {
        check(hasAccount.value) { "UIS account is not connected" }
        if (session.state.value == UisResult.CONNECTED) session.check()
        if (session.state.value != UisResult.CONNECTED) {
            val credentials = readAfterAuthentication() ?: error("UIS credentials are unavailable")
            session.login(credentials)
        }
        check(session.state.value == UisResult.CONNECTED) { "UIS login needs attention" }
        session.readCourseLessons(code).map { item ->
            UisLessonOption(item.key, item.day, item.start, item.end, item.room, item.lessonType, item.date)
        }
    }
    private val teachingCache = CachedCourseTeachingRepository(
        store = teachingStore,
        loadTeachers = { course, refresh -> autoMutex.withLock {
            ensureTeachingSession()
            session.readCourseTeachers(course, refresh)
        } },
        loadProfile = { id, language -> autoMutex.withLock {
            ensureTeachingSession()
            session.readTeacherProfile(id, language)
        } },
        loadPhoto = { id -> autoMutex.withLock { session.readTeacherPhoto(id) } },
    )
    val teachingRepository: CourseTeachingRepository get() = teachingCache
    private val documentsCache = CachedCourseDocumentsRepository(
        load = { course, activeCodes, refresh -> autoMutex.withLock {
            ensureTeachingSession()
            session.readCourseDocuments(course.code, activeCodes, refresh)
        } },
        stream = { document, output -> autoMutex.withLock {
            ensureTeachingSession()
            session.downloadCourseDocument(document, output)
        } },
    )
    val courseDocumentsRepository: CourseDocumentsRepository get() = documentsCache


    /** Called only from the unlocked app; keystore requirements are unchanged. */
    private suspend fun ensureTeachingSession() {
        if (!hasAccount.value) throw TeachingAuthenticationRequired()
        if (session.state.value != UisResult.CONNECTED) {
            val credentials = try { readAfterAuthentication() }
                catch (_: Exception) { throw TeachingAuthenticationRequired() }
                ?: throw TeachingAuthenticationRequired()
            session.login(credentials)
        }
        if (session.state.value != UisResult.CONNECTED) throw TeachingSessionUnavailable()
    }

    private var pendingSubjectByEventId: Map<String, String> = emptyMap()
    private var pendingAutoSections: Set<UisSyncSection>? = null
    private val autoMutex = Mutex()

    suspend fun setFirstCourseNameChoice(language: CourseNameLanguage) {
        require(language != CourseNameLanguage.FOLLOW_APP)
        settingsRepository?.setUkrainianCourseNameFallback(language)
        settingsRepository?.setCourseNameLanguage(language)
    }

    suspend fun signIn(credentials: UniversityCredentials) {
        pendingAutoSections = null
        session.login(credentials)
        if (session.state.value == UisResult.CONNECTED) preferences.edit().remove(AUTO_INVALID_DAY).apply()
        importAfterLogin()
    }

    suspend fun submitCode(code: String) {
        session.submitCode(code)
        if (session.state.value == UisResult.CONNECTED && pendingAutoSections != null) {
            val sections = pendingAutoSections.orEmpty()
            pendingAutoSections = null
            fetchAutomatic(sections, LocalDate.now())
        } else importAfterLogin()
    }

    suspend fun retryImport() = importAfterLogin(manual = true)

    suspend fun checkAndImport() {
        session.check()
        importAfterLogin(manual = true)
    }

    suspend fun checkProgressNow(): UisAutoSyncOutcome =
        fetchSelectedManually(setOf(UisSyncSection.PROGRESS))

    suspend fun checkTimetableNow(): UisAutoSyncOutcome =
        fetchSelectedManually(setOf(UisSyncSection.TIMETABLE))

    private suspend fun fetchSelectedManually(sections: Set<UisSyncSection>): UisAutoSyncOutcome {
        if (mutablePreview.value != null) return UisAutoSyncOutcome.REVIEW_READY
        session.check()
        if (session.state.value != UisResult.CONNECTED) return UisAutoSyncOutcome.FAILED
        return fetchAutomatic(sections, LocalDate.now(), applyAllowed = false)
    }

    /** Runs when the owner opens an unlocked app. Keystore access never runs in a background worker. */
    suspend fun runAutoSyncIfDue(today: LocalDate = LocalDate.now()): UisAutoSyncOutcome = autoMutex.withLock {
        if (!hasAccount.value || !preferences.getBoolean(INITIAL_IMPORT_DONE, false) ||
            mutablePreview.value != null || studyPlanRepository == null || settingsRepository == null) {
            return@withLock UisAutoSyncOutcome.NOT_DUE
        }
        if (preferences.getLong(AUTO_INVALID_DAY, Long.MIN_VALUE) == today.toEpochDay()) {
            return@withLock UisAutoSyncOutcome.FAILED
        }
        if (session.state.value == UisResult.SECOND_FACTOR) return@withLock UisAutoSyncOutcome.SECOND_FACTOR
        if (session.state.value == UisResult.CONNECTING) return@withLock UisAutoSyncOutcome.NOT_DUE
        val localCourses = studyPlanRepository.observeCourses().first()
        val settings = settingsRepository.settings.value
        val currentSemester = settings.effectiveCurrentSemester(localCourses)
        val currentPeriod = currentSemester?.let(settings.semesterPeriods::get)
        val checked = UisSyncSection.entries.mapNotNull { section ->
            preferences.getLong("uis_auto_last_${section.name}", Long.MIN_VALUE)
                .takeIf { it != Long.MIN_VALUE }?.let { section to LocalDate.ofEpochDay(it) }
        }.toMap()
        val due = UisAutoSyncPlanner.due(today, settings.uisAutoSync, checked,
            currentPeriod?.studyStart, currentPeriod?.studyEnd, currentPeriod?.examEnd)
        if (due.isEmpty()) return@withLock UisAutoSyncOutcome.NOT_DUE
        if (session.state.value == UisResult.CONNECTED) {
            session.check()
            if (session.state.value == UisResult.UNAVAILABLE ||
                session.state.value == UisResult.UNEXPECTED_RESPONSE) return@withLock UisAutoSyncOutcome.FAILED
        }
        if (session.state.value != UisResult.CONNECTED) {
            val credentials = runCatching { readAfterAuthentication() }.getOrNull()
                ?: return@withLock UisAutoSyncOutcome.NEEDS_DEVICE_AUTH
            session.login(credentials)
            if (session.state.value == UisResult.INVALID_CREDENTIALS) {
                preferences.edit().putLong(AUTO_INVALID_DAY, today.toEpochDay()).apply()
            }
            if (session.state.value == UisResult.SECOND_FACTOR) {
                pendingAutoSections = due
                return@withLock UisAutoSyncOutcome.SECOND_FACTOR
            }
            if (session.state.value != UisResult.CONNECTED) return@withLock UisAutoSyncOutcome.FAILED
        }
        fetchAutomatic(due, today)
    }

    private suspend fun fetchAutomatic(sections: Set<UisSyncSection>, today: LocalDate,
        applyAllowed: Boolean = true): UisAutoSyncOutcome {
        val study = studyPlanRepository ?: return UisAutoSyncOutcome.FAILED
        val settingsRepo = settingsRepository ?: return UisAutoSyncOutcome.FAILED
        val previous = mutableImportProgress.value
        mutableImportProgress.value = previous.copy(running = true, error = null)
        return try {
            val plan = session.readStudyPlan()
            val results = if (UisSyncSection.PROGRESS in sections) session.readStudyResults(plan.studyId, plan.periodId)
                else emptyMap()
            val courses = plan.toCourses(results)
            settingsRepo.setUisCurrentSemester(plan.currentSemester)
            if (applyAllowed && (UisSyncSection.SUBJECTS in sections ||
                UisSyncSection.PROGRESS in sections && settingsRepo.settings.value.uisAutoSync.applyProgressAutomatically)) {
                val localForImport = study.observeCourses().first()
                val prepared = prepareAutomaticCourseImports(courses, localForImport, UisSyncSection.SUBJECTS in sections)
                try { study.upsertImportedCourses(prepared) }
                catch (_: AmbiguousUisCourse) { /* Keep all local records and offer explicit selection in review. */ }
            }
            val localBefore = study.observeCourses().first()
            val previousSemester = settingsRepo.settings.value.effectiveCurrentSemester(courses)
                ?: courses.firstOrNull { it.status == CourseStatus.ENROLLED }?.semester
                ?: courses.maxOfOrNull { it.semester } ?: 1
            val previousPeriod = settingsRepo.settings.value.semesterPeriods[previousSemester]
            val (period, timetable) = session.readSelectedContext(plan.studyId, plan.periodId,
                calendar = UisSyncSection.PERIODS in sections,
                timetable = UisSyncSection.TIMETABLE in sections)
            val nextStudyStart = period?.studyStart
            val previousExamEnd = previousPeriod?.examEnd
            val semester = plan.currentSemester ?: if (nextStudyStart != null && previousExamEnd != null &&
                nextStudyStart > previousExamEnd && courses.any { it.semester == previousSemester + 1 }) {
                previousSemester + 1
            } else previousSemester
            val localPeriod = settingsRepo.settings.value.semesterPeriods[semester]
            val ids = courses.mapNotNull { course ->
                localBefore.matchingUisCourse(course)?.id?.let { course.code to it }
            }.toMap()
            val bySubject = plan.courses.mapNotNull { source ->
                ids[source.code]?.let { source.subjectId to it }
            }.toMap()
            pendingSubjectByEventId = timetable.orEmpty().mapNotNull { item ->
                item.subjectId?.let { subject ->
                    ("uis:" + UUID.nameUUIDFromBytes(item.key.toByteArray(UTF_8))) to subject
                }
            }.toMap()
            val rules = timetable.orEmpty().filter { it.date == null }.map { item ->
                ScheduleRule("uis:" + UUID.nameUUIDFromBytes(item.key.toByteArray(UTF_8)),
                    item.subjectId?.let(bySubject::get), item.title, item.day, item.start, item.end,
                    ScheduleRecurrence.WEEKLY, item.room, item.lessonType)
            }
            val oneOffs = timetable.orEmpty().filter { it.date != null }.map { item ->
                OneOffScheduleEvent("uis:" + UUID.nameUUIDFromBytes(item.key.toByteArray(UTF_8)),
                    item.subjectId?.let(bySubject::get), item.title, item.date!!, item.start, item.end,
                    item.room, OneOffScheduleEventType.BLOCK_ACTION, item.lessonType)
            }
            val baseline = readProgressBaseline()
            val nextStatuses = baseline.statuses.toMutableMap()
            val nextResults = baseline.passFailResults.toMutableMap()
            if (applyAllowed && UisSyncSection.PROGRESS in sections &&
                settingsRepo.settings.value.uisAutoSync.applyProgressAutomatically) {
                courses.forEach { remote ->
                    localBefore.matchingUisCourse(remote)?.let { local ->
                        val localResult = local.passFailResult?.name ?: "none"
                        study.setOfficialResult(local.id, remote.officialGrade, remote.fulfilledOn)
                        if (UisProgressAutoMerge.shouldApply(local.status.storageValue,
                                remote.status.storageValue, baseline.statuses[remote.code],
                                localResult.takeIf { remote.gradingType == com.kpyruy.takt.core.model.CourseGradingType.PASS_FAIL },
                                (remote.passFailResult?.name ?: "none").takeIf {
                                    remote.gradingType == com.kpyruy.takt.core.model.CourseGradingType.PASS_FAIL
                                },
                                baseline.passFailResults[remote.code])) {
                            study.updateStatus(local.id, remote.status)
                            if (remote.gradingType == com.kpyruy.takt.core.model.CourseGradingType.PASS_FAIL) {
                                study.setPassFailResult(local.id, remote.passFailResult)
                                nextResults[remote.code] = remote.passFailResult?.name ?: "none"
                            }
                            nextStatuses[remote.code] = remote.status.storageValue
                        } else if (UisProgressAutoMerge.canRemember(local.status.storageValue,
                                remote.status.storageValue)) {
                            nextStatuses[remote.code] = remote.status.storageValue
                            if (local.passFailResult == remote.passFailResult) {
                                nextResults[remote.code] = localResult
                            }
                        }
                    }
                }
                val localEarned = preferences.getInt(EARNED_CREDITS, -1).takeIf { it >= 0 }
                val localRequired = preferences.getInt(REQUIRED_CREDITS, -1).takeIf { it >= 0 }
                if (baseline.earnedCredits != null && baseline.requiredCredits != null &&
                    localEarned == baseline.earnedCredits && localRequired == baseline.requiredCredits) {
                    saveCredits(plan.earnedCredits, plan.requiredCredits)
                    writeProgressBaseline(baseline.copy(statuses = nextStatuses, passFailResults = nextResults,
                        earnedCredits = plan.earnedCredits, requiredCredits = plan.requiredCredits))
                } else {
                    writeProgressBaseline(baseline.copy(statuses = nextStatuses, passFailResults = nextResults,
                        earnedCredits = if (localEarned == plan.earnedCredits) plan.earnedCredits else baseline.earnedCredits,
                        requiredCredits = if (localRequired == plan.requiredCredits) plan.requiredCredits else baseline.requiredCredits))
                }
            } else if (UisSyncSection.PROGRESS in sections) {
                courses.forEach { remote ->
                    localBefore.matchingUisCourse(remote)?.takeIf { it.status == remote.status }
                        ?.let {
                            nextStatuses[remote.code] = remote.status.storageValue
                            if (it.passFailResult == remote.passFailResult) {
                                nextResults[remote.code] = it.passFailResult?.name ?: "none"
                            }
                        }
                }
                writeProgressBaseline(baseline.copy(statuses = nextStatuses, passFailResults = nextResults))
            }
            val localAfter = study.observeCourses().first()
            val localRules = if (UisSyncSection.TIMETABLE in sections) scheduleRepository?.observeRules()?.first().orEmpty()
                else emptyList()
            val localEvents = if (UisSyncSection.TIMETABLE in sections) scheduleRepository?.observeOneOffEvents()?.first().orEmpty()
                else emptyList()
            val review = UisSyncPreview(courses, localAfter, semester, period, localPeriod,
                rules, oneOffs, localRules, localEvents, plan.earnedCredits, plan.requiredCredits,
                preferences.getInt(EARNED_CREDITS, -1).takeIf { it >= 0 },
                preferences.getInt(REQUIRED_CREDITS, -1).takeIf { it >= 0 }, sections)
            markChecked(sections, today)
            mutableImportProgress.value = UisImportProgress(
                earnedCredits = preferences.getInt(EARNED_CREDITS, -1).takeIf { it >= 0 },
                requiredCredits = preferences.getInt(REQUIRED_CREDITS, -1).takeIf { it >= 0 })
            mutablePreview.value = review
            refreshLocalPreview()
            val changed = mutablePreview.value?.hasRelevantDifferences() == true
            if (!changed) mutablePreview.value = null
            (if (changed) UisAutoSyncOutcome.REVIEW_READY else UisAutoSyncOutcome.APPLIED)
                .also { mutableLastCheckOutcome.value = it }
        } catch (cancelled: CancellationException) {
            mutableImportProgress.value = previous
            throw cancelled
        } catch (error: Exception) {
            mutableImportProgress.value = previous.copy(running = false,
                error = if (error is InvalidUisCourseId) error.message else "Не вдалося автоматично перевірити UIS. Спробуй вручну.")
            UisAutoSyncOutcome.FAILED.also { mutableLastCheckOutcome.value = it }
        }
    }

    private fun markChecked(sections: Set<UisSyncSection>, date: LocalDate) {
        val edit = preferences.edit()
        sections.forEach { edit.putLong("uis_auto_last_${it.name}", date.toEpochDay()) }
        edit.apply()
    }

    private suspend fun importAfterLogin(manual: Boolean = false) {
        if (session.state.value != UisResult.CONNECTED || studyPlanRepository == null) return
        if (!manual && preferences.getBoolean(INITIAL_IMPORT_DONE, false)) return
        val previous = mutableImportProgress.value
        mutableImportProgress.value = UisImportProgress(running = true)
        try {
            val plan = session.readStudyPlan()
            val courses = plan.toCourses(session.readStudyResults(plan.studyId, plan.periodId))
            settingsRepository?.setUisCurrentSemester(plan.currentSemester)
            val (period, timetable) = session.readStudyContext(plan.studyId, plan.periodId)
            val localCourses = studyPlanRepository.observeCourses().first()
            val localRules = scheduleRepository?.observeRules()?.first().orEmpty()
            val localOneOffs = scheduleRepository?.observeOneOffEvents()?.first().orEmpty()
            val semester = settingsRepository?.settings?.value?.effectiveCurrentSemester(courses)
                ?: courses.firstOrNull { it.status == CourseStatus.ENROLLED }?.semester
                ?: courses.maxOfOrNull { it.semester } ?: 1
            val localPeriod = settingsRepository?.settings?.value?.semesterPeriods?.get(semester)
            val first = !preferences.getBoolean(INITIAL_IMPORT_DONE, false)
            val importCourses = first
            val ids = courses.mapNotNull { course ->
                (course.id.takeIf { importCourses }
                    ?: localCourses.matchingUisCourse(course)?.id)?.let { course.code to it }
            }.toMap()
            val bySubject = plan.courses.mapNotNull { source ->
                ids[source.code]?.let { source.subjectId to it }
            }.toMap()
            pendingSubjectByEventId = timetable.mapNotNull { item ->
                item.subjectId?.let { subject ->
                    ("uis:" + UUID.nameUUIDFromBytes(item.key.toByteArray(UTF_8))) to subject
                }
            }.toMap()
            val rules = timetable.filter { it.date == null }.map { item ->
                ScheduleRule("uis:" + UUID.nameUUIDFromBytes(item.key.toByteArray(UTF_8)),
                    item.subjectId?.let(bySubject::get), item.title, item.day, item.start, item.end,
                    ScheduleRecurrence.WEEKLY, item.room, item.lessonType)
            }
            val oneOffs = timetable.filter { it.date != null }.map { item ->
                OneOffScheduleEvent("uis:" + UUID.nameUUIDFromBytes(item.key.toByteArray(UTF_8)),
                    item.subjectId?.let(bySubject::get), item.title, item.date!!, item.start, item.end,
                    item.room, OneOffScheduleEventType.BLOCK_ACTION, item.lessonType)
            }
            val count = if (importCourses) {
                try { studyPlanRepository.upsertImportedCourses(courses) }
                catch (_: AmbiguousUisCourse) { 0 }
            } else 0
            val importedIds = studyPlanRepository.observeCourses().first().map { it.id }.toSet()
            val preparedRules = rules.map { it.copy(courseId = it.courseId?.takeIf(importedIds::contains)) }
            val preparedOneOffs = oneOffs.map { it.copy(courseId = it.courseId?.takeIf(importedIds::contains)) }
            if (first && localRules.isEmpty() && localOneOffs.isEmpty() &&
                (preparedRules.map { it.id to it.courseId } + preparedOneOffs.map { it.id to it.courseId })
                    .all { (id, courseId) -> pendingSubjectByEventId[id] == null || courseId != null }) {
                scheduleRepository?.replaceSchedule(preparedRules, preparedOneOffs)
            }
            if (first && (localPeriod == null || localPeriod.isEmpty)) {
                val current = settingsRepository?.settings?.value?.semesterPeriods.orEmpty()
                settingsRepository?.setSemesterPeriods(current + (semester to period))
            }
            if (count > 0) saveCredits(plan.earnedCredits, plan.requiredCredits)
            val baseline = readProgressBaseline()
            val known = baseline.statuses.toMutableMap()
            val knownResults = baseline.passFailResults.toMutableMap()
            courses.forEach { remote ->
                val local = if (count > 0) remote else localCourses.matchingUisCourse(remote)
                if (local?.status == remote.status) known[remote.code] = remote.status.storageValue
                if (local?.passFailResult == remote.passFailResult) {
                    knownResults[remote.code] = remote.passFailResult?.name ?: "none"
                }
            }
            writeProgressBaseline(baseline.copy(statuses = known, passFailResults = knownResults,
                earnedCredits = if (count > 0) plan.earnedCredits else baseline.earnedCredits,
                requiredCredits = if (count > 0) plan.requiredCredits else baseline.requiredCredits))
            preferences.edit().putBoolean(INITIAL_IMPORT_DONE, true).apply()
            markChecked(UisSyncSection.entries.toSet(), LocalDate.now())
            mutablePreview.value = UisSyncPreview(courses, localCourses, semester, period, localPeriod,
                rules, oneOffs, localRules, localOneOffs, plan.earnedCredits, plan.requiredCredits,
                preferences.getInt(EARNED_CREDITS, -1).takeIf { it >= 0 },
                preferences.getInt(REQUIRED_CREDITS, -1).takeIf { it >= 0 })
            refreshLocalPreview()
            val hasChanges = mutablePreview.value?.hasRelevantDifferences() == true
            if (!hasChanges) mutablePreview.value = null
            mutableLastCheckOutcome.value = if (hasChanges) UisAutoSyncOutcome.REVIEW_READY
                else UisAutoSyncOutcome.APPLIED
            mutableImportProgress.value = UisImportProgress(
                importedCourses = count.takeIf { it > 0 },
                earnedCredits = preferences.getInt(EARNED_CREDITS, -1).takeIf { it >= 0 },
                requiredCredits = preferences.getInt(REQUIRED_CREDITS, -1).takeIf { it >= 0 },
            )
        } catch (cancelled: CancellationException) {
            mutableImportProgress.value = previous
            throw cancelled
        } catch (error: Exception) {
            mutableImportProgress.value = previous.copy(running = false, error = when (error) {
                is InvalidUisCourseId -> error.message
                is IOException -> "Не вдалося завантажити дані UIS. Перевір з’єднання."
                is IllegalStateException, is IllegalArgumentException -> "Не вдалося розпізнати навчальний план, календар або розклад UIS."
                else -> "Не вдалося імпортувати дані UIS."
            })
        }
    }

    suspend fun applyCourse(courseId: String, selectedLocalId: String? = null) = performPreviewAction("Предмет оновлено") {
        val preview = mutablePreview.value ?: return@performPreviewAction
        val remote = preview.courses.firstOrNull { it.id == courseId } ?: return@performPreviewAction
        studyPlanRepository?.upsertImportedCourses(listOf(remote),
            selectedLocalId?.let { mapOf(courseId to it) }.orEmpty())
        updateBaselineProgress(remote.code, remote.status, remote.passFailResult)
        refreshLocalPreview()
    }

    suspend fun applyProgress(courseId: String) = performPreviewAction("Прогрес оновлено") {
        val preview = mutablePreview.value ?: return@performPreviewAction
        if (UisSyncSection.PROGRESS !in preview.availableSections) return@performPreviewAction
        val remote = preview.courses.firstOrNull { it.id == courseId } ?: return@performPreviewAction
        if (preview.localCourses.matchingUisCourse(remote) == null) return@performPreviewAction
        studyPlanRepository?.upsertImportedCourses(listOf(remote))
        studyPlanRepository?.setOfficialResult(remote.id, remote.officialGrade, remote.fulfilledOn)
        updateBaselineProgress(remote.code, remote.status, remote.passFailResult)
        refreshLocalPreview()
    }

    suspend fun applyAllProgress() {
        val preview = mutablePreview.value ?: return
        preview.courses.forEach { applyProgress(it.id) }
        applyCredits()
    }

    fun applyCredits() {
        val preview = mutablePreview.value ?: return
        if (UisSyncSection.PROGRESS !in preview.availableSections) return
        saveCredits(preview.earnedCredits, preview.requiredCredits)
        val baseline = readProgressBaseline()
        writeProgressBaseline(baseline.copy(earnedCredits = preview.earnedCredits,
            requiredCredits = preview.requiredCredits))
        mutableImportProgress.value = mutableImportProgress.value.copy(
            earnedCredits = preview.earnedCredits, requiredCredits = preview.requiredCredits)
        mutablePreview.value = preview.copy(localEarnedCredits = preview.earnedCredits,
            localRequiredCredits = preview.requiredCredits)
    }

    suspend fun applyPeriod() {
        val preview = mutablePreview.value ?: return
        if (UisSyncSection.PERIODS !in preview.availableSections || preview.period == null) return
        val current = settingsRepository?.settings?.value?.semesterPeriods.orEmpty()
        settingsRepository?.setSemesterPeriods(current + (preview.semester to preview.period))
        refreshLocalPreview()
    }

    suspend fun addRule(id: String) = performPreviewAction("Заняття додано") {
        if (UisSyncSection.TIMETABLE !in mutablePreview.value?.availableSections.orEmpty()) return@performPreviewAction
        refreshLocalPreview()
        mutablePreview.value?.rules?.firstOrNull { it.id == id }?.let {
            validateTimetableLinks(listOf(it.id to it.courseId))
            scheduleRepository?.upsertRule(it)
        }
        refreshLocalPreview()
    }

    suspend fun addOneOff(id: String) = performPreviewAction("Заняття додано") {
        if (UisSyncSection.TIMETABLE !in mutablePreview.value?.availableSections.orEmpty()) return@performPreviewAction
        refreshLocalPreview()
        mutablePreview.value?.oneOffEvents?.firstOrNull { it.id == id }?.let {
            validateTimetableLinks(listOf(it.id to it.courseId))
            scheduleRepository?.upsertOneOffEvent(it)
        }
        refreshLocalPreview()
    }

    suspend fun replaceSchedule() = performPreviewAction("Розклад UIS збережено") {
        refreshLocalPreview()
        val preview = mutablePreview.value ?: return@performPreviewAction
        if (UisSyncSection.TIMETABLE !in preview.availableSections) return@performPreviewAction
        val schedule = scheduleRepository ?: return@performPreviewAction
        validateTimetableLinks(preview.rules.map { it.id to it.courseId } + preview.oneOffEvents.map { it.id to it.courseId })
        schedule.replaceSchedule(preview.rules, preview.oneOffEvents)
        refreshLocalPreview()
    }

    private suspend fun validateTimetableLinks(events: List<Pair<String, String?>>) {
        val ids = studyPlanRepository?.observeCourses()?.first().orEmpty().map { it.id }.toSet()
        events.forEach { (id, courseId) ->
            val subjectId = pendingSubjectByEventId[id]
            val remote = mutablePreview.value?.courses?.firstOrNull { it.id.substringAfterLast(':') == subjectId }
            val code = remote?.code
            require((subjectId == null || remote != null && courseId == remote.id) && (courseId == null || courseId in ids)) {
                "Спочатку імпортуй предмет ${code ?: subjectId.orEmpty()} з UIS, потім повтори додавання розкладу."
            }
        }
    }

    fun dismissPreview() {
        if (mutableSyncAction.value.running) return
        mutablePreview.value = null
        mutableSyncAction.value = UisSyncActionState()
        pendingSubjectByEventId = emptyMap()
        mutableLastCheckOutcome.value = null
    }

    private suspend fun refreshLocalPreview() {
        val current = mutablePreview.value ?: return
        val local = studyPlanRepository?.observeCourses()?.first().orEmpty()
        val canonicalBySubject = current.courses.associateBy { it.id.substringAfterLast(':') }
        suspend fun resolve(eventId: String, previousId: String?): String? {
            pendingSubjectByEventId[eventId]?.let { subject ->
                canonicalBySubject[subject]?.let { remote ->
                    return local.matchingUisCourse(remote)?.id
                }
                return null
            }
            return previousId?.let { studyPlanRepository?.observeCourse(it)?.first()?.id }
        }
        mutablePreview.value = current.copy(
            localCourses = local,
            associatedCourseIds = studyPlanRepository?.importedCourseChoices().orEmpty(),
            courses = current.courses.map { remote ->
                val existing = local.matchingUisCourse(remote)
                remote.copy(officialGrade = remote.officialGrade ?: existing?.officialGrade,
                    fulfilledOn = remote.fulfilledOn ?: existing?.fulfilledOn)
            },
            rules = current.rules.map { it.copy(courseId = resolve(it.id, it.courseId)) },
            oneOffEvents = current.oneOffEvents.map { it.copy(courseId = resolve(it.id, it.courseId)) },
            localPeriod = settingsRepository?.settings?.value?.semesterPeriods?.get(current.semester),
            localRules = scheduleRepository?.observeRules()?.first().orEmpty(),
            localOneOffEvents = scheduleRepository?.observeOneOffEvents()?.first().orEmpty(),
        )
    }

    private fun saveCredits(earned: Int?, required: Int?) {
        preferences.edit().putInt(EARNED_CREDITS, earned ?: -1)
            .putInt(REQUIRED_CREDITS, required ?: -1).apply()
    }

    private fun readProgressBaseline(): UisProgressBaseline = runCatching {
        val raw = preferences.getString(PROGRESS_BASELINE, null) ?: return@runCatching UisProgressBaseline()
        val json = JSONObject(raw)
        val statuses = json.optJSONObject("statuses") ?: JSONObject()
        val results = json.optJSONObject("results") ?: JSONObject()
        UisProgressBaseline(statuses.keys().asSequence().associateWith { statuses.getString(it) },
            results.keys().asSequence().associateWith { results.getString(it) },
            json.optInt("earned", -1).takeIf { it >= 0 },
            json.optInt("required", -1).takeIf { it >= 0 })
    }.getOrDefault(UisProgressBaseline())

    private fun writeProgressBaseline(baseline: UisProgressBaseline) {
        val statuses = JSONObject()
        baseline.statuses.forEach { (code, status) -> statuses.put(code, status) }
        val results = JSONObject()
        baseline.passFailResults.forEach { (code, result) -> results.put(code, result) }
        val json = JSONObject().put("statuses", statuses).put("results", results)
            .put("earned", baseline.earnedCredits ?: -1)
            .put("required", baseline.requiredCredits ?: -1)
        preferences.edit().putString(PROGRESS_BASELINE, json.toString()).apply()
    }

    private fun updateBaselineProgress(code: String, status: CourseStatus, result: PassFailResult?) {
        val baseline = readProgressBaseline()
        writeProgressBaseline(baseline.copy(
            statuses = baseline.statuses + (code to status.storageValue),
            passFailResults = baseline.passFailResults + (code to (result?.name ?: "none"))))
    }

    /** Call only after a successful device authentication. */
    fun save(login: String, password: String) {
        session.disconnect()
        pendingAutoSections = null
        dismissPreview()
        val cleanLogin = login.trim()
        require(cleanLogin.isNotEmpty() && cleanLogin.length <= 254)
        require(password.isNotEmpty() && password.length <= 1024)
        val sameAccount = runCatching { readAfterAuthentication()?.login == cleanLogin }.getOrDefault(false)
        val plain = JSONObject().put("login", cleanLogin).put("password", password)
            .toString().toByteArray(StandardCharsets.UTF_8)
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey())
            val ciphertext = cipher.doFinal(plain)
            val edit = preferences.edit()
                .putString(CIPHERTEXT, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
                .putString(IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            if (!sameAccount) {
                teachingCache.clear()
                documentsCache.clear()
                edit.remove(EARNED_CREDITS).remove(REQUIRED_CREDITS).remove(INITIAL_IMPORT_DONE)
                    .remove(PROGRESS_BASELINE).remove(AUTO_INVALID_DAY)
                UisSyncSection.entries.forEach { edit.remove("uis_auto_last_${it.name}") }
            }
            check(edit.commit()) { "Could not save UIS credentials" }
            if (!sameAccount) settingsRepository?.setUisCurrentSemester(null)
            activeState.value = true
            mutableImportProgress.value = if (sameAccount) mutableImportProgress.value.copy(
                running = false, importedCourses = null, error = null,
            ) else UisImportProgress()
        } finally {
            plain.fill(0)
        }
    }

    /** The keystore rejects decryption unless the owner recently authenticated. */
    fun readAfterAuthentication(): UniversityCredentials? {
        val encoded = preferences.getString(CIPHERTEXT, null) ?: return null
        val iv = preferences.getString(IV, null) ?: error("Missing UIS credential IV")
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)))
        val plain = cipher.doFinal(Base64.decode(encoded, Base64.NO_WRAP))
        return try {
            val json = JSONObject(String(plain, StandardCharsets.UTF_8))
            UniversityCredentials(json.getString("login"), json.getString("password"))
        } finally {
            plain.fill(0)
        }
    }

    /** Call only after a successful device authentication. */
    fun remove() {
        teachingCache.clear()
        documentsCache.clear()
        session.disconnect()
        pendingAutoSections = null
        dismissPreview()
        check(preferences.edit().remove(CIPHERTEXT).remove(IV)
            .remove(EARNED_CREDITS).remove(REQUIRED_CREDITS).remove(INITIAL_IMPORT_DONE)
            .remove(PROGRESS_BASELINE).remove(AUTO_INVALID_DAY).apply {
                UisSyncSection.entries.forEach { remove("uis_auto_last_${it.name}") }
            }.commit()) { "Could not remove UIS credentials" }
        keyStore.deleteEntry(KEY_ALIAS)
        settingsRepository?.setUisCurrentSemester(null)
        activeState.value = false
        mutableImportProgress.value = UisImportProgress()
    }

    private fun secretKey(): SecretKey {
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val builder = KeyGenParameterSpec.Builder(
            KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setUserAuthenticationParameters(
                AUTH_SECONDS,
                KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL,
            )
        } else {
            @Suppress("DEPRECATION")
            builder.setUserAuthenticationValidityDurationSeconds(AUTH_SECONDS)
        }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            .apply { init(builder.build()) }.generateKey()
    }

    private companion object {
        const val PREFERENCES = "takt_university_account"
        const val CIPHERTEXT = "credentials"
        const val IV = "iv"
        const val EARNED_CREDITS = "uis_earned_credits"
        const val REQUIRED_CREDITS = "uis_required_credits"
        const val INITIAL_IMPORT_DONE = "uis_initial_import_done"
        const val PROGRESS_BASELINE = "uis_progress_baseline"
        const val AUTO_INVALID_DAY = "uis_auto_invalid_day"
        const val KEY_ALIAS = "takt_uis_credentials_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val AUTH_SECONDS = 30
    }
}

private fun UisStudyPlan.toCourses(results: Map<String, UisStudyResult> = emptyMap()): List<Course> = courses.map { source ->
    Course(
        id = "uis:$studyId:${source.subjectId}",
        code = source.code,
        title = source.title,
        titleEn = source.title,
        titleSk = source.titleSk,
        credits = source.credits,
        semester = source.semester,
        status = source.status,
        requirementType = source.requirementType,
        syllabusUrl = source.syllabusUrl,
        gradingType = source.gradingType,
        passFailResult = if (source.gradingType == com.kpyruy.takt.core.model.CourseGradingType.PASS_FAIL &&
            source.status == CourseStatus.FULFILLED) PassFailResult.PASSED else null,
        officialGrade = results[source.code]?.grade,
        fulfilledOn = results[source.code]?.fulfilledOn,
    )
}

private fun UisSyncPreview.hasRelevantDifferences(): Boolean {
    // ID adoption remains reviewable even when only progress/timetable was fetched.
    if (courses.any { remote -> remote.id !in associatedCourseIds && localCourses.filter { it.matchesUisCourse(remote) }.any { it.id != remote.id } }) return true
    if (UisSyncSection.PROGRESS in availableSections &&
        (earnedCredits != localEarnedCredits || requiredCredits != localRequiredCredits ||
            courses.any { remote -> localCourses.matchingUisCourse(remote)?.let { local ->
                local.status != remote.status || local.passFailResult != remote.passFailResult ||
                    local.officialGrade != (remote.officialGrade ?: local.officialGrade) || local.fulfilledOn != (remote.fulfilledOn ?: local.fulfilledOn)
            } == true })) return true
    if (UisSyncSection.SUBJECTS in availableSections && courses.any { remote ->
            localCourses.matchingUisCourse(remote)?.let { local ->
                local.id != remote.id || local.titleEn != remote.titleEn || local.titleSk != remote.titleSk ||
                    local.credits != remote.credits || local.semester != remote.semester ||
                    local.gradingType != remote.gradingType || local.requirementType != remote.requirementType
            } ?: true
        }) return true
    if (UisSyncSection.PERIODS in availableSections && period != localPeriod) return true
    if (UisSyncSection.TIMETABLE in availableSections &&
        (rules.toSet() != localRules.toSet() || oneOffEvents.toSet() != localOneOffEvents.toSet())) return true
    return false
}
