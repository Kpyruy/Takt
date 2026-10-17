package com.kpyruy.takt.app

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
import android.net.Uri
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.platform.app.InstrumentationRegistry
import com.kpyruy.takt.core.data.SharedPreferencesAppSettingsRepository
import com.kpyruy.takt.core.model.*
import com.kpyruy.takt.core.ui.components.TaktGlassBackground
import com.kpyruy.takt.core.ui.i18n.TaktI18n
import com.kpyruy.takt.core.ui.theme.TaktTheme
import com.kpyruy.takt.feature.settings.SettingsScreen
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class CustomWallpaperUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val data get() = (compose.activity.application as TaktApplication).dataContainer
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    @Test fun pickerPreviewsPhotoThenApplyPersistsAndResetRestoresTheBuiltInBackground() {
        runBlocking {
            data.seedLiquidGlassReview()
            data.settingsRepository.setLanguage(AppLanguage.SLOVAK)
            data.settingsRepository.setAppearance(data.settingsRepository.settings.value.copy(customWallpaper = null,
                cardAppearance = CardAppearance.GLASS, themeMode = AppThemeMode.DARK, themeFamily = ThemeFamily.BLUE))
        }
        val source = File(compose.activity.cacheDir, "review-wallpaper.png")
        val photo = Bitmap.createBitmap(1200, 2400, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(photo)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, 1000f, 2400f, intArrayOf(0xFFDA9363.toInt(), 0xFF716B86.toInt(), 0xFF143C4E.toInt()), null, Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, 1200f, 2400f, paint); paint.shader = null
        paint.color = 0xFFFFDCAE.toInt(); canvas.drawCircle(850f, 650f, 170f, paint)
        for ((index, color) in listOf(0xFF4B6577.toInt(), 0xFF254A60.toInt(), 0xFF133747.toInt()).withIndex()) {
            paint.color = color
            val y = 1150f + index * 330f
            val path = Path().apply { moveTo(0f, y + 150f); lineTo(330f, y - 200f); lineTo(650f, y + 80f)
                lineTo(990f, y - 130f); lineTo(1200f, y + 50f); lineTo(1200f, 2400f); lineTo(0f, 2400f); close() }
            canvas.drawPath(path, paint)
        }
        source.outputStream().use { photo.compress(Bitmap.CompressFormat.PNG, 100, it) }; photo.recycle()
        var imported: String? = null
        var entry = 0
        fun appearance() {
            val appearanceEntry = ++entry
            compose.activityRule.scenario.onActivity { activity -> activity.setContent {
                val settings by data.settingsRepository.settings.collectAsStateWithLifecycle()
                TaktI18n.use(settings.language)
                key(appearanceEntry) { TaktTheme(settings) { TaktGlassBackground {
                    SettingsScreen(data.settingsRepository, data.backupRepository, data.documentStore, data.studyPlanRepository,
                        data.universityAccountRepository, { _, reply -> reply(DeviceAuthenticationResult.CANCELLED) },
                        (compose.activity.application as TaktApplication).appReleaseRepository, {}, guidedPage = "appearance")
                } } }
            } }
        }
        try {
            appearance()
            compose.onNodeWithTag("appearance-wallpaper-choose").assertDoesNotExist()
            compose.onNodeWithTag("appearance-wallpaper-open").performScrollTo().assertIsDisplayed()
            capture("appearance-background-entry.png")
            compose.onNodeWithTag("appearance-wallpaper-open").performClick()
            compose.onNodeWithTag("appearance-wallpaper-menu").assertIsDisplayed()
            compose.onNodeWithTag("appearance-wallpaper-choose").performScrollTo().assertIsDisplayed()
            val filter = IntentFilter(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE); addDataType("*/*")
            }
            val monitor = instrumentation.addMonitor(filter,
                Instrumentation.ActivityResult(Activity.RESULT_OK, Intent().setData(Uri.fromFile(source))), true)
            try { compose.onNodeWithTag("appearance-wallpaper-choose").performClick()
                compose.waitUntil(8_000) { compose.onAllNodesWithTag("appearance-wallpaper-reset").fetchSemanticsNodes().isNotEmpty() }
                assertEquals(1, monitor.hits)
            } finally { instrumentation.removeMonitor(monitor) }
            assertNull("Preview must not apply the draft", data.settingsRepository.settings.value.customWallpaper)
            capture("wallpaper-menu-preview.png")
            compose.onNodeWithTag("appearance-wallpaper-done").performClick()
            compose.onNodeWithTag("appearance-wallpaper-menu").assertDoesNotExist()
            compose.onNodeWithTag("appearance-wallpaper-choose").assertDoesNotExist()
            compose.onNodeWithText("Použiť zobrazenie").performClick()
            compose.waitUntil(5_000) { data.settingsRepository.settings.value.customWallpaper != null }
            imported = data.settingsRepository.settings.value.customWallpaper
            assertEquals(imported, SharedPreferencesAppSettingsRepository(compose.activity).settings.value.customWallpaper)
            source.delete()
            capture("wallpaper-applied.png")
            // A new settings entry must reconstruct the applied photo and offer the built-in reset.
            appearance()
            compose.onNodeWithTag("appearance-wallpaper-open").performScrollTo().performClick()
            compose.onNodeWithTag("appearance-wallpaper-reset").performScrollTo().performClick()
            compose.onNodeWithTag("appearance-wallpaper-reset").assertDoesNotExist()
            compose.onNodeWithTag("appearance-wallpaper-done").performClick()
            compose.onNodeWithTag("appearance-wallpaper-menu").assertDoesNotExist()
            compose.onNodeWithTag("appearance-wallpaper-choose").assertDoesNotExist()
            compose.onNodeWithText("Použiť zobrazenie").performClick()
            compose.waitUntil(5_000) { data.settingsRepository.settings.value.customWallpaper == null }
            assertNull(SharedPreferencesAppSettingsRepository(compose.activity).settings.value.customWallpaper)
        } finally {
            runBlocking { data.settingsRepository.setAppearance(data.settingsRepository.settings.value.copy(customWallpaper = null)) }
            imported?.let { File(File(compose.activity.filesDir, "wallpapers"), it).delete() }
            source.delete()
        }
    }

    private fun capture(name: String) {
        compose.waitForIdle(); android.os.SystemClock.sleep(350)
        val image = instrumentation.uiAutomation.takeScreenshot()
        compose.activity.openFileOutput(name, 0).use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        image.recycle()
    }
}
