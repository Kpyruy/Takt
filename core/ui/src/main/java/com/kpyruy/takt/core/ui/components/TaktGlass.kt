package com.kpyruy.takt.core.ui.components

import android.content.Context
import java.io.File
import com.kpyruy.takt.core.model.normalizeWallpaperReference
import com.kpyruy.takt.core.ui.theme.LocalTaktWallpaper
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Trace
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.drawscope.translate
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.CardAppearance
import com.kpyruy.takt.core.model.ThemeFamily
import com.kpyruy.takt.core.ui.R
import com.kpyruy.takt.core.ui.theme.LocalTaktCardAppearance
import com.kpyruy.takt.core.ui.theme.LocalTaktThemeFamily
import com.kpyruy.takt.core.ui.theme.LocalTaktCustomTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Locale/context identity and per-background geometry never participate in the pixel key. */
internal data class GlassPaletteKey(val dark: Boolean, val family: ThemeFamily, val accent: Color)

/** Read-only pixels shared across backgrounds; no screen coordinates or context retained here. */
private class GlassPixels(val image: ImageBitmap, val frost: ImageBitmap, val cropFill: Boolean = false)

private val glassPixelCache = SharedGlassPixelCache<GlassPaletteKey, GlassPixels>()
private data class PhotoKey(val reference: String, val dark: Boolean)
private val photoPixelCache = SharedGlassPixelCache<PhotoKey, GlassPixels>(capacity = 2)
private data class PreviewPhotoKey(val image: Bitmap, val dark: Boolean)
private val previewPhotoPixelCache = SharedGlassPixelCache<PreviewPhotoKey, GlassPixels>(capacity = 2)

/** Each background owns geometry even when it shares pixels with a nested background. */
private class GlassBackdrop(initialPixels: GlassPixels?, private val parentGeometry: GlassBackdrop? = null) {
    var pixels by mutableStateOf(initialPixels)
    var origin by mutableStateOf(Offset.Zero)
    var size by mutableStateOf(IntSize.Zero)
    val sampledOrigin: Offset get() = parentGeometry?.sampledOrigin ?: origin
    val sampledSize: IntSize get() = parentGeometry?.sampledSize ?: size
}

private val LocalGlassBackdrop = staticCompositionLocalOf<GlassBackdrop?> { null }
private data class GlassTint(val image: ColorFilter, val frost: android.graphics.ColorFilter)
private val LocalGlassTint = staticCompositionLocalOf<GlassTint?> { null }

/** Keys describe prepared assets, never custom hues. The existing reflected key signature stays. */
internal fun preparedGlassKey(dark: Boolean, family: ThemeFamily, custom: Boolean): GlassPaletteKey =
    GlassPaletteKey(dark, if (custom || family in setOf(ThemeFamily.ROSE, ThemeFamily.AMBER, ThemeFamily.CYAN))
        ThemeFamily.MONOCHROME else family, Color.Unspecified)

private fun glassTint(accent: Color, dark: Boolean): GlassTint {
    val channels = floatArrayOf(accent.red, accent.green, accent.blue)
    val values = FloatArray(20)
    channels.forEachIndexed { row, channel ->
        val scale = if (dark) .5f + .5f * channel else .6f + .4f * channel
        values[row * 5] = .2126f * scale
        values[row * 5 + 1] = .7152f * scale
        values[row * 5 + 2] = .0722f * scale
        values[row * 5 + 4] = if (dark) 0f else (1f - scale) * 255f
    }
    values[18] = 1f
    return GlassTint(ColorFilter.colorMatrix(ColorMatrix(values)),
        android.graphics.ColorMatrixColorFilter(values))
}

/**
 * Continuous icy silk in screen coordinates, shared by cards, sheets and navigation.
 * Cold loads show the theme background; palette changes keep the previous pixels until ready.
 * Disabled wrappers only lay out content, preserving any enclosing backdrop for their children.
 */
