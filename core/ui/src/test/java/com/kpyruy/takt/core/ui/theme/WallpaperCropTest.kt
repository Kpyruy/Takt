package com.kpyruy.takt.core.ui.theme

import com.kpyruy.takt.core.ui.components.wallpaperCrop
import org.junit.Assert.assertEquals
import org.junit.Test

class WallpaperCropTest {
    @Test fun portraitViewportCropsLandscapePhotoAtItsCenter() {
        assertEquals(listOf(750, 0, 500, 1000), wallpaperCrop(2000, 1000, 500, 1000).toList())
    }
    @Test fun landscapeViewportCropsPortraitPhotoAtItsCenter() {
        assertEquals(listOf(0, 750, 1000, 500), wallpaperCrop(1000, 2000, 1000, 500).toList())
    }
}
