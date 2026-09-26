package com.kpyruy.takt.app

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.kpyruy.takt.core.model.AppSettings
import com.kpyruy.takt.core.model.ThemeFamily
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

class FirstRunScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun themePreviewAndOptionalTourLeadToOwnSetup() {
        var chosen: AppSettings? = null
        compose.setContent {
            FirstRunScreen(AppSettings(), onFinish = { chosen = it }, onRestore = { null })
        }
        compose.onNodeWithText("Твоя перша пара").assertExists()
        compose.onNodeWithTag("onboarding-color-PURPLE").performScrollTo().performClick()
        compose.onNodeWithTag("onboarding-primary").performClick()
        compose.onNodeWithText("Показати, що де?").assertExists()
        compose.onNodeWithTag("onboarding-primary").performClick()
        listOf("Сьогодні", "Календар", "Предмети", "Прогрес").forEach { title ->
            compose.onNodeWithText(title).assertExists()
            compose.onNodeWithTag("onboarding-primary").performClick()
        }
        compose.runOnIdle {
            assertNotNull(chosen)
            assertEquals(ThemeFamily.PURPLE, chosen?.themeFamily)
        }
    }
}
