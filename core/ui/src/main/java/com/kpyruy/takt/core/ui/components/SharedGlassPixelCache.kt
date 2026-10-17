package com.kpyruy.takt.core.ui.components

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * Process-lifetime pixel work, independent of a composable's lifetime. The default scope runs
 * generation off main; an injected worker scope allows deterministic concurrency tests.
 * Only completed values count towards the LRU capacity. One generator at a time bounds transient
 * bitmap allocations too. Eviction drops references, never disposes values still held by readers.
 */
internal class SharedGlassPixelCache<K : Any, V : Any>(
    private val capacity: Int = 2,
    private val workerScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    init { require(capacity > 0) }

    private val lock = Any()
    private val completed = LinkedHashMap<K, V>(capacity, .75f, true)
    private val inFlight = mutableMapOf<K, Deferred<Result<V>>>()
    private val generationPermit = Semaphore(1)

    /** A cheap lookup only: no decoding or generation on the calling thread. */
    fun peek(key: K): V? = synchronized(lock) { completed[key] }

    suspend fun get(key: K, generate: suspend () -> V): V {
        currentCoroutineContext().ensureActive()
        val request = synchronized(lock) {
            completed[key]?.let { return it }
            inFlight[key] ?: workerScope.async(start = CoroutineStart.LAZY) {
                try {
                    generationPermit.withPermit {
                        val pixels = generate()
                        synchronized(lock) {
                            completed[key] = pixels
                            while (completed.size > capacity) {
                                val eldest = completed.entries.iterator()
                                eldest.next()
                                eldest.remove()
                            }
                        }
                        Result.success(pixels)
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    // Report failure to awaiters without cancelling other worker-scope jobs.
                    Result.failure(failure)
                } finally {
                    synchronized(lock) { inFlight.remove(key) }
                }
            }.also { inFlight[key] = it }
        }
        // Start after installing the shared request. UI cancellation only cancels await(),
        // so an existing or future background can still reuse this generation.
        request.start()
        return request.await().getOrThrow()
    }
}
