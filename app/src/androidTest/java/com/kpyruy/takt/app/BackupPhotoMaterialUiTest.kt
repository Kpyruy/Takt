package com.kpyruy.takt.app

import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.kpyruy.takt.core.data.*
import com.kpyruy.takt.core.model.*
import com.kpyruy.takt.core.ui.components.TaktGlassBackground
import com.kpyruy.takt.core.ui.i18n.TaktI18n
import com.kpyruy.takt.core.ui.theme.TaktTheme
import com.kpyruy.takt.feature.settings.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.ByteArrayOutputStream
import kotlin.math.abs

class BackupPhotoMaterialUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun photo(color: Int): BackupWallpaper {
        val image = Bitmap.createBitmap(180, 240, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }
        val output = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.JPEG, 90, output); image.recycle()
        return BackupWallpaper.fromBytes(output.toByteArray(), "Preview only")
    }
    @Test fun sourcePhotoChangesTheMaterialBackdropAndGlassWithoutChangingPhoneAppearance() {
        val data = (compose.activity.application as TaktApplication).dataContainer
        val beforeSettings = data.settingsRepository.settings.value
        val beforeFiles = java.io.File(compose.activity.filesDir, "wallpapers").listFiles()?.map { it.name }?.toSet().orEmpty()
        val phone = BackupPayload(settings = BackupSettings(cardAppearance = "GLASS", themeMode = "LIGHT", wallpaperRecorded = true))
        val warm = photo(0xFFE44724.toInt())
        val cool = photo(0xFF285EDB.toInt())
        var selected by mutableStateOf(warm)
        compose.activityRule.scenario.onActivity { activity -> activity.setContent {
            TaktI18n.use(AppLanguage.SLOVAK)
            TaktTheme(AppSettings(themeMode = AppThemeMode.LIGHT, language = AppLanguage.SLOVAK)) {
                TaktGlassBackground {
                    val source = phone.copy(settings = phone.settings.copy(themeMode = "DARK", themeFamily = "PURPLE", wallpaper = selected))
                    BackupReviewPage(BackupReviewState(BackupPayloadCodec.encode(phone), BackupPayloadCodec.encode(source), BackupReviewSource.DOCUMENTS), {}, {})
                }
            }
        } }
        fun showMaterial() {
            if (compose.onAllNodesWithTag("backup-appearance-file-material").fetchSemanticsNodes().isEmpty())
                compose.onNodeWithTag("backup-expand-APPEARANCE").performScrollTo().performClick()
            compose.onNodeWithTag("backup-appearance-file-material").performScrollTo().assertIsDisplayed()
            compose.waitForIdle(); android.os.SystemClock.sleep(500)
        }
        fun material(side: String) = compose.onNodeWithTag("backup-appearance-$side-material").captureToImage()
        showMaterial()
        val warmSample = material("file"); val phoneSample = material("phone")
        compose.runOnIdle { selected = cool }
        // Source replacement must refresh pixels even when the detail stays expanded.
        showMaterial()
        val coolSample = material("file"); val unchangedPhone = material("phone")
        assertTrue("Source photo missing outside the card", difference(warmSample, coolSample, false) > .06f)
        assertTrue("Glass still samples the built-in background", difference(warmSample, coolSample, true) > .025f)
        assertTrue("Phone preview changed with source photo", difference(phoneSample, unchangedPhone, false) < .015f)
        assertEquals(beforeSettings, data.settingsRepository.settings.value)
        assertEquals(beforeFiles, java.io.File(compose.activity.filesDir, "wallpapers").listFiles()?.map { it.name }?.toSet().orEmpty())
        capture("backup-photo-material.png")
    }
    private fun difference(a: ImageBitmap, b: ImageBitmap, insideCard: Boolean): Float {
        val left = a.toPixelMap(); val right = b.toPixelMap()
        assertEquals(left.width, right.width); assertEquals(left.height, right.height)
        var total = 0f; var count = 0
        val y = (left.height * if (insideCard) .72f else .05f).toInt()
        for (x in left.width / 3 until left.width * 2 / 3) {
            val p = left[x, y]; val q = right[x, y]
            total += abs(p.red-q.red) + abs(p.green-q.green) + abs(p.blue-q.blue); count += 3
        }
        return total / count
    }
    private fun capture(name: String) {
        compose.waitForIdle(); android.os.SystemClock.sleep(250)
        val image = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        compose.activity.openFileOutput(name, 0).use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }; image.recycle()
    }
}
