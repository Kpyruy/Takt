package com.kpyruy.takt.app

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.test.platform.app.InstrumentationRegistry
import com.kpyruy.takt.core.data.*
import com.kpyruy.takt.core.model.*
import com.kpyruy.takt.core.ui.i18n.t
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Real candidate routes; the fixture is explicitly emulator-only. */
class EveningGlassReviewTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val data get() = (compose.activity.application as TaktApplication).dataContainer
    private fun start() {
        runBlocking {
            data.seedLiquidGlassReview()
            data.settingsRepository.setLanguage(AppLanguage.SLOVAK)
            data.settingsRepository.setThemeMode(AppThemeMode.DARK)
            data.settingsRepository.setThemeFamily(ThemeFamily.MONOCHROME)
        }
        compose.waitForIdle()
    }
    @Test fun darkChromeScrollingMenusAndMetricsMatchRequestedStates() {
        start()
        root("Предмети")
        capture("subjects-before")
        val before = compose.onNodeWithTag("subjects-glass-header").captureToImage()
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(2)
        val after = compose.onNodeWithTag("subjects-glass-header").captureToImage()
        val first = before.toPixelMap(); val second = after.toPixelMap()
        var changes = 0
        for (y in 0 until minOf(first.height, second.height) step 4)
            for (x in 0 until minOf(first.width, second.width) step 4) if (first[x,y] != second[x,y]) changes++
        assertTrue("Header must sample scrolled content, not a fixed rectangle", changes > 10)
        capture("subjects-scrolled")
        root("Прогрес")
        compose.onNode(hasText(t("Здані")) and hasClickAction()).performClick()
        capture("progress-completed")
        compose.onNodeWithTag("progress-menu").performClick()
        compose.onNodeWithTag("takt-choice-menu").assertIsDisplayed()
        capture("progress-menu")
        back()
        root("Календар")
        compose.onNodeWithTag("calendar-view-week").performClick()
        compose.onNodeWithTag("week-timetable").assertExists()
        compose.onNodeWithText(t("Для читабельності тиждень показано списком.")).assertDoesNotExist()
        capture("week-timetable")
        compose.onNodeWithTag("calendar-view-week").performTouchInput { longClick() }
        compose.onNodeWithTag("takt-choice-menu").assertExists()
        capture("week-menu")
        compose.onNodeWithText(t("Список")).performClick()
        capture("week-list")
        root("Предмети")
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(0)
        compose.onNode(hasText("Фізика") and hasClickAction()).performClick()
        compose.onNode(hasText(t("Екзамен")) and hasClickAction()).performClick()
        val a = compose.onNodeWithTag("exam-semester-metric").fetchSemanticsNode().boundsInRoot
        val b = compose.onNodeWithTag("exam-available-metric").fetchSemanticsNode().boundsInRoot
        assertEquals(a.height, b.height, 1f)
        capture("exam-metrics")
    }
    @Test fun threeRolePickerUpdatesLiveCancelsAndPersistsThroughBackup() {
        start()
        compose.onNodeWithContentDescription(t("Налаштування")).performClick()
        compose.onNode(hasText(t("Вигляд")) and hasClickAction()).performScrollTo().performClick()
        compose.onNodeWithTag("appearance-color-open").performScrollTo().performClick()
        capture("palette-popup")
        compose.onNodeWithTag("theme-create").performScrollTo().performClick()
        compose.onNodeWithTag("theme-name").performTextInput("Evening three colors")
        for ((role, color) in listOf("PRIMARY" to "#B52D67", "SECONDARY" to "#147D64", "TERTIARY" to "#7048C8")) {
            compose.onNodeWithTag("theme-color-$role").performScrollTo().performClick()
            val hex = compose.onNodeWithTag("theme-picker-hex").performScrollTo()
            hex.performTextReplacement(color)
            compose.onNodeWithTag("theme-picker-apply").performScrollTo().performClick()
        }
        compose.onNodeWithTag("theme-color-SECONDARY").performScrollTo().performClick()
        val initial = hex()
        compose.onNodeWithTag("theme-picker-sv").performScrollTo().performTouchInput {
            swipe(start = centerLeft + androidx.compose.ui.geometry.Offset(8f, 0f), end = centerRight - androidx.compose.ui.geometry.Offset(8f, 0f), durationMillis = 400)
        }
        compose.onNodeWithTag("theme-picker-hue").performScrollTo().performTouchInput { swipeLeft(durationMillis = 400) }
        assertNotEquals("Dragging must update live HEX", initial, hex())
        capture("color-picker")
        compose.onNodeWithTag("theme-picker-cancel").performScrollTo().performClick()
        compose.onNodeWithTag("theme-color-SECONDARY").performScrollTo().performClick()
        assertEquals(initial, hex())
        compose.onNodeWithTag("theme-picker-cancel").performScrollTo().performClick()
        compose.onNodeWithTag("theme-save").performClick()
        capture("custom-palette")
        compose.onNodeWithTag("palette-popup-apply").performClick()
        compose.onNodeWithText(t("Застосувати вигляд")).performClick()
        compose.waitForIdle()
        val saved = runBlocking { data.settingsRepository.settings.first() }.selectedCustomTheme!!
        assertEquals("#B52D67", saved.primaryHex)
        assertEquals("#147D64", saved.secondaryHex)
        assertEquals("#7048C8", saved.tertiaryHex)
        val backup = runBlocking { data.backupRepository.exportJson() }
        val before = runBlocking { data.settingsRepository.settings.first() }
        runBlocking {
            data.settingsRepository.setThemeFamily(ThemeFamily.BLUE)
            data.backupRepository.importSelectedJson(backup, data.backupRepository.exportJson(), setOf(BackupSection.APPEARANCE))
        }
        assertEquals(saved, runBlocking { data.settingsRepository.settings.first() }.selectedCustomTheme)
        assertEquals(before.language, runBlocking { data.settingsRepository.settings.first() }.language)
        assertEquals(saved, runBlocking { SharedPreferencesAppSettingsRepository(instrumentation.targetContext).settings.first() }.selectedCustomTheme)
    }
    @Test fun navigationLensMovesThroughIntermediateFrameAndHasNoDetachedRectangle() {
        start()
        val first = compose.onNodeWithTag("navigation-lens").fetchSemanticsNode().boundsInRoot.left
        compose.mainClock.autoAdvance = false
        root("Календар")
        compose.mainClock.advanceTimeBy(64)
        val mid = compose.onNodeWithTag("navigation-lens").fetchSemanticsNode().boundsInRoot.left
        compose.mainClock.advanceTimeBy(1000)
        val end = compose.onNodeWithTag("navigation-lens").fetchSemanticsNode().boundsInRoot.left
        assertTrue("Lens needs intermediate displacement", mid > first + 1 && mid < end - 1)
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        capture("navigation-final")
    }
    private fun hex() = compose.onNodeWithTag("theme-picker-hex").fetchSemanticsNode().config[SemanticsProperties.EditableText].text
    private fun root(label: String) = compose.onAllNodes(hasText(t(label)) and hasClickAction()).onLast().performClick()
    private fun back() { compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }; compose.waitForIdle() }
    private fun capture(name: String) {
        compose.mainClock.advanceTimeBy(600); compose.waitForIdle()
        val dismissId = compose.activity.resources.getIdentifier("m3c_snackbar_dismiss", "string", compose.activity.packageName)
        if (dismissId != 0) {
            val labels = listOf(java.util.Locale.ENGLISH, java.util.Locale("sk"), java.util.Locale("uk")).map {
                compose.activity.createConfigurationContext(android.content.res.Configuration(compose.activity.resources.configuration).apply { setLocale(it) }).getString(dismissId)
            }
            val nodes = compose.onAllNodes(labels.map { hasContentDescription(it) }.reduce { a,b -> a or b })
            if (nodes.fetchSemanticsNodes().size == 1) nodes.onFirst().performClick()
        }
        compose.waitForIdle(); instrumentation.waitForIdleSync(); android.os.SystemClock.sleep(250)
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        instrumentation.targetContext.openFileOutput("evening-$name.png", 0).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
