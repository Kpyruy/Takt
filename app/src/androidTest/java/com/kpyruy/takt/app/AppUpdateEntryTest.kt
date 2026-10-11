package com.kpyruy.takt.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.kpyruy.takt.core.data.*
import com.kpyruy.takt.core.model.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AppUpdateEntryTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun entryChecksOnceAndDismissedVersionDoesNotPromptOnRecreation() {
        val app=compose.activity.application as TaktApplication
        kotlinx.coroutines.runBlocking { app.dataContainer.settingsRepository.setLanguage(AppLanguage.UKRAINIAN) }
        val field=TaktApplication::class.java.getDeclaredField("appReleaseRepository").apply { isAccessible=true }
        val original=app.appReleaseRepository
        val store=object:AppReleasePreferenceStore {
            var data=AppReleasePreferences()
            override fun read()=data
            override fun write(value:AppReleasePreferences){data=value}
        }
        var calls=0
        val checker=AppReleaseRepository("0.2.0",store, {calls++;AppRelease("v0.3.0",
            "https://github.com/Kpyruy/Takt/releases/tag/v0.3.0","https://github.com/Kpyruy/Takt/releases/download/v0.3.0/Takt.apk")})
        try {
            field.set(app,checker)
            compose.activityRule.scenario.recreate()
            compose.waitUntil(10_000) { checker.state.value.availableUpdate!=null }
            compose.onNodeWithText("Доступне оновлення Takt").assertExists()
            compose.onNodeWithText("Пізніше").performClick()
            compose.waitUntil(5_000) { checker.state.value.promptUpdate==null }
            compose.activityRule.scenario.recreate()
            compose.waitForIdle()
            compose.onNodeWithText("Доступне оновлення Takt").assertDoesNotExist()
            assertEquals(1,calls)
            assertEquals("v0.3.0",store.data.dismissedVersion)
        } finally { field.set(app,original) }
    }
}
