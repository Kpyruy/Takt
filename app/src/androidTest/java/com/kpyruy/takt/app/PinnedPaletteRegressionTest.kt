package com.kpyruy.takt.app

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import com.kpyruy.takt.core.model.*
import com.kpyruy.takt.core.ui.theme.TaktTheme
import com.kpyruy.takt.feature.settings.AppearanceThemeChoices
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Pure UI fixture: no repositories, stored settings, or destructive setup. Run by root. */
class PinnedPaletteRegressionTest {
    @get:Rule val compose = createComposeRule()
    private val original = AppSettings(themeMode = AppThemeMode.DARK,
        customThemes = List(16) { CustomColorTheme("fixture-$it", "Theme $it", "#3869BA", "#387F69", "#9963AB") })
    private val settings = mutableStateOf(original)
    private var applies = 0
    private var keyboard: SoftwareKeyboardController? = null

    private fun open() {
        compose.setContent {
            TaktTheme(settings.value) {
                keyboard = LocalSoftwareKeyboardController.current
                AppearanceThemeChoices(settings.value, { applies++; settings.value = it })
            }
        }
        compose.onNodeWithTag("appearance-color-open").performClick()
    }

    @Test fun scrollingPresetsAndCustomThemesKeepsPreviewPinnedAndCancelDiscardsSelection() {
        open()
        val before = compose.onNodeWithTag("palette-live-preview").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithTag("appearance-color-custom-fixture-15").performScrollTo().performClick()
        compose.onNodeWithTag("palette-live-preview").assertIsDisplayed()
        assertEquals(before, compose.onNodeWithTag("palette-live-preview").fetchSemanticsNode().boundsInRoot)
        compose.onNodeWithTag("palette-popup-cancel").performClick()
        compose.runOnIdle { assertEquals(0, applies); assertEquals(original, settings.value) }
    }

    @Test fun editorScrollWithKeyboardKeepsThreeRolesVisibleAndSaveRemainsTransactional() {
        open()
        compose.onNodeWithTag("theme-edit-fixture-15").performScrollTo().performClick()
        compose.onNodeWithTag("theme-name").performScrollTo().performTextReplacement("Changed draft")
        compose.onNodeWithTag("palette-live-preview").assertIsDisplayed()
        val before = compose.onNodeWithTag("palette-live-preview").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithTag("theme-hex").performScrollTo().assertIsDisplayed()
        assertEquals(before, compose.onNodeWithTag("palette-live-preview").fetchSemanticsNode().boundsInRoot)
        listOf("primary", "secondary", "tertiary").forEach {
            compose.onNodeWithTag("palette-preview-$it").assertIsDisplayed()
        }
        compose.onNodeWithTag("theme-hex").performTextReplacement("#007E91")
        compose.runOnIdle { keyboard?.hide() }
        compose.waitForIdle()
        compose.onNodeWithTag("theme-save").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(0, applies); assertEquals(original, settings.value) }
        compose.onNodeWithTag("palette-popup-apply").performClick()
        compose.runOnIdle {
            assertEquals(1, applies)
            val saved = settings.value.selectedCustomTheme!!
            assertEquals("Changed draft", saved.name)
            assertEquals("#007E91", saved.primaryHex)
            assertEquals("#387F69", saved.secondaryHex)
            assertEquals("#9963AB", saved.tertiaryHex)
        }
    }
}
