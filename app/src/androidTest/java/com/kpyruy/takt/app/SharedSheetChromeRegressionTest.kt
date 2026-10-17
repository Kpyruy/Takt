package com.kpyruy.takt.app

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.kpyruy.takt.core.model.AppLanguage
import com.kpyruy.takt.core.model.AppSettings
import com.kpyruy.takt.core.model.AppThemeMode
import com.kpyruy.takt.core.ui.components.TaktFullSheet
import com.kpyruy.takt.core.ui.i18n.TaktI18n
import com.kpyruy.takt.core.ui.theme.TaktTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Run on the review emulator; screenshot pixels stay in memory. No app data changes. */
class SharedSheetChromeRegressionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun darkSheetDimsBackdrop() = assertScrimDims(AppThemeMode.DARK)
    @Test fun lightSheetDimsBackdrop() = assertScrimDims(AppThemeMode.LIGHT)

    private fun assertScrimDims(mode: AppThemeMode) {
        val open = mutableStateOf(false)
        compose.setContent {
            TaktTheme(AppSettings(themeMode = mode)) {
                Box(Modifier.fillMaxSize().background(Color(0xFF808080)))
                if (open.value) TaktFullSheet(onDismissRequest = { open.value = false }) {
                    Spacer(Modifier.height(120.dp))
                }
            }
        }
        fun sample(): Int {
            compose.waitForIdle()
            val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            return try { AndroidColor.red(bitmap.getPixel(bitmap.width / 2, bitmap.height / 4)) }
            finally { bitmap.recycle() }
        }
        val before = sample()
        compose.runOnIdle { open.value = true }
        var after = before
        // The Android window scrim animates outside Compose's idling clock. Observe its pixels.
        compose.waitUntil(5_000) {
            after = sample()
            after < before * .8f
        }
        assertTrue("Backdrop must darken in $mode: $before -> $after", after < before * .8f)
    }

    @Test fun slovakAddTilesHaveLocalizedUnclippedCopyAndKeepWholeTileActions() {
        val previous = TaktI18n.language
        var selected: CreateItemType? = null
        try {
            TaktI18n.use(AppLanguage.SLOVAK)
            compose.setContent {
                TaktTheme(AppSettings(themeMode = AppThemeMode.DARK)) {
                    GlobalAddSheet(onDismiss = {}, onCreate = { selected = it }, onQuickAdd = {})
                }
            }
            listOf(CreateItemType.NOTE, CreateItemType.EVENT, CreateItemType.REMINDER).forEach { type ->
                val tile = compose.onNodeWithTag("global-add-${type.name}").performScrollTo()
                tile.assertHasClickAction().assertHeightIsAtLeast(48.dp)
                val copy = tile.fetchSemanticsNode().config[SemanticsProperties.Text].joinToString { it.text }
                // Requires root translations for Додати подію and Додати нагадування.
                assertFalse("Untranslated tile: $copy", Regex("[А-Яа-яІіЇїЄєҐґ]").containsMatchIn(copy))
                compose.onAllNodes(hasAnyAncestor(hasTestTag("global-add-${type.name}")) and hasText(type.label),
                    useUnmergedTree = true).onFirst().performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
                    val layouts = mutableListOf<TextLayoutResult>()
                    action(layouts)
                    assertTrue(layouts.isNotEmpty())
                    layouts.forEach { assertFalse(it.hasVisualOverflow) }
                }
                tile.performClick()
                compose.runOnIdle { assertEquals(type, selected) }
            }
        } finally { TaktI18n.use(previous) }
    }
}
