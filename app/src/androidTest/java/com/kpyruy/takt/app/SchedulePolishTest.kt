package com.kpyruy.takt.app

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.kpyruy.takt.core.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/** Opt-in, destructive fixture setup; disposable review emulator only. */
class SchedulePolishTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun tapHoldAbsenceSettingsAndIconScroll() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("pulseReview") == "true")
        val data = (compose.activity.application as TaktApplication).dataContainer
        val repo = data.scheduleRepository
        runBlocking {
            data.seedIfNeeded()
            data.settingsRepository.setThemeMode(if (InstrumentationRegistry.getArguments().getString("dark") == "true") AppThemeMode.DARK else AppThemeMode.LIGHT)
            data.settingsRepository.setThemeFamily(ThemeFamily.BLUE)
            data.settingsRepository.setWeekLayout(WeekLayout.COMPACT_LIST)
            repo.observeRules().first().forEach { repo.deleteRule(it.id) }
            repo.observeOneOffEvents().first().forEach { repo.deleteOneOffEvent(it.id) }
            repo.observeExceptions().first().forEach { repo.deleteException(it.id) }
            data.studyContentRepository.observeAllTasks().first().forEach { data.studyContentRepository.deleteTask(it.id) }
            data.studyPlanRepository.setIcon("FYZI_6B", "Science")
            data.studyPlanRepository.setGradingType("FYZI_6B", CourseGradingType.EXAM_LETTER)
            listOf(
                ScheduleRule("polish-physics", "FYZI_6B", "Fyzika · prednáška", LocalDate.now().dayOfWeek, LocalTime.of(10,30), LocalTime.NOON, ScheduleRecurrence.WEEKLY, "AB-32", LessonType.LECTURE),
                ScheduleRule("polish-tpar", "TPAR_6B", "Technické prostriedky", LocalDate.now().dayOfWeek, LocalTime.of(13,0), LocalTime.of(14,30), ScheduleRecurrence.WEEKLY, "CD-08", LessonType.SEMINAR),
                ScheduleRule("polish-stats", "ZAST_6B", "Základy štatistiky", LocalDate.now().dayOfWeek, LocalTime.of(15,0), LocalTime.of(17,0), ScheduleRecurrence.WEEKLY, "T-106", LessonType.PRACTICE),
            ).forEach { repo.upsertRule(it) }
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Fyzika · prednáška").fetchSemanticsNodes().isNotEmpty() }
        lesson().performClick()
        compose.onNodeWithContentDescription("Налаштування предмета").assertExists().performClick()
        compose.onNodeWithText("ОЦІНЮВАННЯ").assertIsDisplayed()
        capture("settings")
        compose.onNodeWithContentDescription("Змінити статус").performClick()
        compose.onNodeWithTag("settings-status-PLANNED").performClick()
        compose.waitUntil(5_000) { runBlocking { data.studyPlanRepository.observeCourse("FYZI_6B").first()?.status == CourseStatus.PLANNED } }
        compose.onNodeWithContentDescription("Змінити статус").performClick()
        compose.onNodeWithTag("settings-status-ENROLLED").performClick()
        compose.onNodeWithText("Іконка предмета").performClick()
        // Swiping at both boundaries must never drag/dismiss the icon dialog.
        repeat(5) { compose.onNodeWithTag("course-icon-grid").performTouchInput { swipeDown() } }
        compose.onNodeWithContentDescription("Закрити іконки").assertIsDisplayed()
        compose.onNodeWithTag("course-icon-grid").performScrollToIndex(108)
        repeat(5) { compose.onNodeWithTag("course-icon-grid").performTouchInput { swipeUp() } }
        compose.onNodeWithContentDescription("Закрити іконки").assertIsDisplayed()
        capture("icons")
        compose.onNodeWithTag("course-icon-grid").performScrollToIndex(0)
        compose.onNodeWithTag("course-icon-grid").performTouchInput { swipeDown() }
        compose.onNodeWithContentDescription("Закрити іконки").performClick()
        back()
        lesson().performTouchInput { longClick(durationMillis = 1000) }
        compose.onNodeWithText("Позначити пропуск").assertIsDisplayed()
        compose.onNodeWithText("Позначка лише для цього заняття").assertDoesNotExist()
        compose.onNodeWithText("Редагувати").assertIsDisplayed()
        compose.onNodeWithText("Редагувати повторення").assertDoesNotExist()
        capture("lesson-actions")
        compose.onNodeWithText("Позначити пропуск").performClick()
        compose.waitUntil(5_000) { runBlocking { repo.observeAbsences().first().size == 1 } }
        compose.onNodeWithText("Пропущено", substring = true).assertExists()
        capture("home")
        root("Календар")
        lesson().performScrollTo().performClick()
        compose.onNodeWithContentDescription("Налаштування предмета").assertExists()
        back()
        lesson().performScrollTo().performTouchInput { longClick(durationMillis = 1000) }
        compose.onNodeWithText("Зняти позначку пропуску").performClick()
        compose.waitUntil(5_000) { runBlocking { repo.observeAbsences().first().isEmpty() } }
        lesson().performTouchInput { longClick() }
        compose.onNodeWithText("Позначити пропуск").performClick()
        compose.onNodeWithText("Пропущено", substring = true).assertExists()
        capture("calendar")
        // Check week list tap/hold and centred time layout visually.
        compose.onNode(hasText("Тиждень") and hasClickAction()).performClick()
        lesson().performScrollTo().performClick()
        compose.onNodeWithContentDescription("Налаштування предмета").assertExists()
        back()
        // Calendar preserves its mode on reentry.
        compose.onNode(hasText("Тиждень") and hasClickAction()).performClick()
        lesson().performScrollTo()
        capture("week")
        lesson().performTouchInput { longClick() }
        compose.onNodeWithText("Зняти позначку пропуску").assertExists()
        back()
        compose.onNode(hasText("Таймтейбл") and hasClickAction()).performClick()
        lesson().performScrollTo().performClick()
        compose.onNodeWithContentDescription("Налаштування предмета").assertExists()
        back()
        compose.onNode(hasText("Таймтейбл") and hasClickAction()).assertIsSelected()
        lesson().performScrollTo().performTouchInput { longClick() }
        compose.onNodeWithText("Зняти позначку пропуску").assertExists()
        back()
        compose.onNode(hasText("Місяць") and hasClickAction()).performClick()
        lesson().performScrollTo().performClick()
        compose.onNodeWithContentDescription("Налаштування предмета").assertExists()
        back()
        compose.onNode(hasText("Місяць") and hasClickAction()).assertIsSelected()
        lesson().performScrollTo().performTouchInput { longClick() }
        compose.onNodeWithText("Зняти позначку пропуску").assertExists()
        back()
        // Export/import preserves attendance.
        runBlocking {
            val json = data.backupRepository.exportJson()
            val event = ScheduleResolver.eventsForDate(repo.observeRules().first(), emptyList(), emptyList(), LocalDate.now()).first()
            repo.setAbsent(event, false)
            data.backupRepository.importJson(json)
            assertEquals(1, repo.observeAbsences().first().size)
        }
        compose.activityRule.scenario.recreate()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Пропущено", substring = true).fetchSemanticsNodes().isNotEmpty() }
        assertEquals(1, runBlocking { repo.observeAbsences().first().size })
    }
    private fun lesson() = compose.onNode(hasText("Fyzika · prednáška", substring = true) and hasClickAction())
    private fun root(label: String) = compose.onAllNodes(hasText(label) and hasClickAction()).onLast().performClick()
    private fun back() {
        val closeSheet = compose.onAllNodesWithContentDescription("Закрити дії пари")
        if (closeSheet.fetchSemanticsNodes().isNotEmpty()) closeSheet.onFirst().performClick()
        else compose.onNodeWithContentDescription("Назад").performClick()
        compose.waitForIdle()
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        android.os.SystemClock.sleep(400)
        val image = instrumentation.uiAutomation.takeScreenshot()
        val suffix = instrumentation.argumentsSuffix()
        instrumentation.targetContext.openFileOutput("polish-$name$suffix.png", 0).use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        image.recycle()
    }
    private fun android.app.Instrumentation.argumentsSuffix() = InstrumentationRegistry.getArguments().getString("screenshotSuffix").orEmpty()
}
