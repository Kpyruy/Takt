package com.kpyruy.takt.app

import android.view.KeyEvent
import android.view.MotionEvent
import android.os.SystemClock
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.kpyruy.takt.core.model.AppSettings
import com.kpyruy.takt.core.model.AppThemeMode
import com.kpyruy.takt.core.model.CardAppearance
import com.kpyruy.takt.core.ui.components.TaktChoiceMenu
import com.kpyruy.takt.core.ui.components.TaktChoiceMenuItem
import com.kpyruy.takt.core.ui.theme.TaktTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Root integration draft: copy into app/androidTest after reviewing ownership. Not run by worker. */
class TaktChoiceMenuUiTest {
    @get:Rule val compose = createComposeRule()
    private val expanded = mutableStateOf(false)
    private val selected = mutableIntStateOf(-1)
    private val dismissals = mutableIntStateOf(0)

    private fun fixture() {
        compose.setContent {
            TaktTheme(AppSettings(themeMode = AppThemeMode.DARK, cardAppearance = CardAppearance.GLASS)) {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.TopEnd) {
                    Box {
                        TextButton(onClick = { expanded.value = true }, modifier = Modifier.testTag("choice-trigger")) {
                            Text("Open")
                        }
                        TaktChoiceMenu(expanded.value, onDismissRequest = {
                            dismissals.intValue++
                            expanded.value = false
                        }) {
                            TaktChoiceMenuItem(text = { Text("Disabled") }, enabled = false,
                                modifier = Modifier.testTag("disabled-choice"), onClick = { selected.intValue = 100 })
                            repeat(40) { index ->
                                TaktChoiceMenuItem(text = { Text("Choice $index") },
                                    modifier = Modifier.testTag("choice-$index"), onClick = {
                                        selected.intValue = index
                                        expanded.value = false
                                    })
                            }
                        }
                    }
                }
            }
        }
        compose.onNodeWithTag("choice-trigger").performClick()
        compose.onNodeWithTag("takt-choice-menu").assertIsDisplayed()
    }

    @Test fun longMenuScrollsToLastChoiceAndPreservesDisabledAndCallbackSemantics() {
        fixture()
        compose.onNodeWithTag("disabled-choice").assertIsNotEnabled().assertHeightIsAtLeast(48.dp)
            .performTouchInput { click() }
        compose.runOnIdle { assertEquals(-1, selected.intValue) }
        compose.onNodeWithTag("choice-0").assertHeightIsAtLeast(48.dp)
        compose.onNodeWithTag("choice-39").performScrollTo().performClick()
        compose.onNodeWithTag("takt-choice-menu").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(39, selected.intValue)
            assertEquals(0, dismissals.intValue)
        }
    }

    @Test fun nativeBackDismissesWithoutInvokingAnActionAndTriggerCanReopen() {
        fixture()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitUntil(5_000) { dismissals.intValue == 1 }
        compose.waitUntil(5_000) { dismissals.intValue == 1 }
        compose.onNodeWithTag("takt-choice-menu").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(-1, selected.intValue)
            assertEquals(1, dismissals.intValue)
        }
        compose.onNodeWithTag("choice-trigger").performClick()
        compose.onNodeWithTag("takt-choice-menu").assertIsDisplayed()
    }

    @Test fun nativeOutsideTouchDismissesTheFocusablePopup() {
        fixture()
        // The shared popup always leaves an 8dp window margin; tap inside that margin.
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val now = SystemClock.uptimeMillis()
        val metrics = instrumentation.targetContext.resources.displayMetrics
        val x = metrics.widthPixels - 2f
        val y = metrics.heightPixels / 2f
        val down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x, y, 0)
        val up = MotionEvent.obtain(now, now + 20, MotionEvent.ACTION_UP, x, y, 0)
        try {
            instrumentation.uiAutomation.injectInputEvent(down, true)
            instrumentation.uiAutomation.injectInputEvent(up, true)
        } finally {
            down.recycle(); up.recycle()
        }
        compose.waitUntil(5_000) { dismissals.intValue == 1 }
        compose.onNodeWithTag("takt-choice-menu").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(-1, selected.intValue)
            assertEquals(1, dismissals.intValue)
        }
    }
}
