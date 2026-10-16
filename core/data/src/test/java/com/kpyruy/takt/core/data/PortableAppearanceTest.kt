package com.kpyruy.takt.core.data
import org.junit.Assert.*
import org.junit.Test

class PortableAppearanceTest {
    @Test fun appearanceSelectionCarriesPhotoAndNamedThemeButOtherSectionsDoNot() {
        val photo = BackupWallpaper("My background", "/9j/AA==", "test")
        val local = BackupPayload(settings = BackupSettings())
        val file = BackupPayload(settings = BackupSettings(wallpaperRecorded = true, wallpaper = photo,
            customThemes = listOf(BackupCustomColorTheme("mine", "Evening", "#AA6600", "#663399", "#008877")), selectedCustomThemeId = "mine"))
        assertEquals(photo, BackupMerger.merge(local, file, setOf(BackupSection.APPEARANCE)).settings.wallpaper)
        assertEquals("mine", BackupMerger.merge(local, file, setOf(BackupSection.APPEARANCE)).settings.selectedCustomThemeId)
        assertNull(BackupMerger.merge(local, file, setOf(BackupSection.SCHEDULE)).settings.wallpaper)
        assertTrue(BackupSection.APPEARANCE in BackupMerger.changedSections(local, file))
    }
    @Test fun legacyBackupWithoutPhotoKeepsCurrentPhoto() {
        val photo = BackupWallpaper("Current", "/9j/AA==", "test")
        val local = BackupPayload(settings = BackupSettings(wallpaperRecorded = true, wallpaper = photo))
        val legacy = BackupPayload(settings = BackupSettings(themeFamily = "GREEN"))
        assertEquals(photo, BackupMerger.merge(local, legacy, setOf(BackupSection.APPEARANCE)).settings.wallpaper)
    }
}
