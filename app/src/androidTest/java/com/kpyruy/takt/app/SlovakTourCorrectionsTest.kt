package com.kpyruy.takt.app

import android.graphics.Bitmap
import com.kpyruy.takt.core.ui.components.TaktTourSemantics
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.kpyruy.takt.core.model.*
import com.kpyruy.takt.core.ui.i18n.t
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class SlovakTourCorrectionsTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val data get() = (compose.activity.application as TaktApplication).dataContainer

    @Before fun startSlovakTour() {
        runBlocking {
            data.settingsRepository.setLanguage(AppLanguage.SLOVAK)
            data.settingsRepository.setThemeMode(AppThemeMode.DARK)
            data.settingsRepository.setThemeFamily(ThemeFamily.BLUE)
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("root-navigation").fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithContentDescription(t("Налаштування")).onFirst().performClick()
        compose.onNodeWithText(t("Екскурсія по Takt")).performScrollTo().performClick()
    }

    private fun phase(value: String) = compose.waitUntil(8_000) {
        compose.onNodeWithTag("guided-tour").fetchSemanticsNode().config[TaktTourSemantics.Playback].endsWith(":$value")
    }
    private fun ready() = compose.waitUntil(8_000) {
        compose.onAllNodesWithTag("tour-next").fetchSemanticsNodes().any { !it.config.contains(SemanticsProperties.Disabled) }
    }
    private fun calendarState(): String = compose.onNodeWithTag("calendar-screen", useUnmergedTree = true)
        .fetchSemanticsNode().config[TaktTourSemantics.CalendarState]

    @Test fun wholeRenderedTourIsSlovakAndItsDemonstrationsActuallyPerformTheirActions() {
        val original = runBlocking { data.backupRepository.exportJson() }
        GuidedTourStep.entries.forEach { step ->
            compose.onNodeWithTag("tour-card-${step.name}").assertIsDisplayed()
            compose.onNodeWithTag("tour-scene-${step.scene}").assertIsDisplayed()
            if (step == GuidedTourStep.CALENDAR_TODAY) {
                phase("AWAY_FROM_TODAY")
                val away = calendarState()
                assertFalse(away.contains(":${LocalDate.now()}:"))
                capture("correction-sk-away.png")
                phase("RETURNED_TODAY")
                assertTrue(calendarState().contains(":${LocalDate.now()}:"))
                capture("correction-sk-returned.png")
                ready()
                compose.onNodeWithTag("tour-replay").performClick()
                phase("AWAY_FROM_TODAY")
                assertNotEquals(away, calendarState())
            }
            if (step == GuidedTourStep.CALENDAR_LAYOUT) {
                phase("MENU_OPEN")
                compose.onNodeWithTag("takt-choice-menu").assertIsDisplayed()
                compose.onNodeWithText(t("Таймтейбл")).assertIsDisplayed()
                compose.onNodeWithText(t("Список")).assertIsDisplayed()
                capture("correction-sk-popup.png")
                phase("LIST_SELECTED")
                assertTrue(calendarState().endsWith(WeekLayout.COMPACT_LIST.name))
            }
            ready()
            assertNoCyrillic(step.name)
            if (step == GuidedTourStep.ADD) {
                val target = compose.onNodeWithTag("tour-spotlight").fetchSemanticsNode().boundsInRoot
                for (type in listOf(CreateItemType.CLASS, CreateItemType.TEST, CreateItemType.EXAM)) {
                    val tile = compose.onNodeWithTag("global-add-${type.name}", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
                    assertTrue("$type is inside the instructional focus", target.contains(tile.center))
                }
            }
            if (step in listOf(GuidedTourStep.SUBJECTS, GuidedTourStep.ADD, GuidedTourStep.PERIODS)) capture("correction-sk-${step.name}.png")
            if (step == GuidedTourStep.PERIODS) compose.onNodeWithText("Dokončiť").assertIsDisplayed()
            compose.onNodeWithTag("tour-next").performClick()
        }
        compose.onNodeWithTag("tour-completed").assertIsDisplayed()
        assertNoCyrillic("completion")
        assertEquals(original, runBlocking { data.backupRepository.exportJson() })
        capture("correction-sk-complete.png")
    }

    private fun assertNoCyrillic(step: String) {
        val values = compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text), useUnmergedTree = true)
            .fetchSemanticsNodes().filter { it.boundsInRoot.width > 0 && it.boundsInRoot.height > 0 }
            .flatMap { it.config[SemanticsProperties.Text] }.map { it.text }
        assertTrue("$step renders text", values.isNotEmpty())
        assertEquals("$step must contain Slovak throughout", emptyList<String>(), values.filter { it.contains(Regex("[А-Яа-яІіЇїЄє]")) })
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        android.os.SystemClock.sleep(80)
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        compose.activity.openFileOutput(name, 0).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
