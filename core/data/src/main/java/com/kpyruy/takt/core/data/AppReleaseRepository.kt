package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.AppRelease
import com.kpyruy.takt.core.model.AppVersion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex

/** Device-local update preferences; deliberately absent from study-data backup. */
data class AppReleasePreferences(val automaticChecks: Boolean = true, val latest: AppRelease? = null,
    val lastSuccessfulCheck: Long? = null, val dismissedVersion: String? = null, val retryAfter: Long? = null)
interface AppReleasePreferenceStore { fun read(): AppReleasePreferences; fun write(value: AppReleasePreferences) }
enum class AppReleaseError { NETWORK, RATE_LIMITED, INVALID_RESPONSE, UNAVAILABLE }
class AppReleaseException(val reason: AppReleaseError, val retryAt: Long? = null) : Exception(reason.name)
data class AppReleaseState(val installedVersion: String, val automaticChecks: Boolean = true,
    val latest: AppRelease? = null, val lastSuccessfulCheck: Long? = null, val dismissedVersion: String? = null,
    val checking: Boolean = false, val error: AppReleaseError? = null) {
    val availableUpdate: AppRelease? get() = latest?.takeIf { it.isNewerThan(installedVersion) }
    val promptUpdate: AppRelease? get() = availableUpdate?.takeIf {
        automaticChecks && AppVersion.parse(it.tag) != dismissedVersion?.let(AppVersion::parse)
    }
}

class AppReleaseRepository(private val installedVersion: String, private val store: AppReleasePreferenceStore,
    private val fetchLatest: suspend () -> AppRelease, private val clock: () -> Long = System::currentTimeMillis) {
    private var preferences = store.read()
    private val mutableState = MutableStateFlow(snapshot())
    val state = mutableState.asStateFlow()
    private val checkMutex = Mutex()
    private var nextAutomaticAttempt = preferences.retryAfter ?: 0L

    private fun snapshot() = AppReleaseState(installedVersion, preferences.automaticChecks,
        preferences.latest, preferences.lastSuccessfulCheck, preferences.dismissedVersion)

    suspend fun check(force: Boolean = false) {
        // Foreground and Settings may request together. The active check supplies both consumers.
        if (!checkMutex.tryLock()) return
        try {
            val now = clock()
            synchronized(this) {
                val last = preferences.lastSuccessfulCheck
                if (!force && (!preferences.automaticChecks || now < nextAutomaticAttempt ||
                    (last != null && now >= last && now - last < CHECK_INTERVAL))) return
                mutableState.value = mutableState.value.copy(checking = true, error = null)
            }
            try {
                val release = fetchLatest()
                synchronized(this) {
                    preferences = preferences.copy(latest = release, lastSuccessfulCheck = clock(), retryAfter = null)
                    store.write(preferences)
                    nextAutomaticAttempt = 0L
                    mutableState.value = snapshot().copy(checking = true)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                synchronized(this) {
                    val failure = error as? AppReleaseException
                    nextAutomaticAttempt = failure?.retryAt ?: (clock() + FAILURE_RETRY_INTERVAL)
                    preferences = preferences.copy(retryAfter = nextAutomaticAttempt)
                    store.write(preferences)
                    mutableState.value = mutableState.value.copy(error = failure?.reason ?: AppReleaseError.NETWORK)
                }
            }
        } finally {
            synchronized(this) { mutableState.value = mutableState.value.copy(checking = false) }
            checkMutex.unlock()
        }
    }

    @Synchronized fun dismissUpdate() {
        val version = state.value.availableUpdate?.tag ?: return
        preferences = preferences.copy(dismissedVersion = version)
        store.write(preferences)
        mutableState.value = mutableState.value.copy(dismissedVersion = version)
    }

    @Synchronized fun setAutomaticChecks(enabled: Boolean) {
        preferences = preferences.copy(automaticChecks = enabled)
        store.write(preferences)
        mutableState.value = mutableState.value.copy(automaticChecks = enabled)
    }

    companion object {
        private const val CHECK_INTERVAL = 86_400_000L
        private const val FAILURE_RETRY_INTERVAL = 300_000L
    }
}
