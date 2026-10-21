package com.kpyruy.takt.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.kpyruy.takt.core.model.AppLanguage
import com.kpyruy.takt.core.ui.i18n.t
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TourScenePairingTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun subjectExplanationTravelsWithSubjectsDuringCalendarTransition() {
        runBlocking { (compose.activity.application as TaktApplication).dataContainer.settingsRepository.setLanguage(AppLanguage.SLOVAK) }
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("root-navigation").fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithContentDescription(t("Settings")).onFirst().performClick()
        compose.onNodeWithText(t("Tour of Takt")).performScrollTo().performClick()
        repeat(6) {
            compose.waitUntil(8_000) { compose.onAllNodesWithTag("tour-next").fetchSemanticsNodes().any { it.config.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled).not() } }
            compose.onNodeWithTag("tour-next").performClick()
        }
        compose.onNodeWithTag("tour-card-CALENDAR_LAYOUT").assertIsDisplayed()
        compose.waitUntil(8_000) { compose.onAllNodesWithTag("tour-next").fetchSemanticsNodes().any { !it.config.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled) } }
        compose.mainClock.autoAdvance = false
        try {
            compose.onNodeWithTag("tour-next").performClick()
            compose.mainClock.advanceTimeBy(96)
            val scene = compose.onNodeWithTag("subjects-screen", useUnmergedTree = true).fetchSemanticsNode()
            val card = compose.onNodeWithTag("tour-card-SUBJECTS").fetchSemanticsNode()
            val inset = 12 * compose.activity.resources.displayMetrics.density
            assertEquals("Explanation must share the incoming subject scene's position",
                inset, card.positionInRoot.x - scene.positionInRoot.x, 2f)
            compose.mainClock.advanceTimeBy(700)
        } finally { compose.mainClock.autoAdvance = true }
    }
}
