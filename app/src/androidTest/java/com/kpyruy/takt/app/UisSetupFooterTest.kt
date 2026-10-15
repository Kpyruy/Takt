package com.kpyruy.takt.app

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.platform.app.InstrumentationRegistry
import com.kpyruy.takt.core.data.UniversityAccountRepository
import com.kpyruy.takt.core.model.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Exercise the saved-account UIS branch without real credentials, device authentication or network. */
class UisSetupFooterTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun uisInputKeyboardDoesNotLeaveAnInsetOrWallpaperGapOnTheTourInvitation() {
        val context = compose.activity
        val sandbox = object : ContextWrapper(context) {
            override fun getApplicationContext(): Context = this
            override fun getSharedPreferences(name: String, mode: Int) =
                context.getSharedPreferences("tour_ui_fixture_$name", mode)
        }
        sandbox.getSharedPreferences("takt_university_account", 0).edit().clear()
            .putString("credentials", "noncredential-ui-fixture").commit()
        val account = UniversityAccountRepository(sandbox)
        assertTrue(account.hasAccount.value)
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                FirstRunScreen(AppSettings(language = AppLanguage.SLOVAK, themeMode = AppThemeMode.DARK,
                    themeFamily = ThemeFamily.BLUE), account,
                    authenticateDevice = { _, reply -> reply(DeviceAuthenticationResult.CANCELLED) },
                    onFinish = { _, _ -> }, onRestore = { null })
            }
        }
        try {
            compose.onNodeWithTag("welcome-connect").performScrollTo().performClick()
            compose.onNodeWithTag("onboarding-primary").performClick()
            compose.onNodeWithTag("uis-password").performScrollTo().performClick().performTextInput("example-only")
            compose.waitUntil(5_000) { imeVisible() }
            compose.onNodeWithTag("onboarding-primary").performSemanticsAction(SemanticsActions.OnClick) { it() }
            compose.waitUntil(5_000) { !imeVisible() }
            compose.onNodeWithText("Ukázať, čo je kde?").assertIsDisplayed()
            compose.onAllNodesWithTag("glass-wallpaper").assertCountEquals(1)
            val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
            val footer = compose.onNodeWithTag("onboarding-footer").fetchSemanticsNode().boundsInRoot
            assertEquals("Footer reaches the window bottom", root.bottom, footer.bottom, 1f)
            compose.onNodeWithText("Začnite bez prehliadky").assertIsDisplayed()
            compose.waitForIdle()
            android.os.SystemClock.sleep(100)
            val screenshot = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            compose.activity.openFileOutput("correction-sk-uis-footer.png", 0).use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
            screenshot.recycle()
        } finally { sandbox.getSharedPreferences("takt_university_account", 0).edit().clear().commit() }
    }

    private fun imeVisible() = ViewCompat.getRootWindowInsets(compose.activity.window.decorView)
        ?.isVisible(WindowInsetsCompat.Type.ime()) == true
}
