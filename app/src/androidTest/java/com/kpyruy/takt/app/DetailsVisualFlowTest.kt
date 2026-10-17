package com.kpyruy.takt.app

import android.graphics.Bitmap
import android.view.WindowInsets
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.Role
import androidx.test.platform.app.InstrumentationRegistry
import com.kpyruy.takt.core.model.*
import com.kpyruy.takt.core.ui.i18n.t
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class DetailsVisualFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val data get() = (compose.activity.application as TaktApplication).dataContainer

    @Test fun actualCreateAndEditRoutesKeepDetailsOrganizedAndSaveIndependentRules() {
        runBlocking {
            data.seedLiquidGlassReview()
            data.settingsRepository.setLanguage(AppLanguage.SLOVAK)
            data.settingsRepository.setThemeMode(AppThemeMode.DARK)
            data.settingsRepository.setThemeFamily(ThemeFamily.PURPLE)
            data.awaitRestoredPlanning()
        }
        compose.waitForIdle()
        dismissConnectionPrompt()
        root("Предмети")
        compose.onNodeWithTag("subjects-glass-header").assertDoesNotExist()
        compose.onNodeWithTag("subjects-header").assertIsDisplayed()
        capture("subjects-normal")
        compose.onNodeWithTag("root-add").performClick()
        compose.onNodeWithTag("global-add-TEST").performClick()
        compose.onAllNodes(hasText("Фізика") and hasClickAction()).onLast().performScrollTo().performClick()
        compose.onNodeWithTag("grade-title").performTextInput("Details flow review")
        compose.onNodeWithTag("grade-maximum").performTextInput("20")
        hideKeyboard()
        val before = compose.onNodeWithTag("grade-save").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithTag("grade-details-trigger").performClick()
        compose.onNodeWithTag("grade-details").assertIsDisplayed()
        assertEquals(before, compose.onNodeWithTag("grade-save").fetchSemanticsNode().boundsInRoot)
        compose.onNodeWithText(t("Тиждень 1 — тиждень початку навчання, з понеділка.")).assertDoesNotExist()
        compose.onNodeWithTag("grade-explicit-date").assertIsDisplayed()
        compose.onNodeWithTag("grade-result-PLANNED").assertDoesNotExist()
        capture("when-date")
        compose.onNodeWithTag("grade-date-mode-SCHEDULE").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("grade-teaching-week").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("grade-teaching-week").performClick()
        compose.onNodeWithTag("grade-week-4").performScrollTo().performClick()
        capture("when-timetable")
        compose.onNodeWithTag("grade-details-tab-RESULT").performClick()
        compose.onNodeWithTag("grade-result-KNOWN").performClick()
        compose.onNodeWithTag("grade-earned").performScrollTo().performTextReplacement("17")
        hideKeyboard()
        capture("result-known")
        compose.onNodeWithTag("grade-details-tab-RULES").performClick()
        compose.onNodeWithTag("grade-contributes").performClick()
        compose.onNodeWithTag("grade-required").performClick()
        compose.onNodeWithTag("grade-minimum").performScrollTo().performTextInput("10")
        hideKeyboard()
        capture("rules-admission")
        compose.onNodeWithTag("grade-details-done").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("grade-details").fetchSemanticsNodes().isEmpty() }
        capture("main-summary")
        compose.onNodeWithTag("grade-save").performClick()
        compose.waitUntil(5_000) { runBlocking { data.gradeRepository.observeItems("glass-physics").first().any { it.title == "Details flow review" } } }
        val saved = runBlocking { data.gradeRepository.observeItems("glass-physics").first().single { it.title == "Details flow review" } }
        assertEquals(17.0, saved.earnedPoints, 0.0)
        assertEquals(20.0, saved.maxPoints, 0.0)
        assertTrue(saved.completed)
        assertTrue(saved.requiredForExam)
        assertFalse(saved.contributesToGrade)
        assertFalse(saved.awaitingResult)
        assertEquals(10.0, saved.minimumPointsForExam!!, 0.0)
        assertNotNull(saved.dueDate)
        assertNotNull(saved.lessonId)

        runBlocking {
            data.settingsRepository.setLanguage(AppLanguage.ENGLISH)
            data.settingsRepository.setThemeMode(AppThemeMode.LIGHT)
        }
        compose.waitForIdle()
        root("Предмети")
        compose.onNode(hasText("Фізика") and hasClickAction()).performClick()
        compose.onNode(hasText(t("Задачі")) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)).performClick()
        compose.onNodeWithTag("course-work-${saved.id}").performScrollTo().performClick()
        compose.onNodeWithTag("grade-details-trigger").performClick()
        compose.onNodeWithTag("grade-details-tab-RESULT").performClick()
        compose.onNodeWithTag("grade-earned").assertTextContains("17")
        capture("light-editor-result")
        compose.onNodeWithTag("grade-result-AWAITING").performClick()
        compose.onNodeWithTag("grade-details-tab-RULES").performClick()
        compose.onNodeWithTag("grade-minimum").assertTextContains("10")
        capture("light-editor-rules")
        compose.onNodeWithTag("grade-details-done").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("grade-details").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithTag("grade-save").performScrollTo().performClick()
        compose.waitUntil(5_000) { runBlocking { data.gradeRepository.observeItems("glass-physics").first().single { it.id == saved.id }.isAwaitingResult } }
    }
    private fun hideKeyboard() {
        // The focused field may belong to the details dialog, so the Activity token is insufficient.
        if (compose.activity.window.decorView.rootWindowInsets.isVisible(WindowInsets.Type.ime())) {
            instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
            compose.waitUntil(5_000) { !compose.activity.window.decorView.rootWindowInsets.isVisible(WindowInsets.Type.ime()) }
        }
    }
    private fun root(key: String) = compose.onAllNodes(hasText(t(key)) and hasClickAction()).onLast().performClick()
    private fun dismissConnectionPrompt() {
        val id = compose.activity.resources.getIdentifier("m3c_snackbar_dismiss", "string", compose.activity.packageName)
        if (id == 0) return
        val labels = listOf(java.util.Locale.ENGLISH, java.util.Locale("sk"), java.util.Locale("uk")).map {
            compose.activity.createConfigurationContext(android.content.res.Configuration(compose.activity.resources.configuration).apply { setLocale(it) }).getString(id)
        }
        val nodes = compose.onAllNodes(labels.map { hasContentDescription(it) }.reduce { a,b -> a or b })
        compose.waitUntil(7_000) { nodes.fetchSemanticsNodes().isNotEmpty() }
        nodes.onFirst().performClick()
    }
    private fun capture(name: String) {
        compose.mainClock.advanceTimeBy(500); compose.waitForIdle(); instrumentation.waitForIdleSync()
        android.os.SystemClock.sleep(350) // Commit native dialog/window frames after Compose is idle.
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        instrumentation.targetContext.openFileOutput("details-$name.png", 0).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
