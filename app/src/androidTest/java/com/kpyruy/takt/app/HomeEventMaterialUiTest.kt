package com.kpyruy.takt.app

import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.kpyruy.takt.core.data.PlanningSnapshot
import com.kpyruy.takt.core.model.*
import com.kpyruy.takt.core.ui.i18n.TaktI18n
import com.kpyruy.takt.core.ui.theme.TaktTheme
import com.kpyruy.takt.feature.home.HomeScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.abs

class HomeEventMaterialUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val data get() = (compose.activity.application as TaktApplication).dataContainer

    @Test fun courseLessEventGlyphIsLegibleAndOnlyGlassLetsWallpaperThroughTheActiveCard() {
        runBlocking { data.seedLiquidGlassReview(); data.awaitRestoredPlanning() }
        val source = runBlocking { data.readPlanningSnapshot() }
        val event = OneOffScheduleEvent("material-event", null, "Samostatná udalosť", LocalDate.now(),
            LocalTime.MIN, LocalTime.MAX, room = "T02 4.105", type = OneOffScheduleEventType.EXTRA,
            lessonType = LessonType.EVENT)
        val plan = MutableStateFlow<PlanningSnapshot?>(source.copy(rules = emptyList(), exceptions = emptyList(),
            absences = emptyList(), oneOffEvents = listOf(event), tasks = emptyList(), assessments = emptyList(),
            settings = source.settings.copy(language = AppLanguage.SLOVAK, themeMode = AppThemeMode.DARK,
                customWallpaper = null, customThemes = emptyList(), selectedCustomThemeId = null)))
        var wallpaper by mutableStateOf(Color(0xFFE64A19))
        compose.activityRule.scenario.onActivity { activity -> activity.setContent {
            val snapshot by plan.collectAsState()
            val settings = snapshot!!.settings
            TaktI18n.use(settings.language)
            TaktTheme(settings) {
                Box(Modifier.fillMaxSize().background(wallpaper).testTag("event-scene-${settings.themeMode}-${settings.cardAppearance}")) {
                    HomeScreen(plan, data.studyContentRepository, data.settingsRepository, {}, {},
                        onOpenCourse = {}, onOpenAssessment = {}, onEventLongClick = {})
                }
            }
        } }
        for (mode in listOf(AppThemeMode.LIGHT, AppThemeMode.DARK)) for (style in CardAppearance.entries.sortedBy { it == CardAppearance.GLASS }) {
            compose.runOnIdle {
                plan.value = plan.value!!.copy(settings = plan.value!!.settings.copy(themeMode = mode, cardAppearance = style))
                wallpaper = if (mode == AppThemeMode.DARK) Color(0xFF081521) else Color(0xFFE7F3FA)
            }
            compose.waitUntil(5_000) {
                compose.onAllNodesWithTag("event-scene-$mode-$style").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("home-lesson-material-event").performScrollTo().assertIsDisplayed()
            capture("home-event-${style.name}.png")
            compose.onNodeWithTag("home-active-material-event", useUnmergedTree = true).assertExists()
            val glyph = compose.onNodeWithTag("home-event-avatar-material-event", useUnmergedTree = true).captureToImage().toPixelMap()
            var low = 1f; var high = 0f
            // Sample the icon and its immediate fill, excluding the avatar's outer rim.
            for (y in glyph.height / 4 until glyph.height * 3 / 4) for (x in glyph.width / 4 until glyph.width * 3 / 4) {
                val luminance = glyph[x, y].luminance(); low = minOf(low, luminance); high = maxOf(high, luminance)
            }
            assertTrue("Course-less event glyph needs 3:1 contrast: $style $mode contrast=${(high+.05f)/(low+.05f)}",
                (high + .05f) / (low + .05f) >= 3f)
            compose.runOnIdle { wallpaper = Color(0xFFE64A19) }
            compose.waitForIdle()
            val warm = compose.onNodeWithTag("home-lesson-material-event").captureToImage()
            compose.runOnIdle { wallpaper = Color(0xFF1565C0) }
            compose.waitForIdle()
            val cool = compose.onNodeWithTag("home-lesson-material-event").captureToImage()
            val difference = backgroundDifference(warm, cool)
            if (style == CardAppearance.GLASS) assertTrue("Glass lost its transparency", difference > .02f)
            else assertTrue("$style active card reveals the wallpaper: difference=$difference", difference < .005f)
            if (mode == AppThemeMode.DARK) capture("home-event-${style.name}.png")
        }
    }

    private fun backgroundDifference(a: ImageBitmap, b: ImageBitmap): Float {
        val left = a.toPixelMap(); val right = b.toPixelMap()
        // The top inset lies inside the card, above all text, avatars and status pills.
        val y = (left.height * .04f).toInt().coerceAtLeast(1)
        var total = 0f; var count = 0
        for (x in left.width / 3 until left.width * 2 / 3) {
            val p = left[x, y]; val q = right[x, y]
            total += abs(p.red-q.red) + abs(p.green-q.green) + abs(p.blue-q.blue); count += 3
        }
        return total / count
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val image = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        compose.activity.openFileOutput(name, 0).use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        image.recycle()
    }
}
