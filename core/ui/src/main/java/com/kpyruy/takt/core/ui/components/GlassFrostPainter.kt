package com.kpyruy.takt.core.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader

/** Sample the same world-space lens using a pane-sized quad, not a viewport-sized bitmap. */
internal class GlassFrostPainter(bitmap: Bitmap) {
    private var cropFill = false
    fun setCropFill(enabled: Boolean) { cropFill = enabled }
    private val bitmapWidth = bitmap.width
    private val bitmapHeight = bitmap.height
    private val shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    private val matrix = Matrix()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { shader = this@GlassFrostPainter.shader }

    /** GPU tint only; the one-Bitmap constructor remains available to reflected pixel tests. */
    fun setColorFilter(filter: android.graphics.ColorFilter?) { paint.colorFilter = filter }

    fun draw(canvas: Canvas, localX: Float, localY: Float, worldWidth: Int, worldHeight: Int,
        width: Float, height: Float, magnification: Float) {
        draw(canvas, localX, localY, worldWidth, worldHeight, width, height, magnification, false)
    }

    /** A window-edge pane can continue the clamped material beyond the sampled viewport. */
    fun draw(canvas: Canvas, localX: Float, localY: Float, worldWidth: Int, worldHeight: Int,
        width: Float, height: Float, magnification: Float, extendBackdrop: Boolean) {
        val scaleX = worldWidth.toFloat() / bitmapWidth
        val scaleY = worldHeight.toFloat() / bitmapHeight
        val sx = if (cropFill) maxOf(scaleX, scaleY) else scaleX
        val sy = if (cropFill) sx else scaleY
        matrix.setScale(sx * magnification, sy * magnification)
        val tx = -magnification * localX + (1f - magnification) * width / 2f
        val ty = -magnification * localY + (1f - magnification) * height / 2f
        matrix.postTranslate(tx + if (cropFill) (worldWidth - bitmapWidth * sx) / 2f * magnification else 0f,
            ty + if (cropFill) (worldHeight - bitmapHeight * sy) / 2f * magnification else 0f)
        shader.setLocalMatrix(matrix)
        val left = if (extendBackdrop) 0f else maxOf(0f, tx)
        val top = if (extendBackdrop) 0f else maxOf(0f, ty)
        val right = if (extendBackdrop) width else minOf(width, magnification * worldWidth + tx)
        val bottom = if (extendBackdrop) height else minOf(height, magnification * worldHeight + ty)
        if (right > left && bottom > top) canvas.drawRect(left, top, right, bottom, paint)
    }
}
