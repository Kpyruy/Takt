package com.kpyruy.takt.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CalendarViewMotionTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun dayAndMonthMoveTogetherRatherThanTeleporting() {
        compose.onAllNodes(hasText("Календар") and hasClickAction()).onLast().performClick()
        compose.onNodeWithTag("calendar-week-strip").assertIsDisplayed()
        val dayX = compose.onNodeWithTag("calendar-week-strip").fetchSemanticsNode().positionInRoot.x
        compose.mainClock.autoAdvance = false
        try {
            compose.onNodeWithTag("calendar-view-month").performClick()
            compose.mainClock.advanceTimeBy(96)
            assertTrue("Day slides left while month arrives", compose.onNodeWithTag("calendar-week-strip")
                .fetchSemanticsNode().positionInRoot.x < dayX - 1f)
            compose.mainClock.advanceTimeBy(500)
            compose.onNodeWithTag("calendar-week-strip").assertDoesNotExist()
            android.os.SystemClock.sleep(130)
            compose.onNodeWithTag("calendar-view-day").performClick()
            compose.mainClock.advanceTimeBy(96)
            assertTrue("Day arrives from the left", compose.onNodeWithTag("calendar-week-strip")
                .fetchSemanticsNode().positionInRoot.x < dayX - 1f)
            compose.mainClock.advanceTimeBy(500)
        } finally { compose.mainClock.autoAdvance = true }
        compose.onNodeWithTag("calendar-week-strip").assertIsDisplayed()
    }
}
