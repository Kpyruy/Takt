package com.kpyruy.takt.core.ui.components

import androidx.compose.ui.graphics.Color
import com.kpyruy.takt.core.model.ThemeFamily
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SharedGlassPixelCacheTest {
    @Test fun equivalentPaletteRequestsReusePixelsWhileEachPixelInputSeparatesEntries() = runTest {
        val cache = SharedGlassPixelCache<GlassPaletteKey, Any>(workerScope = this)
        val original = cache.get(GlassPaletteKey(false, ThemeFamily.BLUE, Color.Blue)) { Any() }
        // New instances/locales construct new keys, but never include a context/resource identity.
        assertSame(original, cache.get(GlassPaletteKey(false, ThemeFamily.BLUE, Color.Blue)) {
            error("equivalent palette must reuse pixels")
        })
        val dark = cache.get(GlassPaletteKey(true, ThemeFamily.BLUE, Color.Blue)) { Any() }
        val otherFamily = ThemeFamily.entries.first { it != ThemeFamily.BLUE }
        val family = cache.get(GlassPaletteKey(false, otherFamily, Color.Blue)) { Any() }
        val accent = cache.get(GlassPaletteKey(false, ThemeFamily.BLUE, Color.Red)) { Any() }
        listOf(dark, family, accent).forEach { assertNotSame(original, it) }
    }

    @Test fun simultaneousBackgroundsShareOneGenerationAndTheSamePixels() = runTest {
        val cache = SharedGlassPixelCache<String, Any>(workerScope = this)
        val release = CompletableDeferred<Unit>()
        var generations = 0
        val readers = List(12) {
            async {
                cache.get("light-blue") {
                    generations++
                    release.await()
                    Any()
                }
            }
        }
        runCurrent()
        assertEquals(1, generations)
        assertTrue(readers.none { it.isCompleted })
        release.complete(Unit)
        val pixels = readers.first().await()
        readers.forEach { assertSame(pixels, it.await()) }
        assertSame(pixels, cache.get("light-blue") { error("cached pixels must be reused") })
    }

    @Test fun fifthPaletteEvictsLeastRecentlyUsedPairWithoutInvalidatingHeldPixels() = runTest {
        val cache = SharedGlassPixelCache<String, MutableList<Int>>(capacity = 4, workerScope = this)
        val held = cache.get("a") { mutableListOf(1) }
        val b = cache.get("b") { mutableListOf(2) }
        cache.get("c") { mutableListOf(3) }
        cache.get("d") { mutableListOf(4) }
        assertSame(b, cache.peek("b")) // Access keeps this pair resident.
        cache.get("e") { mutableListOf(5) }

        assertNull(cache.peek("a"))
        assertSame(b, cache.peek("b"))
        assertEquals(listOf(1), held) // An active background may still own the evicted pair.
        val regenerated = cache.get("a") { mutableListOf(6) }
        assertNotSame(held, regenerated)
        assertEquals(listOf(1), held)
        assertNull(cache.peek("c"))
    }

    @Test fun cancellingASupersededReaderDoesNotCancelAnotherBackgroundsGeneration() = runTest {
        val cache = SharedGlassPixelCache<String, Any>(workerScope = this)
        val release = CompletableDeferred<Unit>()
        var generations = 0
        var publishedByCancelledReader = false
        val first = async {
            cache.get("dark-green") {
                generations++
                release.await()
                Any()
            }
            publishedByCancelledReader = true
        }
        runCurrent()
        val second = async { cache.get("dark-green") { error("duplicate generation") } }
        runCurrent()
        first.cancelAndJoin()
        release.complete(Unit)
        val pixels = second.await()

        assertEquals(1, generations)
        assertTrue(!publishedByCancelledReader)
        assertSame(pixels, cache.peek("dark-green"))
    }

    @Test fun abandonedRequestWarmsCacheForTheNextInstance() = runTest {
        val cache = SharedGlassPixelCache<String, Any>(workerScope = this)
        val release = CompletableDeferred<Unit>()
        val pixels = Any()
        val reader = async { cache.get("a") { release.await(); pixels } }
        runCurrent()
        reader.cancelAndJoin()
        release.complete(Unit)
        runCurrent()
        assertSame(pixels, cache.get("a") { error("unneeded regeneration") })
    }

    @Test fun distinctPaletteWorkIsSerializedToBoundTransientBitmapMemory() = runTest {
        val cache = SharedGlassPixelCache<String, String>(workerScope = this)
        val release = CompletableDeferred<Unit>()
        val order = mutableListOf<String>()
        val first = async {
            cache.get("a") { order += "a-start"; release.await(); order += "a-end"; "a" }
        }
        runCurrent()
        val second = async { cache.get("b") { order += "b"; "b" } }
        runCurrent()
        assertEquals(listOf("a-start"), order)
        release.complete(Unit)
        assertEquals("a", first.await())
        assertEquals("b", second.await())
        assertEquals(listOf("a-start", "a-end", "b"), order)
    }

    @Test fun failedGenerationCanBeRetriedAndIsNeverCached() = runTest {
        val cache = SharedGlassPixelCache<String, String>(workerScope = this)
        var attempts = 0
        try {
            cache.get("a") { attempts++; throw IllegalStateException("decode failed") }
            error("the reader must observe the failure")
        } catch (expected: IllegalStateException) {
            assertEquals("decode failed", expected.message)
        }
        assertNull(cache.peek("a"))
        assertEquals("ready", cache.get("a") { attempts++; "ready" })
        assertEquals(2, attempts)
    }

    @Test fun defaultGenerationRunsOffTheRequestingThread() = runBlocking {
        val cache = SharedGlassPixelCache<String, Thread>()
        val requestingThread = Thread.currentThread()
        val generationThread = cache.get("a") { Thread.currentThread() }
        assertNotSame(requestingThread, generationThread)
    }
}
