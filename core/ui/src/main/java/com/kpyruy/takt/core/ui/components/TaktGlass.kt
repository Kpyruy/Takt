package com.kpyruy.takt.core.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalContext
import com.kpyruy.takt.core.ui.R
import com.kpyruy.takt.core.model.ThemeFamily
import com.kpyruy.takt.core.ui.theme.LocalTaktThemeFamily
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.CardAppearance
import com.kpyruy.takt.core.ui.theme.LocalTaktCardAppearance

/** A single shared backdrop. Surfaces sample this image, never their own foreground. */
private class GlassBackdrop(val image: ImageBitmap, val frost: ImageBitmap) {
    var origin by mutableStateOf(Offset.Zero)
    var size by mutableStateOf(IntSize.Zero)
}

private val LocalGlassBackdrop = staticCompositionLocalOf<GlassBackdrop?> { null }

/** Continuous icy silk in screen coordinates, shared by cards, sheets and navigation. */
@Composable
fun TaktGlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < .3f
    val resources = LocalContext.current.resources
    val family = LocalTaktThemeFamily.current
    val backdrop = remember(resources, family, colors.primary, dark) {
        val resource = if (dark) R.drawable.takt_glass_dark else R.drawable.takt_glass_light
        val source = BitmapFactory.decodeResource(resources, resource)
        val image = if (family == ThemeFamily.BLUE) source else recolorSilk(source, colors.primary, dark)
        val reduced = Bitmap.createScaledBitmap(image, 360, 720, true)
        val pixels = IntArray(360 * 720)
        reduced.getPixels(pixels, 0, 360, 0, 0, 360, 720)
        val frost = Bitmap.createBitmap(blurGlassBackdrop(pixels, 360, 720, 5), 360, 720, Bitmap.Config.ARGB_8888)
        // Compose retains the image and frost; the intermediate scaled bitmap has no readers.
        if (reduced !== image) reduced.recycle()
        if (image !== source) source.recycle()
        GlassBackdrop(image.asImageBitmap(), frost.asImageBitmap())
    }
    CompositionLocalProvider(LocalGlassBackdrop provides backdrop) {
        Box(modifier.fillMaxSize().onGloballyPositioned {
            backdrop.origin = it.positionOnScreen()
            backdrop.size = it.size
        }) {
            Canvas(Modifier.matchParentSize()) {
                drawImage(backdrop.image, dstSize = IntSize(size.width.toInt(), size.height.toInt()))
            }
            content()
        }
    }
}

/** Frosted body, refracted backdrop and illuminated glass rim; content is drawn separately. */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    tint: Color = Color.Unspecified,
    content: @Composable () -> Unit,
) {
    Box(modifier.glassSurface(shape, tint)) { content() }
}

/**
 * The backdrop is low-pass filtered once, then sampled at the surface's screen position.
 * A slight lens magnification shifts the silk beneath each pane. No RenderEffect is applied
 * to content, and the same implementation works on API 26 without platform blur support.
 */
