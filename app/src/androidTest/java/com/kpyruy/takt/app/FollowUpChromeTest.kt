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
            data.database.courseDao().insertAll((1..12).map { sample.copy(id = "follow-$it", title = "Predmet $it", semester = 3, status = "enrolled") })
            data.gradeRepository.observeItems("glass-physics").first().forEach { data.gradeRepository.deleteItem(it.id) }
            data.studyContentRepository.observeTasks("glass-physics").first().forEach { data.studyContentRepository.deleteTask(it.id) }
            data.gradeRepository.upsertItem(GradeItem("follow-score", "glass-physics", "Test", GradeItemType.TEST, 17.0, 20.0, completed = true))
            data.awaitRestoredPlanning()
        }
        compose.waitForIdle()
    }
    @Test fun subjectsHeaderTransitionsAndCourseworkRemainsSeventeenOfTwenty() {
        start()
        root("Предмети")
        compose.onNodeWithTag("subjects-glass-header").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "clear"))
        capture("subjects-clear")
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(8)
        compose.onNodeWithTag("subjects-glass-header").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "glass"))
        capture("subjects-glass")
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(0)
        compose.onNode(hasText("Фізика") and hasClickAction()).performClick()
        compose.onNode(hasText(t("Екзамен")) and hasClickAction()).performClick()
        compose.onNodeWithTag("exam-available-metric").assertDoesNotExist()
        compose.onNodeWithTag("exam-semester-metric").assert(hasText("17", substring = true) and hasText("/ 20", substring = true))
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
        val day = java.time.LocalDate.now()
        val event = compose.onNodeWithTag("week-timetable-event-glass-lecture")
        val before = event.fetchSemanticsNode().boundsInRoot.left
        compose.onNodeWithTag("week-timetable-days").performTouchInput { swipeLeft(durationMillis = 450) }
        val after = event.fetchSemanticsNode().boundsInRoot.left
        assertTrue("One-finger horizontal scrolling must remain enabled", after < before)
        capture("week-zoomed")
    }
    private fun root(key: String) = compose.onAllNodes(hasText(t(key)) and hasClickAction()).onLast().performClick()
    private fun capture(name: String) {
        compose.mainClock.advanceTimeBy(500); compose.waitForIdle()
        instrumentation.waitForIdleSync()
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        instrumentation.targetContext.openFileOutput("follow-$name.png", 0).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
