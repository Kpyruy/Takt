package com.kpyruy.takt.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import com.kpyruy.takt.core.data.SharedPreferencesAppSettingsRepository
import com.kpyruy.takt.core.data.WallpaperStore
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class WallpaperStoreTest {
    @Test fun importOwnsBoundedPixelsAndPersistsSelectionAcrossRepositoryRestart() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = "wallpaper_test_${System.nanoTime()}"
        val repository = SharedPreferencesAppSettingsRepository(context, prefs)
        val actualApplied = SharedPreferencesAppSettingsRepository(context).settings.value.customWallpaper
        val source = File.createTempFile("wallpaper-source", ".png", context.cacheDir)
        var imported: File? = null
        try {
            val bitmap = Bitmap.createBitmap(3000, 1500, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(android.graphics.Color.MAGENTA)
            source.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
            val reference = WallpaperStore(context).importImage(Uri.fromFile(source), actualApplied)
            imported = File(File(context.filesDir, "wallpapers"), reference)
            assertTrue(imported.isFile)
            source.delete()
            val owned = BitmapFactory.decodeFile(imported.path)
            assertNotNull(owned)
            assertTrue(owned.width <= 2048 && owned.height <= 2048)
            assertEquals(2f, owned.width.toFloat() / owned.height, .01f)
            owned.recycle()
            repository.setAppearance(repository.settings.value.copy(customWallpaper = reference))
            assertEquals(reference, SharedPreferencesAppSettingsRepository(context, prefs).settings.value.customWallpaper)
            repository.setLanguage(com.kpyruy.takt.core.model.AppLanguage.SLOVAK)
            repository.setThemeMode(com.kpyruy.takt.core.model.AppThemeMode.DARK)
            assertEquals(reference, SharedPreferencesAppSettingsRepository(context, prefs).settings.value.customWallpaper)
            repository.setAppearance(repository.settings.value.copy(customWallpaper = null))
            assertNull(SharedPreferencesAppSettingsRepository(context, prefs).settings.value.customWallpaper)
        } finally {
            imported?.delete()
            source.delete()
            context.deleteSharedPreferences(prefs)
        }
    }

    @Test fun malformedInputIsRejectedWithoutPublishingWallpaper() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = File.createTempFile("invalid-wallpaper", ".jpg", context.cacheDir)
        try {
            source.writeText("not an image")
            try {
                WallpaperStore(context).importImage(Uri.fromFile(source), null)
                fail("Malformed image accepted")
            } catch (_: IllegalArgumentException) { }
        } finally { source.delete() }
    }
}
