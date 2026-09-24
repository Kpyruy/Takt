package com.kpyruy.takt.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RootMenuMotionTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

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

    private fun selectedDay(): String = compose.onNodeWithTag("home-date-number")
        .fetchSemanticsNode().config[SemanticsProperties.Text].first().text

    private fun calendarTitle() = compose.onNode(hasText("Календар") and !hasClickAction())
}
