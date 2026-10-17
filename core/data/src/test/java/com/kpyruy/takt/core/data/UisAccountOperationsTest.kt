package com.kpyruy.takt.core.data

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class UisAccountOperationsTest {
    @Test fun accountSwitchWaitsForTheOldWorkersCleanupBeforeStartingNewWork() = runBlocking {
        val operations = UisAccountOperations()
        val started = CompletableDeferred<Unit>()
        val cleanup = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val events = mutableListOf<String>()
        val old = launch {
            operations.run {
                started.complete(Unit)
                try { awaitCancellation() }
                finally { withContext(NonCancellable) {
                    cleanup.complete(Unit)
                    release.await()
                    events += "old stopped"
                } }
            }
        }
        started.await()
        val change = async { operations.change { events += "account replaced" } }
        cleanup.await()
        val next = launch(start = CoroutineStart.UNDISPATCHED) { operations.run { events += "new operation" } }
        assertFalse(change.isCompleted)
        assertEquals(emptyList<String>(), events)
        release.complete(Unit)
        withTimeout(2_000) { old.join(); change.await(); next.join() }
        assertEquals(listOf("old stopped", "account replaced", "new operation"), events)
    }

    @Test fun workQueuedBeforeAnotherAccountReplacementCannotRunForTheNewAccount() = runBlocking {
        val operations = UisAccountOperations()
        val replacing = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val first = launch { operations.change { replacing.complete(Unit); release.await() } }
        replacing.await()
        // Queue the next replacement first, then a read stamped with the old generation.
        val second = launch(start = CoroutineStart.UNDISPATCHED) { operations.change { } }
        var wrote = false
        val stale = launch(start = CoroutineStart.UNDISPATCHED) { operations.run { wrote = true } }
        release.complete(Unit)
        withTimeout(2_000) { first.join(); second.join(); stale.join() }
        assertTrue(stale.isCancelled)
        assertFalse(wrote)
        var currentRan = false
        operations.run { currentRan = true }
        assertTrue(currentRan)
    }
}