@Composable
fun TaktGlassBackground(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    drawWallpaper: Boolean = true,
    // Portable backup thumbnails can preview the real material without publishing a wallpaper file.
    photoPreview: Bitmap? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    if (!enabled) {
        Box(modifier.fillMaxSize(), content = content)
        return
    }
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < .3f
    // Only application resources reach the worker; localized/Activity contexts are not captured.
    val context = LocalContext.current.applicationContext
    val resources = context.resources
    val wallpaper = normalizeWallpaperReference(LocalTaktWallpaper.current)
    val previewKey = photoPreview?.let { PreviewPhotoKey(it, dark) }
    val photoKey = wallpaper?.takeIf { previewKey == null }?.let { PhotoKey(it, dark) }
    val family = LocalTaktThemeFamily.current
    val custom = LocalTaktCustomTheme.current
    val key = preparedGlassKey(dark, family, custom != null)
    val tinted = wallpaper == null && previewKey == null && (custom != null || family in setOf(ThemeFamily.ROSE, ThemeFamily.AMBER, ThemeFamily.CYAN))
    val wallpaperAccent = custom?.let { Color(it.primaryArgb) } ?: colors.primary
    val tint = remember(tinted, wallpaperAccent, dark) {
        if (tinted) glassTint(wallpaperAccent, dark) else null
    }
    val requestedKey by rememberUpdatedState(previewKey ?: photoKey ?: key)
    val parent = LocalGlassBackdrop.current
    val backdrop = remember(dark, wallpaper, previewKey, drawWallpaper, parent) {
        GlassBackdrop(when {
            previewKey != null -> previewPhotoPixelCache.peek(previewKey)
            photoKey != null -> photoPixelCache.peek(photoKey)
            else -> glassPixelCache.peek(key)
        },
            parentGeometry = parent.takeUnless { drawWallpaper })
    }
    LaunchedEffect(key, photoKey, previewKey) {
        try {
            val pixels = if (previewKey != null) {
                previewPhotoPixelCache.get(previewKey) {
                    preparePhotoPixels(requireNotNull(previewKey.image.copy(Bitmap.Config.ARGB_8888, true)), dark)
                }
            } else if (photoKey != null) {
                try { photoPixelCache.get(photoKey) { decodePhotoPixels(context, photoKey) } }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { glassPixelCache.get(key) { decodePreparedGlassPixels(resources, key) } }
            } else glassPixelCache.get(key) { decodePreparedGlassPixels(resources, key) }
            currentCoroutineContext().ensureActive()
            if (requestedKey == (previewKey ?: photoKey ?: key)) backdrop.pixels = pixels
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            // Preserve the old pair (or the first-frame theme fill) on decode/generation failure.
            Log.w("TaktGlass", "Unable to generate glass backdrop", failure)
        }
    }
    CompositionLocalProvider(LocalGlassBackdrop provides backdrop, LocalGlassTint provides tint) {
        Box(modifier.fillMaxSize().then(if (drawWallpaper) Modifier.background(colors.background) else Modifier).onGloballyPositioned {
            backdrop.origin = it.positionOnScreen()
            backdrop.size = it.size
        }) {
            if (drawWallpaper) Canvas(Modifier.matchParentSize().testTag("glass-wallpaper")) {
                backdrop.pixels?.let {
                    val destination = IntSize(size.width.toInt(), size.height.toInt())
                    val crop = if (it.cropFill) wallpaperCrop(it.image.width, it.image.height, destination.width, destination.height)
                        else intArrayOf(0, 0, it.image.width, it.image.height)
                    drawImage(it.image, srcOffset = IntOffset(crop[0], crop[1]),
                        srcSize = IntSize(crop[2], crop[3]), dstSize = destination, colorFilter = tint?.image)
                }
            }
            content()
        }
    }
}

/** The approved pixel math is exported once by GlassAssetExportTest, not repeated on phones. */
private fun decodePreparedGlassPixels(resources: Resources, key: GlassPaletteKey): GlassPixels {
    Trace.beginSection("TaktGlass.decodePrepared")
    try {
        val pair = when (key.family) {
            ThemeFamily.BLUE -> if (key.dark) R.drawable.takt_glass_blue_dark to R.drawable.takt_glass_blue_dark_frost
                else R.drawable.takt_glass_blue_light to R.drawable.takt_glass_blue_light_frost
            ThemeFamily.GREEN -> if (key.dark) R.drawable.takt_glass_green_dark to R.drawable.takt_glass_green_dark_frost
                else R.drawable.takt_glass_green_light to R.drawable.takt_glass_green_light_frost
            ThemeFamily.PURPLE -> if (key.dark) R.drawable.takt_glass_purple_dark to R.drawable.takt_glass_purple_dark_frost
                else R.drawable.takt_glass_purple_light to R.drawable.takt_glass_purple_light_frost
            ThemeFamily.WARM -> if (key.dark) R.drawable.takt_glass_warm_dark to R.drawable.takt_glass_warm_dark_frost
                else R.drawable.takt_glass_warm_light to R.drawable.takt_glass_warm_light_frost
            ThemeFamily.MONOCHROME -> if (key.dark) R.drawable.takt_glass_monochrome_dark to R.drawable.takt_glass_monochrome_dark_frost
                else R.drawable.takt_glass_monochrome_light to R.drawable.takt_glass_monochrome_light_frost
            ThemeFamily.ROSE, ThemeFamily.AMBER, ThemeFamily.CYAN -> if (key.dark)
                R.drawable.takt_glass_monochrome_dark to R.drawable.takt_glass_monochrome_dark_frost
                else R.drawable.takt_glass_monochrome_light to R.drawable.takt_glass_monochrome_light_frost
        }
        val image = checkNotNull(BitmapFactory.decodeResource(resources, pair.first))
        try {
            val frost = checkNotNull(BitmapFactory.decodeResource(resources, pair.second))
            return GlassPixels(image.asImageBitmap(), frost.asImageBitmap())
        } catch (failure: Throwable) {
            image.recycle()
            throw failure
        }
    } finally {
        Trace.endSection()
    }
}

