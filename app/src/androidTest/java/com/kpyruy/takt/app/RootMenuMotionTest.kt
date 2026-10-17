package com.kpyruy.takt.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import com.kpyruy.takt.core.model.AppLanguage
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class RootMenuMotionTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Before fun useReviewLanguage() {
        runBlocking {
            val data = (compose.activity.application as TaktApplication).dataContainer
            data.settingsRepository.setLanguage(AppLanguage.UKRAINIAN)
            data.awaitRestoredPlanning()
        }
        compose.waitForIdle()
    }

    @Test fun rapidTapsKeepFirstTransitionAndDoNotQueueAnotherScreen() {
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Твій день").fetchSemanticsNodes().isNotEmpty() }
        compose.mainClock.autoAdvance = false
        try {
            val actions = listOf("Календар", "Предмети", "Прогрес").map { label ->
                compose.onAllNodes(hasText(label) and hasClickAction()).onLast().fetchSemanticsNode()
                    .config[androidx.compose.ui.semantics.SemanticsActions.OnClick].action!!
            }
            compose.runOnIdle { repeat(10) { actions.forEach { it() } } }
            compose.mainClock.advanceTimeBy(700)
            calendarTitle().assertIsDisplayed()
            compose.onAllNodes(hasText("Календар") and hasClickAction()).onLast().assertIsSelected()
            // Production uses a monotonic wall clock; virtual animation time must not unlock it.
            android.os.SystemClock.sleep(130)
            compose.onAllNodes(hasText("Сьогодні") and hasClickAction()).onLast().performClick()
            compose.mainClock.advanceTimeBy(700)
        } finally { compose.mainClock.autoAdvance = true }
        compose.onNodeWithText("Твій день").assertIsDisplayed()
        compose.onAllNodesWithTag("glass-wallpaper").assertCountEquals(1)
    }

    @Test fun subjectTabsAlsoIgnoreAlternatingSpamAndUnlockAfterTheCooldown() {
        runBlocking { (compose.activity.application as TaktApplication).dataContainer.seedLiquidGlassReview() }
        runBlocking { (compose.activity.application as TaktApplication).dataContainer.settingsRepository.setLanguage(AppLanguage.UKRAINIAN) }
        compose.waitForIdle()
        compose.onAllNodes(hasText("Предмети") and hasClickAction()).onLast().performClick()
        compose.onNode(hasText("Фізика") and hasClickAction()).performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("subject-header-avatar").fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        try {
            val task = subjectTab("Задачі").fetchSemanticsNode()
                .config[androidx.compose.ui.semantics.SemanticsActions.OnClick].action!!
            val exam = subjectTab("Екзамен").fetchSemanticsNode()
                .config[androidx.compose.ui.semantics.SemanticsActions.OnClick].action!!
            compose.runOnIdle { repeat(15) { task(); exam() } }
            compose.mainClock.advanceTimeBy(700)
            subjectTab("Задачі").assertIsSelected()
            android.os.SystemClock.sleep(130)
            subjectTab("Екзамен").performClick()
            compose.mainClock.advanceTimeBy(700)
            subjectTab("Екзамен").assertIsSelected()
        } finally { compose.mainClock.autoAdvance = true }
    }

    @Test fun rootSwitchIsAcceptedAfter120msWhilePreviousAnimationIsStillRunning() {
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Твій день").fetchSemanticsNodes().isNotEmpty() }
        compose.mainClock.autoAdvance = false
        try {
            compose.onAllNodes(hasText("Календар") and hasClickAction()).onLast().performClick()
            compose.mainClock.advanceTimeBy(128)
            android.os.SystemClock.sleep(130)
            compose.onAllNodes(hasText("Предмети") and hasClickAction()).onLast().performClick()
            compose.mainClock.advanceTimeBy(32)
            compose.onAllNodes(hasText("Предмети") and hasClickAction()).onLast().assertIsSelected()
            compose.mainClock.advanceTimeBy(700)
            compose.onNodeWithTag("subjects-header").assertIsDisplayed()
        } finally { compose.mainClock.autoAdvance = true }
    }

    @Test fun tappingTabsSlidesBothScreensAndReverses() {
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Твій день").fetchSemanticsNodes().isNotEmpty() }
        val originalHomeX = compose.onNodeWithText("Твій день").fetchSemanticsNode().positionInRoot.x
        compose.mainClock.autoAdvance = false
        try {
            compose.onNodeWithText("Календар").performClick()
            compose.mainClock.advanceTimeBy(96)
            val outgoingX = compose.onNodeWithText("Твій день").fetchSemanticsNode().positionInRoot.x
            val incomingX = calendarTitle().fetchSemanticsNode().positionInRoot.x
            assertTrue("Home moves left", outgoingX < originalHomeX)
            assertTrue("Calendar enters from right", incomingX > originalHomeX)
            compose.mainClock.advanceTimeBy(700)
            val originalCalendarX = calendarTitle().fetchSemanticsNode().positionInRoot.x
            android.os.SystemClock.sleep(130)
            compose.onNodeWithText("Сьогодні").performClick()
            compose.mainClock.advanceTimeBy(96)
            assertTrue("Calendar moves right", calendarTitle().fetchSemanticsNode().positionInRoot.x > originalCalendarX)
            assertTrue("Home enters from left", compose.onNodeWithText("Твій день").fetchSemanticsNode().positionInRoot.x < originalHomeX)
            compose.mainClock.advanceTimeBy(700)
        } finally { compose.mainClock.autoAdvance = true }
        compose.onNodeWithText("Твій день").assertIsDisplayed()
    }

    @Test fun swipingBlankSpaceDoesNotChangeTabsButWeekStripChangesWeek() {
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Твій день").fetchSemanticsNodes().isNotEmpty() }
        val before = selectedDay()
        compose.onNodeWithTag("root-content").performTouchInput {
            swipe(Offset(width * .25f, 5f), Offset(width * .75f, 5f), 350)
        }
        compose.onNodeWithText("Твій день").assertIsDisplayed()

        compose.onNodeWithTag("home-week-strip").performTouchInput { swipeLeft() }
        compose.waitUntil(5_000) { selectedDay() != before }
        assertNotEquals(before, selectedDay())
        compose.onNodeWithText("Твій день").assertIsDisplayed()
    }

    private fun subjectTab(label: String) = compose.onNode(hasText(com.kpyruy.takt.core.ui.i18n.t(label)) and
        SemanticsMatcher.expectValue(SemanticsProperties.Role, androidx.compose.ui.semantics.Role.Tab))

    private fun selectedDay(): String = compose.onNodeWithTag("home-date-number", useUnmergedTree = true)
        .fetchSemanticsNode().config[SemanticsProperties.Text].first().text

    private fun calendarTitle() = compose.onNode(hasText("Календар") and !hasClickAction())
}
