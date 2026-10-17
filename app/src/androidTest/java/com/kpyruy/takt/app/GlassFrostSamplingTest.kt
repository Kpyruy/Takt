package com.kpyruy.takt.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.test.platform.app.InstrumentationRegistry
import com.kpyruy.takt.core.ui.R
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class GlassFrostSamplingTest {
    @Test fun onboardingMaterialContinuesBelowTheSampledViewportWithoutATransparentStrip() {
        val type = Class.forName("com.kpyruy.takt.core.ui.components.GlassFrostPainter")
        val frost = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).apply { eraseColor(0xFF345678.toInt()) }
        val painter = type.getDeclaredConstructor(Bitmap::class.java).newInstance(frost)
        val parameters = arrayOf(Canvas::class.java, Float::class.javaPrimitiveType!!,
            Float::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!,
            Float::class.javaPrimitiveType!!, Float::class.javaPrimitiveType!!, Float::class.javaPrimitiveType!!)
        val extendedDraw = runCatching { type.getDeclaredMethod("draw", *parameters, Boolean::class.javaPrimitiveType!!) }.getOrNull()
        val originalDraw = type.getDeclaredMethod("draw", *parameters)
        // Reproduce a footer reaching 80 pixels past the previous viewport after IME/inset changes.
        for (extended in listOf(false, true)) {
            val pane = Bitmap.createBitmap(300, 200, Bitmap.Config.ARGB_8888)
            if (extendedDraw != null) extendedDraw.invoke(painter, Canvas(pane), 0f, 880f, 300, 1000, 300f, 200f, 1.025f, extended)
            else originalDraw.invoke(painter, Canvas(pane), 0f, 880f, 300, 1000, 300f, 200f, 1.025f)
            val alpha = pane.getPixel(150, 199) ushr 24
            org.junit.Assert.assertEquals(if (extended) 255 else 0, alpha)
            pane.recycle()
        }
        frost.recycle()
    }

    @Test fun paneQuadPreservesViewportLensPixels() {
        val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
        val type = Class.forName("com.kpyruy.takt.core.ui.components.GlassFrostPainter")
        val constructor = type.getDeclaredConstructor(Bitmap::class.java)
        val draw = type.getDeclaredMethod("draw", Canvas::class.java, Float::class.javaPrimitiveType,
            Float::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType,
            Float::class.javaPrimitiveType, Float::class.javaPrimitiveType, Float::class.javaPrimitiveType)
        for (id in listOf(R.drawable.takt_glass_blue_light_frost, R.drawable.takt_glass_purple_dark_frost)) {
            val frost = BitmapFactory.decodeResource(resources, id)
            for ((x, y) in listOf(0f to 0f, 74f to 500f, 150f to 1500f, -100f to -150f, 1000f to 2350f)) {
                val expected = Bitmap.createBitmap(300, 200, Bitmap.Config.ARGB_8888)
                val actual = Bitmap.createBitmap(300, 200, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(expected)
                val mag = 1.025f
                canvas.translate(-x, -y)
                canvas.scale(mag, mag, x + 150f, y + 100f)
                canvas.drawBitmap(frost, null, RectF(0f, 0f, 1080f, 2400f), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
                draw.invoke(constructor.newInstance(frost), Canvas(actual), x, y, 1080, 2400, 300f, 200f, mag)
                val a = IntArray(300 * 200); val b = IntArray(a.size)
                expected.getPixels(a, 0, 300, 0, 0, 300, 200)
                actual.getPixels(b, 0, 300, 0, 0, 300, 200)
                var maxError = 0
                for (index in a.indices) for (shift in listOf(0, 8, 16, 24)) {
                    maxError = maxOf(maxError, abs(((a[index] ushr shift) and 255) - ((b[index] ushr shift) and 255)))
                }
                assertTrue("Lens differs at ($x,$y): $maxError levels", maxError <= 2)
                expected.recycle(); actual.recycle()
            }
            frost.recycle()
        }
    }
}
