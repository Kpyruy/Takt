package com.kpyruy.takt.app

import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import com.kpyruy.takt.core.model.ThemeFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Offline asset build step: freezes the approved pixel pipeline, never ships in the app. */
class GlassAssetExportTest {
    @Test fun exportExactApprovedPalettesAndFrost() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("exportGlassAssets") == "true")
        assumeTrue(Build.HARDWARE == "ranchu" && Build.VERSION.SDK_INT >= 30)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        withContext(Dispatchers.Default) {
            val paletteMethod = Class.forName("com.kpyruy.takt.core.ui.theme.TaktPaletteKt")
                .getDeclaredMethod("paletteFor", ThemeFamily::class.java)
            val keyClass = Class.forName("com.kpyruy.takt.core.ui.components.GlassPaletteKey")
            val keyConstructor = keyClass.declaredConstructors.first { it.parameterCount == 3 }.apply { isAccessible = true }
            val generate = Class.forName("com.kpyruy.takt.core.ui.components.TaktGlassKt")
                .getDeclaredMethod("generateGlassPixels", android.content.res.Resources::class.java, keyClass)
                .apply { isAccessible = true }
            for (family in ThemeFamily.entries) for (dark in listOf(false, true)) {
                val palette = paletteMethod.invoke(null, family)
                val colors = palette.javaClass.getDeclaredMethod(if (dark) "getDark" else "getLight")
                    .invoke(palette) as ColorScheme
                val key = keyConstructor.newInstance(dark, family, colors.primary.value.toLong())
                val pixels = generate.invoke(null, context.applicationContext.resources, key)
                for (part in listOf("image", "frost")) {
                    val field = pixels.javaClass.getDeclaredField(part).apply { isAccessible = true }
                    val bitmap = (field.get(pixels) as ImageBitmap).asAndroidBitmap()
                    val name = "glass_${family.name.lowercase()}_${if (dark) "dark" else "light"}_$part.webp"
                    context.openFileOutput(name, 0).use {
                        check(bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, 100, it))
                    }
                }
                Log.i("TaktAssetExport", "Exported $family dark=$dark")
            }
        }
    }
}
