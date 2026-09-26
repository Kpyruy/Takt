package com.kpyruy.takt.core.ui.theme

import com.kpyruy.takt.core.model.AppSettings
import com.kpyruy.takt.core.model.AppThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TaktWindowBackgroundTest {
    @Test fun savedDarkThemeWinsOverLightPhoneSetting() {
        val dark = taktWindowBackgroundColor(AppSettings(themeMode = AppThemeMode.DARK), systemDark = false)
        val light = taktWindowBackgroundColor(AppSettings(themeMode = AppThemeMode.LIGHT), systemDark = false)
        assertNotEquals(light, dark)
        assertEquals(dark, taktWindowBackgroundColor(AppSettings(), systemDark = true))
    }
}
