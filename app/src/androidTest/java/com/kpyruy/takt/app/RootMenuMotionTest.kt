package com.kpyruy.takt.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class RootMenuMotionTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun horizontalSwipeSlidesBothScreensAndReverses() {
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Твій день").fetchSemanticsNodes().isNotEmpty() }
        val originalHomeX = compose.onNodeWithText("Твій день").fetchSemanticsNode().positionInRoot.x
        compose.mainClock.autoAdvance = false
        try {
            swipe(true)
            compose.mainClock.advanceTimeBy(96)
            val outgoingX = compose.onNodeWithText("Твій день").fetchSemanticsNode().positionInRoot.x
            val incomingX = calendarTitle().fetchSemanticsNode().positionInRoot.x
            assertTrue("Home moves left", outgoingX < originalHomeX)
            assertTrue("Calendar enters from right", incomingX > originalHomeX)
            compose.mainClock.advanceTimeBy(600)
            val originalCalendarX = calendarTitle().fetchSemanticsNode().positionInRoot.x
            swipe(false)
            compose.mainClock.advanceTimeBy(96)
            assertTrue("Calendar moves right", calendarTitle().fetchSemanticsNode().positionInRoot.x > originalCalendarX)
            assertTrue("Home enters from left", compose.onNodeWithText("Твій день").fetchSemanticsNode().positionInRoot.x < originalHomeX)
            compose.mainClock.advanceTimeBy(600)
        } finally { compose.mainClock.autoAdvance = true }
        compose.onNodeWithText("Твій день").assertIsDisplayed()
    }
    private fun calendarTitle() = compose.onNode(hasText("Календар") and !hasClickAction())
    private fun swipe(right: Boolean) {
        compose.onNodeWithTag("root-content").performTouchInput {
            swipe(Offset(width * if(right) .25f else .75f, 5f), Offset(width * if(right) .75f else .25f, 5f), 350)
        }
    }
}