/** Offline reference pipeline invoked by the opt-in asset exporter; never used by screen rendering. */
private fun generateGlassPixels(resources: Resources, key: GlassPaletteKey): GlassPixels {
    Trace.beginSection("TaktGlass.generate")
    var source: Bitmap? = null
    var image: Bitmap? = null
    var reduced: Bitmap? = null
    var frost: Bitmap? = null
    var published = false
    try {
        val resource = if (key.dark) R.drawable.takt_glass_dark else R.drawable.takt_glass_light
        val decoded = checkNotNull(BitmapFactory.decodeResource(resources, resource))
        source = decoded
        val silk = if (key.family == ThemeFamily.BLUE) decoded else recolorSilk(decoded, key.accent, key.dark)
        image = silk
        val scaled = Bitmap.createScaledBitmap(silk, 360, 720, true)
        reduced = scaled
        val pixels = IntArray(360 * 720)
        scaled.getPixels(pixels, 0, 360, 0, 0, 360, 720)
        val blurred = Bitmap.createBitmap(blurGlassBackdrop(pixels, 360, 720, 10), 360, 720, Bitmap.Config.ARGB_8888)
        frost = blurred
        return GlassPixels(silk.asImageBitmap(), blurred.asImageBitmap()).also { published = true }
    } finally {
        // Only unpublished/intermediate bitmaps have no readers. Cached pairs are never recycled.
        if (reduced !== image) reduced?.recycle()
        if (source !== image) source?.recycle()
        if (!published) {
            image?.recycle()
            frost?.recycle()
        }
        Trace.endSection()
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
    Box(modifier.glassSurface(shape, tint), propagateMinConstraints = true) { content() }
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
    sampleContent: Boolean = false,
    extendBackdrop: Boolean = false,
): Modifier {
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < .3f
    val appearance = LocalTaktCardAppearance.current
    val backdrop = LocalGlassBackdrop.current
    var position by remember { mutableStateOf(Offset.Zero) }
    val solidAccent = tint == colors.primary
    val tonal = appearance == CardAppearance.TONAL_FILLED
    val glass = appearance == CardAppearance.GLASS
    if (!glass) {
        val fill = opaqueSurfaceFill(if (tonal) colors.primaryContainer else colors.surface, tint)
        return this.shadow(if (tonal) 0.dp else 7.dp, shape, clip = true)
            .background(fill)
            .border(1.dp, if (tonal) colors.primary.copy(alpha = .18f) else colors.outlineVariant, shape)
    }
    val backdropTint = LocalGlassTint.current
    val contentBackdrop = LocalTaktContentBackdrop.current.takeIf { sampleContent }
    val contentPane = if (contentBackdrop != null) androidx.compose.ui.graphics.rememberGraphicsLayer() else null
    val body = if (tint != Color.Unspecified) tint else if (dark) Color(0xFF87ADD3) else Color.White
    val edge = if (dark) Color(0xFFB7DFFF) else Color.White
    return this
        .onGloballyPositioned { position = it.positionOnScreen() }
        // One render layer owns both the shadow and shape clip.
        .shadow(if (tonal) 1.dp else 7.dp, shape, clip = true,
            ambientColor = Color(0xFF315677).copy(alpha = .12f),
            spotColor = Color(0xFF315677).copy(alpha = .17f))
        .drawWithCache {
            val outline = shape.createOutline(size, layoutDirection, this)
            val frostPainter = backdrop?.pixels?.frost?.asAndroidBitmap()?.let(::GlassFrostPainter)
                ?.apply {
                    setColorFilter(backdropTint?.frost)
                    setCropFill(backdrop?.pixels?.cropFill == true)
                }
            val rim = Brush.linearGradient(
                listOf(edge.copy(alpha = if (dark) .52f else .95f), edge.copy(alpha = .22f), edge.copy(alpha = .68f)),
                start = Offset.Zero, end = Offset(size.width * .75f, size.height),
            )
            val sheen = Brush.linearGradient(
                listOf(Color.White.copy(alpha = if (dark) .12f else if (solidAccent) .08f else .34f), Color.Transparent,
                    colors.primary.copy(alpha = if (dark) .07f else .035f)),
                start = Offset.Zero, end = Offset(size.width * .35f, size.height),
            )
            if (contentPane != null && android.os.Build.VERSION.SDK_INT >= 31) {
                contentPane.renderEffect = android.graphics.RenderEffect.createBlurEffect(
                    10.dp.toPx(), 10.dp.toPx(), android.graphics.Shader.TileMode.CLAMP).asComposeRenderEffect()
            }
            onDrawBehind {
                backdrop?.let { source ->
                    if (frostPainter != null && source.sampledSize.width > 0 && source.sampledSize.height > 0) {
                        val local = position - source.sampledOrigin
                        // Lens distortion is anchored to this pane's center, not to its scrolling parent.
                        val magnification = if (glass) 1.025f else 1.01f
                        drawIntoCanvas { canvas ->
                            frostPainter.draw(canvas.nativeCanvas, local.x, local.y,
                                source.sampledSize.width, source.sampledSize.height,
                                size.width, size.height, magnification, extendBackdrop)
                        }
                    }
                }
                if (contentPane != null && contentBackdrop != null && contentBackdrop.layer.size.width > 0) {
                    val local = position - contentBackdrop.origin
                    contentPane.record(size = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt())) {
                        translate(-local.x, -local.y) {
                            drawLayer(contentBackdrop.layer)
                        }
                    }
                    drawLayer(contentPane)
                }
                drawOutline(outline, body.copy(alpha = when {
                    solidAccent -> .98f
                    dark && backdrop != null && backdrop.pixels == null -> .96f
                    backdrop?.pixels?.cropFill == true && tint == Color.Unspecified -> if (dark) .32f else .72f
                    tonal -> if (dark) .24f else .63f
                    tint != Color.Unspecified -> if (dark) .20f else .28f
                    else -> if (dark) .13f else if (glass) .52f else .62f
                }))
                drawOutline(outline, sheen)
                drawOutline(outline, rim, style = Stroke(1.4.dp.toPx()))
            }
        }
}

