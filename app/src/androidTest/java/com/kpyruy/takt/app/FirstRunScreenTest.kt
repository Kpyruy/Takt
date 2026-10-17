package com.kpyruy.takt.app

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.kpyruy.takt.core.model.AppSettings
import com.kpyruy.takt.core.model.AppLanguage
import com.kpyruy.takt.core.model.ThemeFamily
import com.kpyruy.takt.core.data.UniversityAccountRepository
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

class FirstRunScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun themePreviewAndOptionalTourLeadToOwnSetup() {
        var chosen: AppSettings? = null
        var startTour = false
        compose.setContent {
            FirstRunScreen(
                settings = AppSettings(language = AppLanguage.UKRAINIAN),
                universityAccountRepository = UniversityAccountRepository(InstrumentationRegistry.getInstrumentation().targetContext),
                authenticateDevice = { _, callback -> callback(com.kpyruy.takt.core.model.DeviceAuthenticationResult.CANCELLED) },
                onFinish = { settings, tour -> chosen = settings; startTour = tour },
                onRestore = { null },
            )
        }
        compose.onNodeWithText("Почати локально").performClick()
        compose.onNodeWithText("Твій Takt").assertExists()
        compose.onNodeWithTag("onboarding-color-open").performScrollTo().performClick()
        compose.onNodeWithTag("onboarding-color-PURPLE").performScrollTo().performClick()
        compose.onNodeWithTag("palette-popup-apply").performClick()
        compose.onNodeWithTag("onboarding-primary").performClick()
        compose.onNodeWithText("Як користуватися Takt?").assertExists()
        compose.onNodeWithTag("onboarding-uis").performClick()
        compose.onNodeWithTag("onboarding-primary").assertIsNotEnabled()
        compose.onNodeWithTag("onboarding-local").performClick()
        compose.onNodeWithTag("onboarding-primary").performClick()
        compose.onNodeWithText("Показати, що де?").assertExists()
        compose.onNodeWithText("Перед стартом розкладу").assertExists()
        compose.onNodeWithTag("onboarding-primary").performClick()
        compose.runOnIdle {
            org.junit.Assert.assertTrue(startTour)
            assertNotNull(chosen)
            assertEquals(ThemeFamily.PURPLE, chosen?.themeFamily)
        }
    }
}
