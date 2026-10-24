package com.kpyruy.takt.app

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.kpyruy.takt.core.ui.components.*
import com.kpyruy.takt.core.ui.theme.TaktTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.Before

class TaktNavigationSelectionTest {
    @get:Rule val compose = createComposeRule()
    @Before fun useEnglish() {
        com.kpyruy.takt.core.ui.i18n.TaktI18n.use(com.kpyruy.takt.core.model.AppLanguage.ENGLISH)
    }
    @Test fun floatingAddAnimatesIntoAndOutOfAddingMode() {
        var selected by mutableStateOf(false)
        compose.setContent { TaktTheme { TaktFloatingAddFab(selected) { selected = true } } }
        fun glyphWidth() = compose.onNodeWithContentDescription("Add", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.width
        compose.mainClock.autoAdvance = false
        try {
            val idle = glyphWidth()
            compose.onNodeWithTag("root-add").performClick()
            compose.mainClock.advanceTimeBy(64)
            val entering = glyphWidth()
            assertTrue("The glyph moves during entry", entering > idle + 1f)
            compose.mainClock.advanceTimeBy(400)
            val active = glyphWidth()
            assertTrue("Entry settles at the active angle", active > entering)
            compose.onNodeWithTag("root-add").assertIsSelected()
            captureStudiesScreenshot("navigation-floating-add-active.png")
            compose.runOnIdle { selected = false }
            compose.mainClock.advanceTimeBy(64)
            val leaving = glyphWidth()
            assertTrue("The glyph animates back", leaving < active && leaving > idle)
            compose.mainClock.advanceTimeBy(400)
            assertEquals(idle, glyphWidth(), 1f)
            compose.onNodeWithTag("root-add").assertIsNotSelected()
        } finally { compose.mainClock.autoAdvance = true }
    }
    @Test fun sharedLensMovesToAddAndReturnsToTheChosenTab() {
        compose.setContent {
            var selected by remember { mutableIntStateOf(0) }
            var addSelected by remember { mutableStateOf(false) }
            val labels = listOf("Today", "Calendar", "Subjects", "Studies")
            val icons = listOf(TaktIcons.Home, TaktIcons.Calendar, TaktIcons.Book, TaktIcons.Progress)
            TaktTheme { TaktGlassBackground {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    TaktBottomNavigation(labels.mapIndexed { index, label ->
                        TaktNavItem(label, icons[index], index == selected) { selected = index; addSelected = false }
                    }, centerSelected = addSelected, centerContent = {
                        TaktAddFab(selected = addSelected) { addSelected = true }
                    })
                }
            } }
        }
        compose.onNodeWithTag("root-add").assertIsNotSelected()
        captureStudiesScreenshot("navigation-add-inactive.png")
        compose.onNodeWithTag("root-add").performClick()
        compose.onNodeWithTag("root-add").assertIsSelected()
        compose.onNode(hasText("Today") and hasClickAction()).assertIsNotSelected()
        compose.mainClock.advanceTimeBy(400)
        val lens = compose.onNodeWithTag("navigation-lens").fetchSemanticsNode().boundsInRoot
        val add = compose.onNodeWithTag("root-add").fetchSemanticsNode().boundsInRoot
        assertEquals("Add lens horizontal center", add.center.x, lens.center.x, 1f)
        assertEquals("Add lens vertical center", add.center.y, lens.center.y, 1f)
        assertEquals(lens.width, lens.height, 1f)
        captureStudiesScreenshot("navigation-add-active.png")
        compose.onNode(hasText("Subjects") and hasClickAction()).performClick()
        compose.onNodeWithTag("root-add").assertIsNotSelected()
        compose.mainClock.advanceTimeBy(400)
        val subject = compose.onNode(hasText("Subjects") and hasClickAction()).assertIsSelected().fetchSemanticsNode().boundsInRoot
        val returned = compose.onNodeWithTag("navigation-lens").fetchSemanticsNode().boundsInRoot
        assertEquals(subject.center.x, returned.center.x, 1f)
        captureStudiesScreenshot("navigation-add-reset.png")
    }
    @Test fun floatingAddSitsAboveFourTabsAndDoesNotMoveTheirSelection() {
        compose.setContent {
            var selected by remember { mutableIntStateOf(0) }
            var add by remember { mutableStateOf(false) }
            val labels = listOf("Today", "Calendar", "Subjects", "Studies")
            val icons = listOf(TaktIcons.Home, TaktIcons.Calendar, TaktIcons.Book, TaktIcons.Progress)
            TaktTheme { TaktGlassBackground {
                androidx.compose.material3.Scaffold(containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    floatingActionButton = { TaktFloatingAddFab(add) { add = true } },
                    bottomBar = { TaktBottomNavigation(labels.mapIndexed { index, label ->
                        TaktNavItem(label, icons[index], selected == index) { selected = index; add = false }
                    }) }) { Box(Modifier.padding(it)) }
            } }
        }
        val add = compose.onNodeWithTag("root-add").fetchSemanticsNode().boundsInRoot
        val rail = compose.onNodeWithTag("root-navigation").fetchSemanticsNode().boundsInRoot
        assertTrue(add.bottom < rail.top)
        compose.onNodeWithTag("root-add").performClick().assertIsSelected()
        compose.onNode(hasText("Today") and hasClickAction()).assertIsSelected()
        compose.onNode(hasText("Studies") and hasClickAction()).performClick().assertIsSelected()
        compose.onNodeWithTag("root-add").assertIsNotSelected()
        captureStudiesScreenshot("navigation-floating-add.png")
    }

}
