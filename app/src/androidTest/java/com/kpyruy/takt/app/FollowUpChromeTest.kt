package com.kpyruy.takt.app

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.kpyruy.takt.core.model.*
import com.kpyruy.takt.core.ui.i18n.t
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Emulator-only candidate checks for the second October14 review. */
class FollowUpChromeTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val data get() = (compose.activity.application as TaktApplication).dataContainer
    private fun start() {
        runBlocking {
            data.seedLiquidGlassReview()
            data.settingsRepository.setLanguage(AppLanguage.SLOVAK)
            data.settingsRepository.setThemeMode(AppThemeMode.DARK)
            data.settingsRepository.setThemeFamily(ThemeFamily.PURPLE)
            val sample = data.database.courseDao().observeAll().first().first()
            data.database.courseDao().insertAll((1..12).map { sample.copy(id = "follow-$it", title = "Я Predmet $it", semester = 3, status = "enrolled") })
            data.gradeRepository.observeItems("glass-physics").first().forEach { data.gradeRepository.deleteItem(it.id) }
            data.studyContentRepository.observeTasks("glass-physics").first().forEach { data.studyContentRepository.deleteTask(it.id) }
            data.gradeRepository.upsertItem(GradeItem("follow-score", "glass-physics", "Test", GradeItemType.TEST, 17.0, 20.0, completed = true))
            data.awaitRestoredPlanning()
        }
        compose.waitForIdle()
        dismissConnectionPrompt()
    }
    @Test fun subjectsUsesNormalHeaderWhileCourseworkRemainsSeventeenOfTwenty() {
        start()
        root("Предмети")
        compose.onNodeWithTag("subjects-glass-header").assertDoesNotExist()
        val headerBefore = compose.onNodeWithTag("subjects-header").fetchSemanticsNode().boundsInRoot
        capture("subjects-normal")
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(8)
        compose.onNodeWithTag("subjects-glass-header").assertDoesNotExist()
        assertEquals(headerBefore, compose.onNodeWithTag("subjects-header").fetchSemanticsNode().boundsInRoot)
        capture("subjects-normal-scrolled")
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(0)
        compose.onNode(hasText("Фізика") and hasClickAction()).performClick()
        compose.onNode(hasText(t("Екзамен")) and hasClickAction()).performClick()
        compose.onNodeWithTag("exam-available-metric").assertDoesNotExist()
        compose.onNode(hasText("17") and hasAnyAncestor(hasTestTag("exam-semester-metric"))).assertExists()
        compose.onNode(hasText("/ 20") and hasAnyAncestor(hasTestTag("exam-semester-metric"))).assertExists()
        capture("exam-coursework")
    }
    @Test fun weekHidesDayStripPinchesAndStillScrollsWithOneFinger() {
        start()
        root("Календар")
        compose.onNodeWithTag("calendar-week-strip").assertIsDisplayed()
        compose.onNodeWithTag("calendar-view-week").performClick()
        compose.onNodeWithTag("calendar-week-strip").assertDoesNotExist()
        val grid = compose.onNodeWithTag("week-timetable")
        grid.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "100%"))
        capture("week-before")
        grid.performTouchInput {
            val a = Offset(center.x - width * .13f, center.y)
            val b = Offset(center.x + width * .13f, center.y)
            down(0, a); down(1, b)
            for (step in 1..10) {
                moveTo(0, a - Offset(step * 3f, 0f), delayMillis = 16)
                moveTo(1, b + Offset(step * 3f, 0f), delayMillis = 16)
            }
            up(0); up(1)
        }
        val scaled = grid.fetchSemanticsNode().config[SemanticsProperties.StateDescription].removeSuffix("%").toInt()
        assertTrue("Pinch must scale the temporal grid", scaled > 100)
        val event = compose.onNodeWithTag("week-timetable-event-glass-lecture")
        val before = event.fetchSemanticsNode().boundsInRoot.left
        compose.onNodeWithTag("week-timetable-days").performTouchInput { swipeLeft(durationMillis = 450) }
        val after = event.fetchSemanticsNode().boundsInRoot.left
        assertTrue("One-finger horizontal scrolling must remain enabled", after < before)
        capture("week-zoomed")
    }
    @Test fun captureNewFormsPaletteAndNoteReader() {
        start()
        root("Предмети")
        compose.onNode(hasText("Фізика") and hasClickAction()).performClick()
        compose.onNode(hasText(t("Нотатки")) and SemanticsMatcher.expectValue(SemanticsProperties.Role, androidx.compose.ui.semantics.Role.Tab)).performClick()
        capture("notes-preview")
        compose.onNodeWithTag("note-preview-glass-formulas").performClick()
        compose.onNodeWithTag("note-reader").assertIsDisplayed()
        capture("note-reader")
        compose.onNodeWithTag("note-reader-close").performClick()
        compose.onNodeWithTag("subject-header-back").performClick()
        compose.onNodeWithTag("root-add").performClick()
        capture("add-menu")
        compose.onNodeWithTag("global-add-CLASS").performClick()
        compose.onNodeWithTag("lesson-type-trigger").performScrollTo().performClick()
        capture("lesson-type-menu")
        nativeBack()
        nativeBack()
        compose.onNodeWithTag("root-add").performClick()
        compose.onNodeWithTag("global-add-TEST").performClick()
        compose.onAllNodes(hasText("Фізика") and hasClickAction()).onLast().performScrollTo().performClick()
        capture("assessment-compact")
        compose.onNodeWithTag("grade-maximum").performClick()
        compose.onNodeWithTag("grade-maximum").performTextInput("20")
        capture("assessment-keyboard")
        nativeBack()
        compose.onNodeWithTag("grade-details-trigger").performScrollTo().performClick()
        capture("assessment-details")
        compose.onNodeWithTag("grade-details-done").performClick()
        nativeBack()
        root("Предмети")
        compose.onNodeWithTag("subjects-settings").performClick()
        compose.onNode(hasText(t("Вигляд")) and hasClickAction()).performScrollTo().performClick()
        compose.onNodeWithTag("appearance-color-open").performScrollTo().performClick()
        compose.onNodeWithTag("theme-create").performScrollTo()
        compose.onNodeWithTag("palette-live-preview").assertIsDisplayed()
        capture("palette-pinned")
    }
    private fun nativeBack() {
        instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        instrumentation.waitForIdleSync()
    }
    private fun dismissConnectionPrompt() {
        val dismissId = compose.activity.resources.getIdentifier("m3c_snackbar_dismiss", "string", compose.activity.packageName)
        if (dismissId != 0) {
            val labels = listOf(java.util.Locale.ENGLISH, java.util.Locale("sk"), java.util.Locale("uk")).map {
                compose.activity.createConfigurationContext(android.content.res.Configuration(compose.activity.resources.configuration).apply { setLocale(it) }).getString(dismissId)
            }
            val nodes = compose.onAllNodes(labels.map { hasContentDescription(it) }.reduce { a,b -> a or b })
            compose.waitUntil(7_000) { nodes.fetchSemanticsNodes().isNotEmpty() }
            nodes.onFirst().performClick()
        }
    }
    private fun root(key: String) = compose.onAllNodes(hasText(t(key)) and hasClickAction()).onLast().performClick()
    private fun capture(name: String) {
        compose.mainClock.advanceTimeBy(500); compose.waitForIdle()
        compose.waitForIdle()
        instrumentation.waitForIdleSync()
        // Android dialog/window transitions use the compositor clock, outside Compose's test clock.
        android.os.SystemClock.sleep(400)
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        instrumentation.targetContext.openFileOutput("follow-$name.png", 0).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
