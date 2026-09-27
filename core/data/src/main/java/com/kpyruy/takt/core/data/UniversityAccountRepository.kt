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
import com.kpyruy.takt.core.model.PassFailResult
import java.io.IOException
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

class UniversityAccountRepository(
    context: Context,
    private val studyPlanRepository: StudyPlanRepository? = null,
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

    suspend fun signIn(credentials: UniversityCredentials) {
        session.login(credentials)
        importAfterLogin()
    }

    suspend fun submitCode(code: String) {
        session.submitCode(code)
        importAfterLogin()
    }

    suspend fun retryImport() = importAfterLogin()

    suspend fun checkAndImport() {
        session.check()
        importAfterLogin()
    }

    private suspend fun importAfterLogin() {
        if (session.state.value != UisResult.CONNECTED || studyPlanRepository == null) return
        val previous = mutableImportProgress.value
        mutableImportProgress.value = UisImportProgress(running = true)
        try {
            val plan = session.readStudyPlan()
            val courses = plan.courses.map { source ->
                Course(
                    id = "uis:${plan.studyId}:${source.subjectId}",
                    code = source.code,
                    title = source.title,
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
            val count = studyPlanRepository.upsertImportedCourses(courses)
            preferences.edit()
                .putInt(EARNED_CREDITS, plan.earnedCredits ?: -1)
                .putInt(REQUIRED_CREDITS, plan.requiredCredits ?: -1)
                .apply()
            mutableImportProgress.value = UisImportProgress(
                importedCourses = count,
                earnedCredits = plan.earnedCredits,
                requiredCredits = plan.requiredCredits,
            )
        } catch (cancelled: CancellationException) {
            mutableImportProgress.value = previous
            throw cancelled
        } catch (error: Exception) {
            mutableImportProgress.value = previous.copy(running = false, error = when (error) {
                is IOException -> "Не вдалося завантажити навчальний план UIS. Перевір з’єднання."
                is IllegalStateException -> "Не вдалося розпізнати навчальний план UIS або сесія завершилась."
                else -> "Не вдалося імпортувати навчальний план UIS."
            })
        }
    }

    /** Call only after a successful device authentication. */
    fun save(login: String, password: String) {
        session.disconnect()
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
            if (!sameAccount) edit.remove(EARNED_CREDITS).remove(REQUIRED_CREDITS)
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
        check(preferences.edit().remove(CIPHERTEXT).remove(IV)
            .remove(EARNED_CREDITS).remove(REQUIRED_CREDITS).commit()) { "Could not remove UIS credentials" }
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
        const val KEY_ALIAS = "takt_uis_credentials_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val AUTH_SECONDS = 30
    }
}
