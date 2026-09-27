package com.kpyruy.takt.app

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import org.junit.Assert.assertTrue
import androidx.test.platform.app.InstrumentationRegistry
import com.kpyruy.takt.core.data.UniversityAccountRepository
import com.kpyruy.takt.core.model.DeviceAuthenticationResult
import com.kpyruy.takt.core.model.AppLanguage
import com.kpyruy.takt.core.ui.i18n.TaktI18n
import com.kpyruy.takt.feature.settings.UniversityAccountPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class UisSignInFormTest {
    @get:Rule val compose = createComposeRule()

    @Test fun passwordEyeAndSingleSignInRespectCancelledAuthentication() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = UniversityAccountRepository(context)
        assertFalse(repository.hasAccount.value)
        var confirmations = 0
        TaktI18n.use(AppLanguage.UKRAINIAN)
        compose.setContent {
            UniversityAccountPage(repository, authenticate = { _, callback ->
                confirmations++
                callback(DeviceAuthenticationResult.CANCELLED)
            }, onBack = {})
        }
        compose.onNodeWithText("Підставити збережені дані").assertDoesNotExist()
        compose.onNodeWithText("Оновити дані").assertDoesNotExist()
        compose.onNodeWithText("Зберегти дані").assertDoesNotExist()
        compose.onNodeWithTag("uis-sign-in").assertIsNotEnabled()
        compose.onNodeWithTag("uis-login").performTextInput("current-account")
        compose.onNodeWithTag("uis-password").performTextInput("test-password")
        assertRenderedPassword("•".repeat(13))
        compose.onNodeWithTag("uis-password-visibility").performClick()
        assertRenderedPassword("test-password")
        compose.onNodeWithTag("uis-password").assertTextContains("test-password")
        compose.onNodeWithTag("uis-sign-in").performScrollTo().performClick()
        assertRenderedPassword("•".repeat(13))
        compose.onNodeWithText("Підтвердження скасовано").assertExists()
        compose.runOnIdle {
            assertEquals(1, confirmations)
            assertFalse(repository.hasAccount.value)
        }
    }
    private fun assertRenderedPassword(expected: String) {
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithTag("uis-password").performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue("Rendered password must match the selected visibility", layouts.any { it.layoutInput.text.text == expected })
    }
}
