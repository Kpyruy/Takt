package com.kpyruy.takt.app

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.kpyruy.takt.core.model.AppLanguage
import com.kpyruy.takt.core.ui.i18n.t
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/** Guards the valid lock and appearance-preview siblings while removing duplicate wallpaper. */
class GlassBackdropOwnershipTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun incompleteSetupWithLockedAccountStillHasWallpaper() {
        assumeTrue(android.os.Build.HARDWARE == "ranchu")
        val app = compose.activity.application as TaktApplication
        runBlocking { app.dataContainer.settingsRepository.setLanguage(AppLanguage.ENGLISH) }
        app.dataContainer.database.clearAllTables()
        compose.activity.getSharedPreferences("takt_onboarding", 0).edit().putBoolean("complete", false).commit()
        val field = app.dataContainer.universityAccountRepository.javaClass.getDeclaredField("activeState").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val account = field.get(app.dataContainer.universityAccountRepository) as MutableStateFlow<Boolean>
        try {
            compose.runOnIdle { app.deviceUnlocked = false; account.value = true }
            compose.activityRule.scenario.recreate()
            compose.waitUntil(5_000) { compose.onAllNodesWithText("Your data is protected").fetchSemanticsNodes().isNotEmpty() }
            compose.onAllNodesWithTag("glass-wallpaper").assertCountEquals(1)
            capture("incomplete-onboarding-lock")
        } finally {
            compose.runOnIdle { account.value = false }
            compose.activity.getSharedPreferences("takt_onboarding", 0).edit().putBoolean("complete", true).commit()
        }
    }

    @Test fun darkAppearanceDraftSamplesDarkFrostWithoutAnotherWallpaper() {
        val app = compose.activity.application as TaktApplication
        runBlocking { app.dataContainer.settingsRepository.setLanguage(AppLanguage.ENGLISH) }
        compose.waitForIdle()
        compose.onNodeWithContentDescription(t("Settings")).performClick()
        compose.onNode(hasText(t("Appearance")) and hasClickAction()).performScrollTo().performClick()
        compose.onNode(hasText("Dark") and hasClickAction()).performScrollTo().performClick()
        compose.onNodeWithTag("appearance-color-open").performScrollTo().performClick()
        compose.onAllNodesWithTag("glass-wallpaper").assertCountEquals(1)
        compose.onNodeWithText("Your first class").assertIsDisplayed()
        capture("dark-appearance-preview")
    }

    private fun capture(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        android.os.SystemClock.sleep(500)
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        instrumentation.targetContext.openFileOutput("$name.png", 0).use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }
}
