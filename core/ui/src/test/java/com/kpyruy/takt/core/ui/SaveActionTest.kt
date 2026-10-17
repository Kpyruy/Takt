package com.kpyruy.takt.core.ui

import com.kpyruy.takt.core.ui.components.SaveAction
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SaveActionTest {
    @Test fun failedWriteCanBeRetriedAndRapidDuplicateSaveDoesNotRunTwice() = runTest {
        val action = SaveAction(this)
        action.launch { throw java.io.IOException("full") }
        runCurrent()
        assertTrue(action.failed)
        assertFalse(action.saving)
        action.dismissError()
        val release = CompletableDeferred<Unit>()
        var writes = 0
        action.launch { release.await(); writes++ }
        action.launch { writes++ }
        runCurrent()
        assertTrue(action.saving)
        release.complete(Unit); runCurrent()
        assertEquals(1, writes)
        assertFalse(action.failed)
        assertFalse(action.saving)
    }
    @Test fun cancellationNeverBecomesAStorageError() = runTest {
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val action = SaveAction(scope)
        action.launch { awaitCancellation() }
        runCurrent(); scope.cancel(); runCurrent()
        assertFalse(action.failed)
        assertFalse(action.saving)
    }
    @Test fun rapidDifferentSettingChangesAreQueuedInOrder() = runTest {
        val action = SaveAction(this)
        val release = CompletableDeferred<Unit>()
        val writes = mutableListOf<String>()
        action.queue { release.await(); writes += "language" }
        action.queue { writes += "week" }
        runCurrent(); assertTrue(writes.isEmpty())
        release.complete(Unit); runCurrent()
        assertEquals(listOf("language", "week"), writes)
        assertFalse(action.saving)
    }

}
