package com.kpyruy.takt.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class GlobalAddSheetTest {
    @get:Rule val compose = createComposeRule()

    @Test fun allAddActionsAreVisibleAndTestOpensItsForm() {
        var selected: CreateItemType? = null
        compose.setContent {
            MaterialTheme {
                GlobalAddSheet(onDismiss = {}, onCreate = { selected = it }, onQuickAdd = {})
            }
        }

        if (androidx.test.platform.app.InstrumentationRegistry.getArguments().getString("glassCapture") == "true") {
            compose.waitForIdle()
            val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
            android.os.SystemClock.sleep(350)
            val image = instrumentation.uiAutomation.takeScreenshot()
            instrumentation.targetContext.openFileOutput("glass-add-menu.png", 0).use {
                image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
            image.recycle()
        }
        compose.onNodeWithText("Предмет").assertIsDisplayed()
        compose.onNodeWithText("Тест").assertIsDisplayed().performClick()
        compose.onNodeWithText("Нагадування").assertIsDisplayed()
        compose.runOnIdle { assertEquals(CreateItemType.TEST, selected) }
    }
}