/** A tonal accent changes the material's colour, never its opacity over wallpaper. */
internal fun opaqueSurfaceFill(base: Color, tint: Color): Color {
    val opaqueBase = base.copy(alpha = 1f)
    return if (tint == Color.Unspecified) opaqueBase else tint.compositeOver(opaqueBase)
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

/** RGB box blur using sliding windows. Works on every supported API, once per generated pair. */
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

/** Center-crop source rectangle, shared by background drawing and tested aspect-ratio policy. */
internal fun wallpaperCrop(sourceWidth: Int, sourceHeight: Int, width: Int, height: Int): IntArray {
    if (width <= 0 || height <= 0) return intArrayOf(0, 0, sourceWidth, sourceHeight)
    val scale = maxOf(width.toFloat() / sourceWidth, height.toFloat() / sourceHeight)
    val cropWidth = (width / scale).toInt().coerceIn(1, sourceWidth)
    val cropHeight = (height / scale).toInt().coerceIn(1, sourceHeight)
    return intArrayOf((sourceWidth - cropWidth) / 2, (sourceHeight - cropHeight) / 2, cropWidth, cropHeight)
}

/** Runs once on the shared worker; cached pixels already include the readability veil. */
private fun decodePhotoPixels(context: Context, key: PhotoKey): GlassPixels {
    val file = File(File(context.filesDir, "wallpapers"), key.reference)
    require(file.length() in 1..(24L * 1024 * 1024))
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    require(bounds.outWidth in 1..2048 && bounds.outHeight in 1..2048)
    val decoded = requireNotNull(BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inMutable = true }))
    return preparePhotoPixels(decoded, key.dark)
}

/** Owns its mutable input; the same veil, crop and frost are used for saved photos and backup previews. */
private fun preparePhotoPixels(decoded: Bitmap, dark: Boolean): GlassPixels {
    var image: Bitmap? = null
    var reduced: Bitmap? = null
    var frost: Bitmap? = null
    var published = false
    try {
        val mutable = decoded
        image = mutable
        android.graphics.Canvas(mutable).drawColor(if (dark) 0x66000000 else 0x99FFFFFF.toInt())
        val scale = minOf(1f, 360f / maxOf(mutable.width, mutable.height))
        val width = maxOf(1, (mutable.width * scale).toInt())
        val height = maxOf(1, (mutable.height * scale).toInt())
        val small = Bitmap.createScaledBitmap(mutable, width, height, true)
        reduced = small
        val pixels = IntArray(width * height)
        small.getPixels(pixels, 0, width, 0, 0, width, height)
        val blurred = Bitmap.createBitmap(blurGlassBackdrop(pixels, width, height, 10), width, height, Bitmap.Config.ARGB_8888)
        frost = blurred
        return GlassPixels(mutable.asImageBitmap(), blurred.asImageBitmap(), cropFill = true).also { published = true }
    } finally {
        if (reduced !== image) reduced?.recycle()
        if (!published) { image?.recycle(); frost?.recycle() }
    }
}
