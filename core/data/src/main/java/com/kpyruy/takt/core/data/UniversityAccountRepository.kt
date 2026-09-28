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
            val courses = plan.toCourses()
            val existingCourses = study.observeCourses().first()
            if (applyAllowed && UisSyncSection.SUBJECTS in sections) {
                val knownCodes = existingCourses.map { it.code.uppercase(java.util.Locale.ROOT) }.toSet()
                val missing = courses.filter { it.code.uppercase(java.util.Locale.ROOT) !in knownCodes }
                if (missing.isNotEmpty()) study.upsertImportedCourses(missing)
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
            val semester = if (nextStudyStart != null && previousExamEnd != null &&
                nextStudyStart > previousExamEnd && courses.any { it.semester == previousSemester + 1 }) {
                previousSemester + 1
            } else previousSemester
            val localPeriod = settingsRepo.settings.value.semesterPeriods[semester]
            val ids = courses.mapNotNull { course ->
                localBefore.firstOrNull { it.code == course.code }?.id?.let { course.code to it }
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
                    localBefore.firstOrNull { it.code == remote.code }?.let { local ->
                        val localResult = local.passFailResult?.name ?: "none"
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
                    localBefore.firstOrNull { it.code == remote.code }?.takeIf { it.status == remote.status }
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
            val changed = review.hasRelevantDifferences()
            mutablePreview.value = review.takeIf { changed }
            (if (changed) UisAutoSyncOutcome.REVIEW_READY else UisAutoSyncOutcome.APPLIED)
                .also { mutableLastCheckOutcome.value = it }
        } catch (cancelled: CancellationException) {
            mutableImportProgress.value = previous
            throw cancelled
        } catch (error: Exception) {
            mutableImportProgress.value = previous.copy(running = false,
                error = "Не вдалося автоматично перевірити UIS. Спробуй вручну.")
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
            val courses = plan.toCourses()
            val (period, timetable) = session.readStudyContext(plan.studyId, plan.periodId)
            val localCourses = studyPlanRepository.observeCourses().first()
            val localRules = scheduleRepository?.observeRules()?.first().orEmpty()
            val localOneOffs = scheduleRepository?.observeOneOffEvents()?.first().orEmpty()
            val semester = settingsRepository?.settings?.value?.effectiveCurrentSemester(courses)
                ?: courses.firstOrNull { it.status == CourseStatus.ENROLLED }?.semester
                ?: courses.maxOfOrNull { it.semester } ?: 1
            val localPeriod = settingsRepository?.settings?.value?.semesterPeriods?.get(semester)
            val first = !preferences.getBoolean(INITIAL_IMPORT_DONE, false)
            val importCourses = first && localCourses.isEmpty()
            val ids = courses.mapNotNull { course ->
                (localCourses.firstOrNull { it.code == course.code }?.id
                    ?: course.id.takeIf { importCourses })?.let { course.code to it }
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
            val count = if (importCourses) studyPlanRepository.upsertImportedCourses(courses) else 0
            if (first && localRules.isEmpty() && localOneOffs.isEmpty()) {
                scheduleRepository?.upsertRules(rules)
                scheduleRepository?.upsertOneOffEvents(oneOffs)
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
                val local = if (count > 0) remote else localCourses.firstOrNull { it.code == remote.code }
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
                is IOException -> "Не вдалося завантажити дані UIS. Перевір з’єднання."
                is IllegalStateException, is IllegalArgumentException -> "Не вдалося розпізнати навчальний план, календар або розклад UIS."
                else -> "Не вдалося імпортувати дані UIS."
            })
        }
    }

    suspend fun applyCourse(courseId: String) {
        val preview = mutablePreview.value ?: return
        if (UisSyncSection.SUBJECTS !in preview.availableSections) return
        val remote = preview.courses.firstOrNull { it.id == courseId } ?: return
        val local = preview.localCourses.firstOrNull { it.code == remote.code }
        val selected = if (local == null) remote else remote.copy(
            status = local.status, passFailResult = local.passFailResult)
        studyPlanRepository?.upsertImportedCourses(listOf(selected))
        if (local == null) updateBaselineProgress(remote.code, remote.status, remote.passFailResult)
        val subjectId = courseId.substringAfterLast(':')
        mutablePreview.value = preview.copy(
            rules = preview.rules.map { rule ->
                if (pendingSubjectByEventId[rule.id] == subjectId) rule.copy(courseId = courseId) else rule
            },
            oneOffEvents = preview.oneOffEvents.map { event ->
                if (pendingSubjectByEventId[event.id] == subjectId) event.copy(courseId = courseId) else event
            },
        )
        refreshLocalPreview()
    }

    suspend fun applyProgress(courseId: String) {
        val preview = mutablePreview.value ?: return
        if (UisSyncSection.PROGRESS !in preview.availableSections) return
        val remote = preview.courses.firstOrNull { it.id == courseId } ?: return
        val local = preview.localCourses.firstOrNull { it.code == remote.code }
        if (local == null) return
        studyPlanRepository?.updateStatus(local.id, remote.status)
        if (remote.gradingType == com.kpyruy.takt.core.model.CourseGradingType.PASS_FAIL) {
            studyPlanRepository?.setPassFailResult(local.id, remote.passFailResult)
        }
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

    suspend fun addRule(id: String) {
        if (UisSyncSection.TIMETABLE !in mutablePreview.value?.availableSections.orEmpty()) return
        mutablePreview.value?.rules?.firstOrNull { it.id == id }?.let { scheduleRepository?.upsertRule(it) }
        refreshLocalPreview()
    }

    suspend fun addOneOff(id: String) {
        if (UisSyncSection.TIMETABLE !in mutablePreview.value?.availableSections.orEmpty()) return
        mutablePreview.value?.oneOffEvents?.firstOrNull { it.id == id }?.let { scheduleRepository?.upsertOneOffEvent(it) }
        refreshLocalPreview()
    }

    suspend fun replaceSchedule() {
        val preview = mutablePreview.value ?: return
        if (UisSyncSection.TIMETABLE !in preview.availableSections) return
        val schedule = scheduleRepository ?: return
        schedule.observeExceptions().first().forEach { schedule.deleteException(it.id) }
        schedule.observeRules().first().forEach { schedule.deleteRule(it.id) }
        schedule.observeOneOffEvents().first().forEach { schedule.deleteOneOffEvent(it.id) }
        schedule.upsertRules(preview.rules)
        schedule.upsertOneOffEvents(preview.oneOffEvents)
        refreshLocalPreview()
    }

    fun dismissPreview() {
        mutablePreview.value = null
        pendingSubjectByEventId = emptyMap()
        mutableLastCheckOutcome.value = null
    }

    private suspend fun refreshLocalPreview() {
        val current = mutablePreview.value ?: return
        mutablePreview.value = current.copy(
            localCourses = studyPlanRepository?.observeCourses()?.first().orEmpty(),
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
                edit.remove(EARNED_CREDITS).remove(REQUIRED_CREDITS).remove(INITIAL_IMPORT_DONE)
                    .remove(PROGRESS_BASELINE).remove(AUTO_INVALID_DAY)
                UisSyncSection.entries.forEach { edit.remove("uis_auto_last_${it.name}") }
            }
            check(edit.commit()) { "Could not save UIS credentials" }
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
        session.disconnect()
        pendingAutoSections = null
        dismissPreview()
        check(preferences.edit().remove(CIPHERTEXT).remove(IV)
            .remove(EARNED_CREDITS).remove(REQUIRED_CREDITS).remove(INITIAL_IMPORT_DONE)
            .remove(PROGRESS_BASELINE).remove(AUTO_INVALID_DAY).apply {
                UisSyncSection.entries.forEach { remove("uis_auto_last_${it.name}") }
            }.commit()) { "Could not remove UIS credentials" }
        keyStore.deleteEntry(KEY_ALIAS)
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

private fun UisStudyPlan.toCourses(): List<Course> = courses.map { source ->
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
    )
}

private fun UisSyncPreview.hasRelevantDifferences(): Boolean {
    if (UisSyncSection.PROGRESS in availableSections &&
        (earnedCredits != localEarnedCredits || requiredCredits != localRequiredCredits ||
            courses.any { remote -> localCourses.firstOrNull { it.code == remote.code }?.let { local ->
                local.status != remote.status || local.passFailResult != remote.passFailResult
            } == true })) return true
    if (UisSyncSection.SUBJECTS in availableSections && courses.any { remote ->
            localCourses.firstOrNull { it.code == remote.code }?.let { local ->
                local.titleEn != remote.titleEn || local.titleSk != remote.titleSk ||
                    local.credits != remote.credits || local.semester != remote.semester ||
                    local.gradingType != remote.gradingType || local.requirementType != remote.requirementType
            } ?: true
        }) return true
    if (UisSyncSection.PERIODS in availableSections && period != localPeriod) return true
    if (UisSyncSection.TIMETABLE in availableSections &&
        (rules.toSet() != localRules.toSet() || oneOffEvents.toSet() != localOneOffEvents.toSet())) return true
    return false
}
