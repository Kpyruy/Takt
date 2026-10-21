package com.kpyruy.takt.app

import android.graphics.Bitmap
import androidx.compose.ui.graphics.toPixelMap
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
            data.settingsRepository.setAppearance(data.settingsRepository.settings.first().copy(
                customThemes = emptyList(), selectedCustomThemeId = null))
            val courses = data.database.courseDao().observeAll().first()
            data.database.courseDao().insertAll(courses.map { row ->
                if (row.id.startsWith("glass-4-") || row.id.startsWith("glass-5-")) row.copy(status = "enrolled") else row
            })
            data.awaitRestoredPlanning()
        }
        compose.waitForIdle()
    }
    @Test fun darkChromeScrollingMenusAndMetricsMatchRequestedStates() {
        start()
        root("Предмети")
        capture("subjects-before")
        val before = compose.onNodeWithTag("subjects-glass-header").captureToImage()
        compose.onNode(hasScrollToIndexAction()).performTouchInput { swipeUp(durationMillis = 650) }
        compose.waitForIdle()
        val after = compose.onNodeWithTag("subjects-glass-header").captureToImage()
        val first = before.toPixelMap(); val second = after.toPixelMap()
        var changes = 0
        for (y in 0 until minOf(first.height, second.height) step 4)
            for (x in 0 until minOf(first.width, second.width) step 4) if (first[x,y] != second[x,y]) changes++
        assertTrue("Header must sample scrolled content, not a fixed rectangle", changes > 10)
        capture("subjects-scrolled")
        root("Прогрес")
        compose.onNode(hasText(t("Completed")) and hasClickAction()).performClick()
        capture("progress-completed")
        compose.onNodeWithTag("progress-menu").performClick()
        compose.onNodeWithTag("takt-choice-menu").assertIsDisplayed()
        capture("progress-menu")
        back()
        root("Календар")
        compose.onNodeWithTag("calendar-view-week").performClick()
        compose.onNodeWithTag("week-timetable").assertExists()
        compose.onNodeWithText(t("Week shown as a list for readability.")).assertDoesNotExist()
        capture("week-timetable")
        compose.onNodeWithTag("calendar-view-week").performTouchInput { longClick() }
        compose.onNodeWithTag("takt-choice-menu").assertExists()
        capture("week-menu")
        compose.onNodeWithText(t("List")).performClick()
        capture("week-list")
        root("Предмети")
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(0)
        compose.onNode(hasText("Фізика") and hasClickAction()).performClick()
        compose.onNode(hasText(t("Exam")) and hasClickAction()).performClick()
        compose.onNodeWithTag("exam-semester-metric").assertIsDisplayed()
        compose.onNodeWithTag("exam-available-metric").assertDoesNotExist()
        capture("exam-metrics")
    }
    @Test fun threeRolePickerUpdatesLiveCancelsAndPersistsThroughBackup() {
        start()
        compose.onNodeWithContentDescription(t("Settings")).performClick()
        compose.onNode(hasText(t("Appearance")) and hasClickAction()).performScrollTo().performClick()
        compose.onNodeWithTag("appearance-color-open").performScrollTo().performClick()
        capture("palette-popup")
        compose.onNodeWithTag("theme-create").performScrollTo().performClick()
        compose.onNodeWithTag("theme-name").performTextInput("Evening three colors")
        for ((role, color) in listOf("PRIMARY" to "#B52D67", "SECONDARY" to "#147D64", "TERTIARY" to "#7048C8")) {
            compose.onNodeWithTag("theme-color-$role").performScrollTo().performClick()
            val hex = compose.onNodeWithTag("theme-picker-hex").performScrollTo()
            hex.performTextReplacement(color)
            compose.onNodeWithTag("theme-picker-apply").performClick()
        }
        compose.onNodeWithTag("theme-color-SECONDARY").performScrollTo().performClick()
        val initial = hex()
        compose.onNodeWithTag("theme-picker-sv").performScrollTo().performTouchInput {
            swipe(start = centerLeft + androidx.compose.ui.geometry.Offset(8f, 0f), end = centerRight - androidx.compose.ui.geometry.Offset(8f, 0f), durationMillis = 400)
        }
        compose.onNodeWithTag("theme-picker-hue").performScrollTo().performTouchInput { swipeLeft(durationMillis = 400) }
        assertNotEquals("Dragging must update live HEX", initial, hex())
        capture("color-picker")
        compose.onNodeWithTag("theme-picker-cancel").performClick()
        compose.onNodeWithTag("theme-color-SECONDARY").performScrollTo().performClick()
        assertEquals(initial, hex())
        compose.onNodeWithTag("theme-picker-cancel").performClick()
        compose.onNodeWithTag("theme-save").performClick()
        capture("custom-palette")
        compose.onNodeWithTag("palette-popup-apply").performClick()
        compose.onNodeWithText(t("Apply appearance")).performClick()
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
    @Test fun connectedSubtitleIsLocalizedAndAutomaticApplyIsFirst() {
        start()
        val field = data.documentStore.javaClass.getDeclaredField("_status").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val state = field.get(data.documentStore) as kotlinx.coroutines.flow.MutableStateFlow<DocumentSyncStatus>
        val original = state.value
        try {
            state.value = DocumentSyncStatus.READY // UI-only fixture, no URI/permissions/SAF writes.
            compose.onNodeWithContentDescription(t("Settings")).performClick()
            for (language in listOf(AppLanguage.UKRAINIAN, AppLanguage.ENGLISH, AppLanguage.SLOVAK)) {
                runBlocking { data.settingsRepository.setLanguage(language) }
                compose.waitForIdle()
                // The real document watcher debounces preference changes for 350 ms and
                // correctly clears READY without a tree URI. Set this UI-only state after it.
                android.os.SystemClock.sleep(500)
                runBlocking { data.documentStore.resume() }
                state.value = DocumentSyncStatus.READY
                compose.waitForIdle()
                val label = when (language) {
                    AppLanguage.UKRAINIAN -> "Підключено"
                    AppLanguage.ENGLISH -> "Connected"
                    AppLanguage.SLOVAK -> "Pripojené"
                }
                compose.onNodeWithText(label).performScrollTo().assertExists()
                capture("connected-${language.name.lowercase()}")
            }
            compose.onNode(hasText(t("Automatic UIS updates")) and hasClickAction()).performScrollTo().performClick()
            val apply = compose.onNodeWithText(t("Apply progress automatically")).fetchSemanticsNode().boundsInRoot
            val progress = compose.onNodeWithText(t("Progress")).fetchSemanticsNode().boundsInRoot
            assertTrue(apply.top < progress.top)
            capture("uis-toggle-first")
        } finally { state.value = original }
    }
    @Test fun lessonActionsAndLongAssessmentFieldsUseThemeAndEqualBounds() {
        start()
        root("Календар")
        compose.onNodeWithTag("calendar-view-week").performClick()
        compose.onNodeWithTag("week-timetable-event-glass-lecture").performTouchInput { longClick() }
        capture("lesson-actions")
        compose.onNodeWithText(t("Mark absent")).performClick()
        compose.waitUntil(5_000) { runBlocking { data.scheduleRepository.observeAbsences().first().any { it.eventId == "glass-lecture" } } }
        compose.onNodeWithTag("week-timetable-event-glass-lecture").performTouchInput { longClick() }
        compose.onNodeWithText(t("Remove absence mark")).assertExists()
        capture("lesson-absence-selected")
        back()
        root("Предмети")
        compose.onNode(hasText("Фізика") and hasClickAction()).performClick()
        compose.onNode(hasText(t("Tasks")) and androidx.compose.ui.test.SemanticsMatcher.expectValue(
            SemanticsProperties.Role, androidx.compose.ui.semantics.Role.Tab)).performClick()
        compose.onNodeWithTag("course-work-glass-gate").performScrollTo().performClick()
        compose.onNodeWithTag("grade-details-trigger").performScrollTo().performClick()
        compose.onNodeWithTag("grade-minimum").performScrollTo().assertIsDisplayed()
        val maximum = compose.onNodeWithTag("grade-maximum").fetchSemanticsNode().boundsInRoot
        val minimum = compose.onNodeWithTag("grade-minimum").fetchSemanticsNode().boundsInRoot
        assertTrue(maximum.height < 90 * compose.activity.resources.displayMetrics.density)
        assertTrue(minimum.height < 90 * compose.activity.resources.displayMetrics.density)
        capture("grade-fields")
    }
    private fun hex() = compose.onNodeWithTag("theme-picker-hex").fetchSemanticsNode().config[SemanticsProperties.EditableText].text
    @Test fun recordMotionAtDisplayCadence() {
        org.junit.Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("recordGlassMotion") == "true")
        start()
        android.os.SystemClock.sleep(1700)
        capture("motion-ready")
        compose.mainClock.autoAdvance = false
        fun frames(count: Int) {
            repeat(count) { compose.mainClock.advanceTimeByFrame(); android.os.SystemClock.sleep(17) }
        }
        try {
            frames(30)
            for (label in listOf("Календар", "Предмети", "Прогрес", "Сьогодні", "Прогрес")) {
                root(label); frames(36)
            }
            for (label in listOf("Активні", "Здані", "План")) {
                val node = compose.onNode(hasText(t(label)) and hasClickAction())
                node.performTouchInput { down(center) }
                frames(12)
                node.performTouchInput { up() }
                frames(30)
            }
        } finally { compose.mainClock.autoAdvance = true; compose.waitForIdle() }
    }
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
