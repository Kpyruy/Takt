package com.kpyruy.takt.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.kpyruy.takt.core.ui.i18n.t
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LiquidGlassInsetsTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun navigationAndAddRemainAboveSystemControls() {
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("root-add").fetchSemanticsNodes().isNotEmpty() }
        val density = compose.activity.resources.displayMetrics.density
        val view = compose.activity.window.decorView
        val bottom = ViewCompat.getRootWindowInsets(view)?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0
        val safeBottom = (view.height - bottom) / density
        listOf("Сьогодні", "Календар", "Предмети", "Прогрес").forEach { source ->
            val node = compose.onAllNodes(hasText(t(source)) and hasClickAction()).onLast()
            assertTrue("$source overlaps system controls", node.getUnclippedBoundsInRoot().bottom.value <= safeBottom + 1f)
        }
        assertTrue("Add overlaps system controls", compose.onNodeWithTag("root-add").getUnclippedBoundsInRoot().bottom.value <= safeBottom + 1f)
    }

    @Test fun saveActionCanBeReachedAboveKeyboard() {
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("root-add").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("root-add").performClick()
        compose.onNodeWithText(t("Subject")).performClick()
        compose.onNodeWithTag("course-title").performClick().performTextInput("Keyboard review")
        compose.onNodeWithTag("save-course").performScrollTo().assertIsDisplayed()
        val view = compose.activity.window.decorView
        val density = compose.activity.resources.displayMetrics.density
        val ime = ViewCompat.getRootWindowInsets(view)?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
        assertTrue("Expected visible software keyboard", ime > 0)
        assertTrue("Save overlaps keyboard", compose.onNodeWithTag("save-course").getUnclippedBoundsInRoot().bottom.value <= (view.height - ime) / density + 1f)
    }
}
