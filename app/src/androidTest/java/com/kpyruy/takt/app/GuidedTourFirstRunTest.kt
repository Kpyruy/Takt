package com.kpyruy.takt.app

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.kpyruy.takt.core.model.AppLanguage
import com.kpyruy.takt.core.model.AppThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/** Disposable emulator only; restore the exact original data and setup flag afterwards. */
class GuidedTourFirstRunTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val data get() = (compose.activity.application as TaktApplication).dataContainer
    private var original: String? = null
    private var complete = true

    @After fun restore() {
        original?.let { backup ->
            runBlocking { data.backupRepository.importJson(backup) }
            compose.activity.getSharedPreferences("takt_onboarding", 0).edit().putBoolean("complete", complete).commit()
        }
    }

    private fun freshSetup() {
        assumeTrue(android.os.Build.HARDWARE == "ranchu")
        original = runBlocking { data.backupRepository.exportJson() }
        complete = compose.activity.getSharedPreferences("takt_onboarding", 0).getBoolean("complete", false)
        runBlocking { data.database.clearAllTables(); data.settingsRepository.setLanguage(AppLanguage.UKRAINIAN) }
        compose.activity.getSharedPreferences("takt_onboarding", 0).edit().putBoolean("complete", false).commit()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("welcome-local").performScrollTo().performClick()
    }

    @Test fun firstLaunchOpensRealTourWithSlovakDarkThemeWithoutSeedingCourses() {
        freshSetup()
        compose.onNodeWithTag("onboarding-language-SLOVAK").performClick()
        compose.onNodeWithTag("onboarding-theme-DARK").performScrollTo().performClick()
        repeat(3) { compose.onNodeWithTag("onboarding-primary").performClick() }
        compose.onNodeWithTag("tour-card-TODAY_WEEK").assertIsDisplayed()
        compose.onNodeWithText("Tvoj týždeň jedným potiahnutím").assertIsDisplayed()
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        compose.activity.openFileOutput("tour-sk-dark-first.png", 0).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        assertTrue(runBlocking { data.studyPlanRepository.observeCourses().first() }.isEmpty())
        assertEquals(AppThemeMode.DARK, data.settingsRepository.settings.value.themeMode)
        compose.onNodeWithTag("tour-exit").performClick()
        compose.onNodeWithTag("tour-exit-confirm").performClick()
        compose.onNodeWithTag("root-navigation").assertIsDisplayed()
    }

    @Test fun freshSetupCanStartWithoutTourAndDoesNotForceItOnNextLaunch() {
        freshSetup()
        repeat(2) { compose.onNodeWithTag("onboarding-primary").performClick() }
        compose.onNodeWithText("Почати без туру").performClick()
        compose.onNodeWithTag("root-navigation").assertIsDisplayed()
        compose.onNodeWithTag("guided-tour").assertDoesNotExist()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("guided-tour").assertDoesNotExist()
        compose.onNodeWithTag("root-navigation").assertIsDisplayed()
    }
}
