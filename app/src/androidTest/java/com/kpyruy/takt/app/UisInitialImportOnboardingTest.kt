package com.kpyruy.takt.app

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.kpyruy.takt.core.data.UniversityAccountRepository
import com.kpyruy.takt.core.model.AppLanguage
import com.kpyruy.takt.core.model.AppSettings
import com.kpyruy.takt.core.model.DeviceAuthenticationResult
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class UisInitialImportOnboardingTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun savedCredentialsDoNotAllowLeavingUisSetupBeforeItsFirstImportCompletes() {
        val app = compose.activity
        val prefix = "initial_import_ui_${System.nanoTime()}"
        val context = object : ContextWrapper(app) {
            override fun getApplicationContext(): Context = this
            override fun getSharedPreferences(name: String, mode: Int) = app.getSharedPreferences("$prefix-$name", mode)
        }
        val preferences = context.getSharedPreferences("takt_university_account", 0)
        preferences.edit().putString("credentials", "noncredential-ui-fixture").commit()
        val account = UniversityAccountRepository(context)
        assertTrue(account.hasAccount.value)
        assertFalse(account.initialImportComplete.value)
        try {
            compose.activityRule.scenario.onActivity { activity ->
                activity.setContent {
                    FirstRunScreen(AppSettings(language = AppLanguage.ENGLISH), account,
                        authenticateDevice = { _, reply -> reply(DeviceAuthenticationResult.CANCELLED) },
                        onFinish = { _, _ -> fail("Setup finished before the UIS import") }, onRestore = { null })
                }
            }
            compose.onNodeWithTag("welcome-uis").performScrollTo().performClick()
            compose.onNodeWithTag("onboarding-primary").performClick()
            compose.onNodeWithTag("uis-login").assertExists()
            compose.onNodeWithTag("onboarding-primary").assertIsNotEnabled()
        } finally {
            preferences.edit().clear().commit()
        }
    }
}
