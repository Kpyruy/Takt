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
import kotlinx.coroutines.flow.first
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
    val period: SemesterPeriod,
    val localPeriod: SemesterPeriod?,
    val rules: List<ScheduleRule>,
    val oneOffEvents: List<OneOffScheduleEvent>,
    val localRules: List<ScheduleRule>,
    val localOneOffEvents: List<OneOffScheduleEvent>,
    val earnedCredits: Int?,
    val requiredCredits: Int?,
    val localEarnedCredits: Int?,
    val localRequiredCredits: Int?,
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
    private val mutablePreview = MutableStateFlow<UisSyncPreview?>(null)
    val syncPreview = mutablePreview.asStateFlow()
    private var pendingSubjectByEventId: Map<String, String> = emptyMap()

    suspend fun setFirstCourseNameChoice(language: CourseNameLanguage) {
        require(language != CourseNameLanguage.FOLLOW_APP)
        settingsRepository?.setUkrainianCourseNameFallback(language)
        settingsRepository?.setCourseNameLanguage(language)
    }

    suspend fun signIn(credentials: UniversityCredentials) {
        session.login(credentials)
        importAfterLogin()
    }

    suspend fun submitCode(code: String) {
        session.submitCode(code)
        importAfterLogin()
    }

    suspend fun retryImport() = importAfterLogin(manual = true)

    suspend fun checkAndImport() {
        session.check()
        importAfterLogin(manual = true)
    }

    private suspend fun importAfterLogin(manual: Boolean = false) {
        if (session.state.value != UisResult.CONNECTED || studyPlanRepository == null) return
        if (!manual && preferences.getBoolean(INITIAL_IMPORT_DONE, false)) return
        val previous = mutableImportProgress.value
        mutableImportProgress.value = UisImportProgress(running = true)
        try {
            val plan = session.readStudyPlan()
            val courses = plan.courses.map { source ->
                Course(
                    id = "uis:${plan.studyId}:${source.subjectId}",
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
                        source.status == com.kpyruy.takt.core.model.CourseStatus.FULFILLED) PassFailResult.PASSED else null,
                )
            }
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
            preferences.edit().putBoolean(INITIAL_IMPORT_DONE, true).apply()
            mutablePreview.value = UisSyncPreview(courses, localCourses, semester, period, localPeriod,
                rules, oneOffs, localRules, localOneOffs, plan.earnedCredits, plan.requiredCredits,
                preferences.getInt(EARNED_CREDITS, -1).takeIf { it >= 0 },
                preferences.getInt(REQUIRED_CREDITS, -1).takeIf { it >= 0 })
            refreshLocalPreview()
            mutableImportProgress.value = UisImportProgress(
                importedCourses = count.takeIf { it > 0 },
                earnedCredits = plan.earnedCredits,
                requiredCredits = plan.requiredCredits,
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
        val remote = preview.courses.firstOrNull { it.id == courseId } ?: return
        val local = preview.localCourses.firstOrNull { it.code == remote.code }
        val selected = if (local == null) remote else remote.copy(
            status = local.status, passFailResult = local.passFailResult)
        studyPlanRepository?.upsertImportedCourses(listOf(selected))
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
        val remote = preview.courses.firstOrNull { it.id == courseId } ?: return
        val local = preview.localCourses.firstOrNull { it.code == remote.code }
        if (local == null) return
        studyPlanRepository?.updateStatus(local.id, remote.status)
        refreshLocalPreview()
    }

    suspend fun applyAllProgress() {
        val preview = mutablePreview.value ?: return
        preview.courses.forEach { applyProgress(it.id) }
    }

    fun applyCredits() {
        val preview = mutablePreview.value ?: return
        saveCredits(preview.earnedCredits, preview.requiredCredits)
        mutableImportProgress.value = mutableImportProgress.value.copy(
            earnedCredits = preview.earnedCredits, requiredCredits = preview.requiredCredits)
        mutablePreview.value = preview.copy(localEarnedCredits = preview.earnedCredits,
            localRequiredCredits = preview.requiredCredits)
    }

    suspend fun applyPeriod() {
        val preview = mutablePreview.value ?: return
        val current = settingsRepository?.settings?.value?.semesterPeriods.orEmpty()
        settingsRepository?.setSemesterPeriods(current + (preview.semester to preview.period))
        refreshLocalPreview()
    }

    suspend fun addRule(id: String) {
        mutablePreview.value?.rules?.firstOrNull { it.id == id }?.let { scheduleRepository?.upsertRule(it) }
        refreshLocalPreview()
    }

    suspend fun addOneOff(id: String) {
        mutablePreview.value?.oneOffEvents?.firstOrNull { it.id == id }?.let { scheduleRepository?.upsertOneOffEvent(it) }
        refreshLocalPreview()
    }

    suspend fun replaceSchedule() {
        val preview = mutablePreview.value ?: return
        val schedule = scheduleRepository ?: return
        schedule.observeExceptions().first().forEach { schedule.deleteException(it.id) }
        schedule.observeRules().first().forEach { schedule.deleteRule(it.id) }
        schedule.observeOneOffEvents().first().forEach { schedule.deleteOneOffEvent(it.id) }
        schedule.upsertRules(preview.rules)
        schedule.upsertOneOffEvents(preview.oneOffEvents)
        refreshLocalPreview()
    }

    fun dismissPreview() { mutablePreview.value = null; pendingSubjectByEventId = emptyMap() }

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

    /** Call only after a successful device authentication. */
    fun save(login: String, password: String) {
        session.disconnect()
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
            if (!sameAccount) edit.remove(EARNED_CREDITS).remove(REQUIRED_CREDITS).remove(INITIAL_IMPORT_DONE)
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
        dismissPreview()
        check(preferences.edit().remove(CIPHERTEXT).remove(IV)
            .remove(EARNED_CREDITS).remove(REQUIRED_CREDITS).remove(INITIAL_IMPORT_DONE).commit()) { "Could not remove UIS credentials" }
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
        const val KEY_ALIAS = "takt_uis_credentials_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val AUTH_SECONDS = 30
    }
}
