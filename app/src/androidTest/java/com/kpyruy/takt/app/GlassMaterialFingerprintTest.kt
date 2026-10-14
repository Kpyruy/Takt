package com.kpyruy.takt.app

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.kpyruy.takt.core.model.*
import com.kpyruy.takt.core.ui.components.*
import com.kpyruy.takt.core.ui.theme.TaktTheme
import org.junit.Rule
import org.junit.Test

/** Static GPU material fingerprint: layout/content are fixed so A/B pixels can be compared. */
class GlassMaterialFingerprintTest {
    @get:Rule val compose = createComposeRule()
    @Test fun dark() = capture(AppThemeMode.DARK)
    @Test fun light() = capture(AppThemeMode.LIGHT)
    private fun capture(mode: AppThemeMode) {
        compose.setContent {
            TaktTheme(AppSettings(themeMode = mode, themeFamily = ThemeFamily.PURPLE)) {
                TaktGlassBackground(Modifier.width(320.dp).height(580.dp).testTag("material-fixture")) {
                    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        GlassSurface(Modifier.fillMaxWidth().height(96.dp), RoundedCornerShape(20.dp)) {
                            Column(Modifier.padding(16.dp)) { Text("Study plan", style = MaterialTheme.typography.titleLarge); Text("Physics · 5 credits") }
                        }
                        GlassSurface(Modifier.fillMaxWidth().height(80.dp), RoundedCornerShape(14.dp), MaterialTheme.colorScheme.primaryContainer) {
                            Text("Next class · 08:00", Modifier.padding(16.dp))
                        }
                        GlassSurface(Modifier.fillMaxWidth().height(88.dp), RoundedCornerShape(28.dp)) { Text("A · 92 points", Modifier.padding(20.dp)) }
                        TaktSegmentedTabs(listOf("Today", "Calendar", "Subjects"), 1, {})
                        TaktPrimaryButton(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Continue") }
                    }
                }
            }
        }
        compose.waitUntil(10_000) {
            val p = compose.onNodeWithTag("material-fixture").captureToImage().toPixelMap()
            (1..5).map { p[2, p.height * it / 7] }.toSet().size > 3
        }
        compose.mainClock.advanceTimeBy(1000); compose.waitForIdle()
        val picture = compose.onNodeWithTag("material-fixture").captureToImage().asAndroidBitmap()
        InstrumentationRegistry.getInstrumentation().targetContext.openFileOutput("material-${mode.name.lowercase()}.png", 0).use {
            picture.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
