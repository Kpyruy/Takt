package com.kpyruy.takt.app

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.kpyruy.takt.core.model.AppLanguage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class GuidedTourTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val data get() = (compose.activity.application as TaktApplication).dataContainer

    @Before fun language() {
        runBlocking { data.settingsRepository.setLanguage(AppLanguage.UKRAINIAN) }
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("root-navigation").fetchSemanticsNodes().isNotEmpty() }
    }

    private fun start() {
        compose.onAllNodesWithContentDescription("Налаштування").onFirst().performClick()
        compose.onNodeWithText("Екскурсія по Takt").performScrollTo().performClick()
        compose.onNodeWithTag("tour-card-TODAY_WEEK").assertIsDisplayed()
    }

    @Test fun everyStepHasAVisibleTargetAndCompletionDoesNotChangeUserData() {
        val before = runBlocking { data.backupRepository.exportJson() }
        start()
        GuidedTourStep.entries.forEach { step ->
            compose.onNodeWithTag("tour-card-${step.name}").assertIsDisplayed()
            compose.waitUntil(5_000) { compose.onAllNodesWithTag("tour-spotlight").fetchSemanticsNodes().isNotEmpty() }
            val card = compose.onNodeWithTag("tour-card-${step.name}").fetchSemanticsNode().boundsInRoot
            val target = compose.onNodeWithTag("tour-spotlight").fetchSemanticsNode().boundsInRoot
            assertFalse("${step.name}: card covers its target", card.overlaps(target))
            screenshot("tour-${step.name}.png")
            compose.onNodeWithTag("tour-next").performClick()
        }
        compose.onNodeWithTag("tour-completed").assertIsDisplayed()
        compose.onNodeWithText("Приємного користування аплікацією").assertIsDisplayed()
        screenshot("tour-completed.png")
        compose.onNodeWithTag("tour-done").performClick()
        compose.onNodeWithTag("root-navigation").assertIsDisplayed()
        assertEquals(before, runBlocking { data.backupRepository.exportJson() })
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("guided-tour").assertDoesNotExist()
        compose.onNodeWithTag("root-navigation").assertIsDisplayed()
    }

    @Test fun exitRequiresConfirmationAndRecreationKeepsCurrentStep() {
        start()
        compose.onNodeWithTag("tour-next").performClick()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("tour-card-TODAY_COURSE").assertIsDisplayed()
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag("tour-exit-cancel").performClick()
        compose.onNodeWithTag("tour-card-TODAY_COURSE").assertIsDisplayed()
        compose.onNodeWithTag("tour-back").performClick()
        compose.onNodeWithTag("tour-card-TODAY_WEEK").assertIsDisplayed()
        compose.onNodeWithTag("tour-exit").performClick()
        compose.onNodeWithTag("tour-exit-confirm").performClick()
        compose.onNodeWithTag("tour-completed").assertDoesNotExist()
        compose.onNodeWithTag("root-navigation").assertIsDisplayed()
        start()
        compose.onNodeWithTag("tour-exit").performClick()
        compose.onNodeWithTag("tour-exit-confirm").performClick()
    }

    @Test fun examplesAreReadOnlyAndAllStepsHaveEnglishAndSlovakCopy() {
        val original = runBlocking { data.backupRepository.exportJson() }
        val demo = GuidedTourData(data.settingsRepository.settings.value)
        runBlocking {
            demo.setIcon(demo.courses.first().id, "Code")
            demo.deleteNote("tour-note")
            demo.setLanguage(AppLanguage.SLOVAK)
            demo.setCurrentSemester(7)
        }
        assertEquals("Atom", demo.courses.first().iconKey)
        assertEquals(1, demo.settings.value.currentSemester)
        assertEquals(original, runBlocking { data.backupRepository.exportJson() })
        for (language in listOf(AppLanguage.ENGLISH, AppLanguage.SLOVAK)) {
            GuidedTourStep.entries.forEach {
                assertFalse(it.title.text(language).contains(Regex("[А-Яа-яІіЇїЄє]")))
                assertFalse(it.body.text(language).contains(Regex("[А-Яа-яІіЇїЄє]")))
            }
        }
    }

    private fun screenshot(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        instrumentation.targetContext.openFileOutput(name, 0).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
