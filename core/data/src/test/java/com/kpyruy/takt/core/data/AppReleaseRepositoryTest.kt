package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.AppRelease
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class AppReleaseRepositoryTest {
    private val release = AppRelease("v0.3.0", "https://github.com/Kpyruy/Takt/releases/tag/v0.3.0", null)
    private class Store(var snapshot: AppReleasePreferences = AppReleasePreferences()) : AppReleasePreferenceStore {
        override fun read() = snapshot
        override fun write(value: AppReleasePreferences) { snapshot = value }
    }
    @Test fun successfulCheckIsCachedForOneDayAndManualCheckForcesRefresh() = runBlocking {
        var calls = 0
        var now = 1000L
        val store = Store()
        val repository = AppReleaseRepository("0.2.0", store, { calls++; release }, { now })
        repository.check()
        assertEquals(1000L, store.snapshot.lastSuccessfulCheck)
        now += 86_399_999
        repository.check()
        assertEquals(1, calls)
        repository.check(force = true)
        assertEquals(2, calls)
        now += 86_400_000
        repository.check()
        assertEquals(3, calls)
    }
    @Test fun automaticRateLimitCooldownSurvivesProcessRepositoryRecreation() = runBlocking {
        val store = Store()
        var calls = 0
        val fetch: suspend () -> AppRelease = { calls++; throw AppReleaseException(AppReleaseError.RATE_LIMITED, 10_000L) }
        AppReleaseRepository("0.2.0", store, fetch, { 1000L }).check()
        AppReleaseRepository("0.2.0", store, fetch, { 2000L }).check()
        assertEquals(1, calls)
        AppReleaseRepository("0.2.0", store, fetch, { 10_001L }).check()
        assertEquals(2, calls)
    }
    @Test fun failureKeepsCachedReleaseAndSuccessfulTimestamp() = runBlocking {
        val store = Store(AppReleasePreferences(latest = release, lastSuccessfulCheck = 10L))
        val repository = AppReleaseRepository("0.2.0", store, { throw AppReleaseException(AppReleaseError.NETWORK) }, { 100L })
        repository.check(force = true)
        assertEquals(release, repository.state.value.availableUpdate)
        assertEquals(10L, store.snapshot.lastSuccessfulCheck)
        assertEquals(AppReleaseError.NETWORK, repository.state.value.error)
    }
    @Test fun disabledAutomaticChecksStillAllowManualAndDismissalSurvivesRecreation() = runBlocking {
        val store = Store(AppReleasePreferences(automaticChecks = false))
        var calls = 0
        var latest = release
        val repository = AppReleaseRepository("0.2.0", store, { calls++; latest }, { 100L })
        repository.check()
        assertEquals(0, calls)
        repository.check(force = true)
        assertEquals(1, calls)
        repository.dismissUpdate()
        assertNull(repository.state.value.promptUpdate)
        val recreated = AppReleaseRepository("0.2.0", store, { release }, { 100L })
        assertNull(recreated.state.value.promptUpdate)
        assertEquals(release, recreated.state.value.availableUpdate)
        latest = release.copy(tag = "v0.4.0")
        repository.setAutomaticChecks(true)
        repository.check(force = true)
        assertEquals("v0.4.0", repository.state.value.promptUpdate?.tag)
    }
    @Test fun overlappingChecksAreDeduplicatedAndCancellationDoesNotMarkSuccess() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val finish = CompletableDeferred<Unit>()
        var calls = 0
        val store = Store(AppReleasePreferences(latest = release, lastSuccessfulCheck = 10L))
        val repository = AppReleaseRepository("0.2.0", store, { calls++; started.complete(Unit); finish.await(); release }, { 100L })
        val first = launch { repository.check(force = true) }
        withTimeout(1000) { started.await() }
        repository.check(force = true)
        assertEquals(1, calls)
        first.cancelAndJoin()
        assertFalse(repository.state.value.checking)
        assertEquals(10L, store.snapshot.lastSuccessfulCheck)
        assertEquals(release, repository.state.value.availableUpdate)
    }
    @Test fun rateLimitPreventsImmediateAutomaticRetriesButManualCanRetry() = runBlocking {
        var calls = 0
        var now = 100L
        val repository = AppReleaseRepository("0.2.0", Store(), {
            calls++
            throw AppReleaseException(AppReleaseError.RATE_LIMITED, retryAt = 1000L)
        }, { now })
        repository.check()
        repository.check()
        assertEquals(1, calls)
        now = 1000L
        repository.check()
        assertEquals(2, calls)
        repository.check(force = true)
        assertEquals(3, calls)
    }
}
