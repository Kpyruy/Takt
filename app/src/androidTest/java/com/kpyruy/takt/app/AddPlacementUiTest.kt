package com.kpyruy.takt.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.kpyruy.takt.core.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Exercises the actual Appearance -> persisted settings -> root navigation path. */
class AddPlacementUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun appearanceChoiceChangesTheRealNavigationAndCanReturnToCenter() {
        val data = (compose.activity.application as TaktApplication).dataContainer
        val old = data.settingsRepository.settings.value
        try {
            runBlocking {
                data.settingsRepository.setLanguage(AppLanguage.ENGLISH)
                data.settingsRepository.setAppearance(old.copy(addButtonPlacement = AddButtonPlacement.CENTER))
            }
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("root-navigation").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("Settings").performClick()
            compose.onNodeWithText("Appearance").performScrollTo().performClick()
            compose.onNodeWithTag("appearance-add-FLOATING").performScrollTo().performClick().assertIsSelected()
            captureStudiesScreenshot("appearance-add-options.png")
            compose.onNodeWithText("Apply appearance").performClick()
            compose.waitForIdle()
            assertEquals(AddButtonPlacement.FLOATING, data.settingsRepository.settings.value.addButtonPlacement)
            compose.onNodeWithContentDescription("Back").performClick()
            compose.onNodeWithTag("floating-add").assertIsDisplayed()
            captureStudiesScreenshot("home-floating-navigation.png")
            compose.onNodeWithTag("root-add").performClick()
            compose.onNodeWithTag("root-add").assertIsSelected()
            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
            compose.onNodeWithContentDescription("Settings").performClick()
            compose.onNodeWithText("Appearance").performScrollTo().performClick()
            compose.onNodeWithTag("appearance-add-FLOATING").performScrollTo().assertIsSelected()
            compose.onNodeWithTag("appearance-add-CENTER").performScrollTo().performClick()
            compose.onNodeWithText("Apply appearance").performClick()
            compose.onNodeWithContentDescription("Back").performClick()
            compose.onNodeWithTag("floating-add").assertDoesNotExist()
            compose.onNodeWithTag("root-add").assertIsDisplayed()
        } finally {
            runBlocking { data.settingsRepository.setAppearance(old); data.settingsRepository.setLanguage(old.language) }
        }
    }
}
