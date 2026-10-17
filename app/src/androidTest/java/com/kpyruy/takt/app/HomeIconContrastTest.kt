package com.kpyruy.takt.app

import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.platform.app.InstrumentationRegistry
import com.kpyruy.takt.core.model.*
import com.kpyruy.takt.core.ui.components.TaktGlassBackground
import com.kpyruy.takt.core.ui.i18n.TaktI18n
import com.kpyruy.takt.core.ui.theme.TaktTheme
import com.kpyruy.takt.feature.home.HomeScreen
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.MutableStateFlow
import com.kpyruy.takt.core.data.PlanningSnapshot
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class HomeIconContrastTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val data get() = (compose.activity.application as TaktApplication).dataContainer
    @Test fun assessmentGlyphRemainsVisibleInEveryMaterialWithLightDarkAndCustomThemes() {
        runBlocking {
            data.seedLiquidGlassReview()
            data.settingsRepository.setLanguage(AppLanguage.SLOVAK)
            data.settingsRepository.setHomeWorkFilter(HomeWorkFilter())
            data.gradeRepository.upsertItem(GradeItem(id = "icon-review", courseId = "glass-physics", title = "Test",
                type = GradeItemType.TEST, earnedPoints = 0.0, maxPoints = 20.0, dueDate = LocalDate.now().plusDays(1), completed = false))
            data.awaitRestoredPlanning()
        }
        // Freeze Room inputs: rapid material scenes must not race the test frame clock with IO-driven replanning.
        val scenePlan = MutableStateFlow<PlanningSnapshot?>(runBlocking { data.readPlanningSnapshot() })
        compose.activityRule.scenario.onActivity { activity -> activity.setContent {
            val planning by scenePlan.collectAsStateWithLifecycle()
            val settings = planning!!.settings
            TaktI18n.use(settings.language)
            TaktTheme(settings) {
                Box(Modifier.testTag("home-icon-scene-${settings.cardAppearance}-${settings.themeMode}-${settings.selectedCustomThemeId}")) {
                    TaktGlassBackground {
                        HomeScreen(scenePlan, data.studyContentRepository, data.settingsRepository, {}, {},
                            onOpenCourse = {}, onOpenAssessment = {}, onEventLongClick = {})
                    }
                }
            }
        } }
        try {
            for (custom in listOf(false, true)) for (mode in listOf(AppThemeMode.LIGHT, AppThemeMode.DARK))
                for (style in CardAppearance.entries) {
                    val sceneSettings = scenePlan.value!!.settings.copy(
                        cardAppearance = style, themeMode = mode, themeFamily = ThemeFamily.BLUE, customWallpaper = null,
                        customThemes = if (custom) listOf(CustomColorTheme("icon-purple", "Moja", "#6938B5")) else emptyList(),
                        selectedCustomThemeId = if (custom) "icon-purple" else null)
                    compose.runOnIdle { scenePlan.value = scenePlan.value!!.copy(settings = sceneSettings) }
                    val scene = "home-icon-scene-$style-$mode-${if (custom) "icon-purple" else "null"}"
                    compose.waitUntil(5_000) { compose.onAllNodesWithTag(scene).fetchSemanticsNodes().isNotEmpty() }
                    compose.onNodeWithTag("home-assessment-icon-review").performScrollTo().assertIsDisplayed()
                    val pixels = compose.onNodeWithTag("home-assessment-glyph-icon-review", useUnmergedTree = true)
                        .captureToImage().toPixelMap()
                    var low = 1f; var high = 0f
                    for (y in 0 until pixels.height) for (x in 0 until pixels.width) {
                        val light = pixels[x, y].luminance(); low = minOf(low, light); high = maxOf(high, light)
                    }
                    assertTrue("Invisible assessment glyph: $style $mode custom=$custom range=${high-low}", high-low > .12f)
                    if (mode == AppThemeMode.DARK && custom) capture("home-icon-${style.name}.png")
                }
        } finally {
            runBlocking { data.gradeRepository.deleteItem("icon-review") }
        }
    }
    private fun capture(name: String) {
        compose.waitForIdle(); android.os.SystemClock.sleep(250)
        val image = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        compose.activity.openFileOutput(name, 0).use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        image.recycle()
    }
}
