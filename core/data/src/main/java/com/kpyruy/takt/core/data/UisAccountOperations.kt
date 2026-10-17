package com.kpyruy.takt.core.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Account replacement waits until every old session/import/review worker has stopped. */
internal class UisAccountOperations {
    private val changes = Mutex()
    private val monitor = Any()
    private var generation = 0L
    private val active = mutableSetOf<Job>()

    suspend fun <T> run(action: suspend () -> T): T {
        val expected = synchronized(monitor) { generation }
        return coroutineScope {
            val operation = currentCoroutineContext()[Job]!!
            changes.withLock {
                synchronized(monitor) {
                    if (expected != generation) throw CancellationException("UIS account changed")
                    active.add(operation)
                }
                operation.invokeOnCompletion { synchronized(monitor) { active.remove(operation) } }
            }
            action()
        }
    }

    suspend fun <T> change(action: suspend () -> T): T = changes.withLock {
        val previous = synchronized(monitor) { generation++; active.toList() }
        previous.forEach { it.cancel(CancellationException("UIS account changed")) }
        // Even caller cancellation must not let an old writer outlive account cleanup.
        withContext(NonCancellable) { previous.joinAll() }
        action()
    }
}
