package com.kpyruy.takt.app

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.view.WindowCompat
import androidx.test.platform.app.InstrumentationRegistry
import com.kpyruy.takt.core.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.After
import org.junit.Rule
import org.junit.Test

class OnboardingChromeColdTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private var previousComplete: Boolean? = null
    @After fun restoreSetupFlag() {
        previousComplete?.let {
            compose.activity.getSharedPreferences("takt_onboarding", 0).edit().putBoolean("complete", it).commit()
        }
    }
    @Test fun unsavedDarkDraftOwnsSystemBarsAndFooterAfterColdSetup() {
        assumeTrue(android.os.Build.HARDWARE == "ranchu")
        val data = (compose.activity.application as TaktApplication).dataContainer
        runBlocking {
            data.database.clearAllTables()
            data.settingsRepository.setThemeMode(AppThemeMode.LIGHT)
            data.settingsRepository.setThemeFamily(ThemeFamily.BLUE)
            data.settingsRepository.setLanguage(AppLanguage.SLOVAK)
        }
        val setup = compose.activity.getSharedPreferences("takt_onboarding", 0)
        previousComplete = setup.getBoolean("complete", false)
        setup.edit().putBoolean("complete", false).commit()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("welcome-local").performScrollTo().performClick()
        compose.onNodeWithTag("onboarding-theme-DARK").performScrollTo().performClick()
        repeat(2) { compose.onNodeWithTag("onboarding-primary").performClick() }
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        android.os.SystemClock.sleep(400)
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        instrumentation.targetContext.openFileOutput("onboarding-draft-system-bars.png", 0).use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        };bitmap.recycle()
        assertEquals("Draft must stay unsaved", AppThemeMode.LIGHT, data.settingsRepository.settings.value.themeMode)
        compose.runOnIdle {
            val controller = WindowCompat.getInsetsController(compose.activity.window, compose.activity.window.decorView)
            assertFalse("Dark draft requires light status icons immediately", controller.isAppearanceLightStatusBars)
            assertFalse("Dark draft requires matching navigation contrast immediately", controller.isAppearanceLightNavigationBars)
        }
    }
}
