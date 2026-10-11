package com.kpyruy.takt.core.ui.components

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlassBackdropBlurTest {
    @Test fun frostPreservesUniformColorIncludingClampedEdges() {
        val source = IntArray(15) { 0xFF529BCC.toInt() }
        assertArrayEquals(source, blurGlassBackdrop(source, 3, 5, 5))
    }

    @Test fun frostSpreadsAnIsolatedHighlightWithoutChangingAlpha() {
        val source = IntArray(25) { 0xFF000000.toInt() }
        source[12] = 0xFFFFFFFF.toInt()
        val blurred = blurGlassBackdrop(source, 5, 5, 1)
        assertEquals(0xFF1C1C1C.toInt(), blurred[12])
        assertEquals(blurred[12], blurred[6])
        assertEquals(0xFF000000.toInt(), blurred[0])
        assertTrue(blurred.all { it ushr 24 == 255 })
    }
}