@Composable
fun Modifier.glassSurface(
    shape: Shape = RoundedCornerShape(20.dp),
    tint: Color = Color.Unspecified,
): Modifier {
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < .3f
    val appearance = LocalTaktCardAppearance.current
    val backdrop = LocalGlassBackdrop.current
    var position by remember { mutableStateOf(Offset.Zero) }
    val tonal = appearance == CardAppearance.TONAL_FILLED
    val body = if (tint != Color.Unspecified) tint else if (dark) Color(0xFF87ADD3) else Color.White
    val edge = if (dark) Color(0xFFB7DFFF) else Color.White
    return this
        .onGloballyPositioned { position = it.positionOnScreen() }
        .shadow(if (tonal) 1.dp else 7.dp, shape, clip = false,
            ambientColor = Color(0xFF315677).copy(alpha = .12f),
            spotColor = Color(0xFF315677).copy(alpha = .17f))
        .clip(shape)
        .drawWithCache {
            val outline = shape.createOutline(size, layoutDirection, this)
            val rim = Brush.linearGradient(
                listOf(edge.copy(alpha = if (dark) .52f else .95f), edge.copy(alpha = .22f), edge.copy(alpha = .68f)),
                start = Offset.Zero, end = Offset(size.width * .75f, size.height),
            )
            val sheen = Brush.linearGradient(
                listOf(Color.White.copy(alpha = if (dark) .12f else .34f), Color.Transparent,
                    colors.primary.copy(alpha = if (dark) .07f else .035f)),
                start = Offset.Zero, end = Offset(size.width * .35f, size.height),
            )
            onDrawBehind {
                backdrop?.let { source ->
                    if (source.size.width > 0 && source.size.height > 0) {
                        val local = position - source.origin
                        // Lens distortion is anchored to this pane's center, not to its scrolling parent.
                        val magnification = 1.025f
                        withTransform({
                            translate(-local.x, -local.y)
                            scale(magnification, magnification, pivot = local + Offset(size.width / 2f, size.height / 2f))
                        }) {
                            drawImage(source.frost, dstOffset = IntOffset.Zero, dstSize = source.size)
                        }
                    }
                }
                drawOutline(outline, body.copy(alpha = when {
                    tonal -> if (dark) .24f else .63f
                    tint != Color.Unspecified -> if (dark) .20f else .28f
                    else -> if (dark) .13f else .42f
                }))
                drawOutline(outline, sheen)
                drawOutline(outline, rim, style = Stroke(1.dp.toPx()))
            }
        }
}

/** Keep luminance and the neutral highlights while adapting the silk to the selected palette. */
private fun recolorSilk(source: Bitmap, accent: Color, dark: Boolean): Bitmap {
    val pixels = IntArray(source.width * source.height)
    source.getPixels(pixels, 0, source.width, 0, 0, source.width, source.height)
    for (index in pixels.indices) {
        val color = Color(pixels[index])
        val neutral = Color(color.red * .25f + color.green * .5f + color.blue * .25f,
            color.red * .25f + color.green * .5f + color.blue * .25f,
            color.red * .25f + color.green * .5f + color.blue * .25f)
        val chroma = (color.blue - color.red).coerceIn(0f, 1f)
        val shade = if (dark) lerp(Color.Black, accent, neutral.red.coerceAtMost(.6f))
            else lerp(accent, Color.White, neutral.red)
        pixels[index] = lerp(neutral, shade, (chroma * 3.5f + .12f).coerceAtMost(.65f)).toArgb()
    }
    return Bitmap.createBitmap(pixels, source.width, source.height, Bitmap.Config.ARGB_8888)
}

/** RGB box blur using sliding windows. Works on every supported API and runs once per palette. */
internal fun blurGlassBackdrop(input: IntArray, width: Int, height: Int, radius: Int): IntArray {
    val horizontal = IntArray(input.size)
    val output = IntArray(input.size)
    fun pass(source: IntArray, target: IntArray, rows: Int, columns: Int, vertical: Boolean) {
        fun offset(row: Int, column: Int): Int = if (vertical) column * width + row else row * width + column
        val count = radius * 2 + 1
        for (row in 0 until rows) {
            var r = 0; var g = 0; var b = 0
            for (column in -radius..radius) {
                val color = source[offset(row, column.coerceIn(0, columns - 1))]
                r += color shr 16 and 255; g += color shr 8 and 255; b += color and 255
            }
            for (column in 0 until columns) {
                target[offset(row, column)] = (255 shl 24) or ((r / count) shl 16) or ((g / count) shl 8) or (b / count)
                val old = source[offset(row, (column - radius).coerceIn(0, columns - 1))]
                val next = source[offset(row, (column + radius + 1).coerceIn(0, columns - 1))]
                r += (next shr 16 and 255) - (old shr 16 and 255)
                g += (next shr 8 and 255) - (old shr 8 and 255)
                b += (next and 255) - (old and 255)
            }
        }
    }
    pass(input, horizontal, height, width, false)
    pass(horizontal, output, width, height, true)
    return output
}
