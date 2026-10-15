package com.kpyruy.takt.core.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader

/** Sample the same world-space lens using a pane-sized quad, not a viewport-sized bitmap. */
internal class GlassFrostPainter(bitmap: Bitmap) {
    private val bitmapWidth = bitmap.width
    private val bitmapHeight = bitmap.height
    private val shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    private val matrix = Matrix()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { shader = this@GlassFrostPainter.shader }

    /** GPU tint only; the one-Bitmap constructor remains available to reflected pixel tests. */
    fun setColorFilter(filter: android.graphics.ColorFilter?) { paint.colorFilter = filter }

    fun draw(canvas: Canvas, localX: Float, localY: Float, worldWidth: Int, worldHeight: Int,
        width: Float, height: Float, magnification: Float) {
        matrix.setScale(worldWidth.toFloat() / bitmapWidth * magnification,
            worldHeight.toFloat() / bitmapHeight * magnification)
        val tx = -magnification * localX + (1f - magnification) * width / 2f
        val ty = -magnification * localY + (1f - magnification) * height / 2f
        matrix.postTranslate(tx, ty)
        shader.setLocalMatrix(matrix)
        val left = maxOf(0f, tx)
        val top = maxOf(0f, ty)
        val right = minOf(width, magnification * worldWidth + tx)
        val bottom = minOf(height, magnification * worldHeight + ty)
        if (right > left && bottom > top) canvas.drawRect(left, top, right, bottom, paint)
    }
}
