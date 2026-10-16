package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.AppSettings
import org.junit.Assert.*
import org.junit.Test

class WallpaperSettingsTest {
    private val reference = "wallpaper-12345678-1234-1234-1234-123456789abc.jpg"
    @Test fun durableReferenceSurvivesDecode() {
        assertEquals(reference, StoredSettingsCodec.decode(null, false, null, customWallpaper = reference).customWallpaper)
    }
    @Test fun invalidAndExternalReferencesAreRejected() {
        listOf("../secret.jpg", "/data/photo.jpg", "content://photo", "wallpaper-oops.jpg").forEach {
            assertNull(StoredSettingsCodec.decode(null, false, null, customWallpaper = it).customWallpaper)
        }
    }
    @Test fun resetSelectsBuiltIn() {
        assertNull(AppSettings(customWallpaper = reference).copy(customWallpaper = null).normalizedAppearance().customWallpaper)
    }
    @Test fun privateWallpaperDoesNotLeaveInBackup() {
        assertEquals(AppSettings().toBackup(), AppSettings(customWallpaper = reference).toBackup())
    }
    @Test fun restoreKeepsThisDevicesWallpaper() {
        assertEquals(reference, AppSettings().toBackup().toModel(localWallpaper = reference).customWallpaper)
    }
    @Test fun importBoundsLimitBytesAndPixels() {
        assertEquals(2048 to 1024, WallpaperImageLimits.outputSize(8192, 4096))
        assertEquals(100 to 200, WallpaperImageLimits.outputSize(100, 200))
        assertThrows(IllegalArgumentException::class.java) { WallpaperImageLimits.outputSize(50000, 50000) }
        assertThrows(IllegalArgumentException::class.java) { WallpaperImageLimits.outputSize(0, 100) }
    }
}
