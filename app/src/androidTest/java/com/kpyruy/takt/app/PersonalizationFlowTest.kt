package com.kpyruy.takt.app

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.kpyruy.takt.core.model.*
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/** Destructive fixture setup is opt-in and restricted to a disposable review emulator. */
class PersonalizationFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun lessonTypesIconsCrossSemesterStatusesAndGesturesPersist() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("pulseReview") == "true")
        val data = (compose.activity.application as TaktApplication).dataContainer
        val fifth = runBlocking {
            data.seedIfNeeded()
            data.settingsRepository.setThemeMode(if (InstrumentationRegistry.getArguments().getString("dark") == "true") AppThemeMode.DARK else AppThemeMode.LIGHT)
            data.studyContentRepository.observeAllTasks().first().forEach { data.studyContentRepository.deleteTask(it.id) }
            data.studyContentRepository.upsertTask(StudyTask("gesture-task", "FYZI_6B", "Повторити рівняння руху", null, null, false))
            data.studyPlanRepository.setIcon("FYZI_6B", null)
            data.studyPlanRepository.setIcon("TPAR_6B", "Memory")
            data.studyPlanRepository.updateStatus("FYZI_6B", CourseStatus.ENROLLED)
            data.scheduleRepository.observeRules().first().forEach { data.scheduleRepository.deleteRule(it.id) }
            data.scheduleRepository.observeOneOffEvents().first().forEach { data.scheduleRepository.deleteOneOffEvent(it.id) }
            data.scheduleRepository.upsertRule(ScheduleRule("metadata-review", "FYZI_6B", "Тестова лекція", LocalDate.now().dayOfWeek,
                LocalTime.of(10,30), LocalTime.NOON, ScheduleRecurrence.WEEKLY, "AB-32", LessonType.LECTURE))
            data.scheduleRepository.upsertRule(ScheduleRule("metadata-seminar", "TPAR_6B", "Technické prostriedky", LocalDate.now().dayOfWeek,
                LocalTime.of(13,15), LocalTime.of(14,45), ScheduleRecurrence.WEEKLY, "CD-08", LessonType.LECTURE))
            data.studyPlanRepository.observeCourses().first().first { it.semester == 5 }.also {
                data.studyPlanRepository.updateStatus(it.id, CourseStatus.PLANNED)
            }
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Тестова лекція").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Лекція · AB-32", substring = true).assertExists()
        compose.onNodeWithTag("home-week-strip").performTouchInput { swipeLeft() }
        compose.onNodeWithText("Твій день").assertExists()
        compose.onNodeWithTag("root-content").performTouchInput {
            swipe(Offset(width * .4f, 5f), Offset(width * .45f, 5f), 200)
        }
        compose.onNodeWithText("Твій день").assertExists()
        // Blank top padding: user's requested right = next, left = previous.
        swipeMenu(right = false)
        compose.onNodeWithText("Твій день").assertExists()
        swipeMenu(right = true)
        compose.onNode(hasText("День") and hasClickAction()).assertExists()
        swipeMenu(right = true)
        compose.onNodeWithText("Знайти предмет").assertExists()
        swipeMenu(right = true)
        compose.onNodeWithText("Твій шлях").assertExists()
        swipeMenu(right = true) // Last menu does not wrap to Home.
        compose.onNodeWithText("Твій шлях").assertExists()
        swipeMenu(right = false)
        compose.onNodeWithText("Знайти предмет").assertExists()

        // Pick a custom icon through the actual subject UI.
        compose.onNode(hasText("Fyzika") and hasClickAction()).performScrollTo().performClick()
        compose.onNodeWithContentDescription("Змінити іконку предмета").performClick()
        capture("icons")
        compose.onNode(hasSetTextAction()).performTextInput("Science")
        compose.onNodeWithTag("course-icon-Science").performClick()
        compose.waitUntil(5_000) { runBlocking { data.studyPlanRepository.observeCourse("FYZI_6B").first()?.iconKey == "Science" } }
        compose.onNodeWithContentDescription("Хімія колба").assertExists()
        back()
        root("Прогрес")
        compose.onNodeWithTag("progress-screen").performScrollToNode(hasTestTag("semester-toggle-5"))
        compose.onNodeWithTag("semester-toggle-5").performClick()
        compose.onNodeWithTag("progress-screen").performScrollToNode(hasTestTag("status-${fifth.id}"))
        compose.onNodeWithTag("status-${fifth.id}").performClick()
        capture("status")
        compose.onNodeWithTag("choose-status-ENROLLED").performClick()
        compose.waitUntil(5_000) { runBlocking { data.studyPlanRepository.observeCourse(fifth.id).first()?.status == CourseStatus.ENROLLED } }
        // Jump to top and show active subjects across semester boundaries.
        compose.onNodeWithTag("progress-screen").performScrollToIndex(0)
        compose.onNode(hasText("Активні", substring = true) and hasClickAction()).performClick()
        compose.onNodeWithTag("progress-screen").performScrollToNode(hasTestTag("progress-course-${fifth.id}"))
        compose.onNodeWithTag("progress-course-${fifth.id}").assertIsDisplayed()
        capture("active")
        compose.onNodeWithTag("status-${fifth.id}").performClick()
        val beforeCredits = runBlocking { data.studyPlanRepository.observeCourses().first().filter { it.status == CourseStatus.FULFILLED }.sumOf { it.credits } }
        compose.onNodeWithTag("choose-status-FULFILLED").performClick()
        compose.waitUntil(5_000) { runBlocking { data.studyPlanRepository.observeCourse(fifth.id).first()?.status == CourseStatus.FULFILLED } }
        assertEquals(beforeCredits + fifth.credits, runBlocking { data.studyPlanRepository.observeCourses().first().filter { it.status == CourseStatus.FULFILLED }.sumOf { it.credits } })
        compose.onNodeWithTag("progress-screen").performScrollToIndex(0)
        compose.onNode(hasText("Здані", substring = true) and hasClickAction()).performClick()
        capture("completed")
        compose.onNodeWithTag("progress-screen").performScrollToNode(hasTestTag("progress-course-${fifth.id}"))
        compose.onNodeWithTag("progress-course-${fifth.id}").assertExists()
        // Restore active status for final demo; independent of the current third semester.
        compose.onNodeWithTag("status-${fifth.id}").performClick()
        compose.onNodeWithTag("choose-status-ENROLLED").performClick()
        root("Календар")
        compose.onNode(hasText("Тестова лекція") and hasClickAction()).performScrollTo().performTouchInput { longClick() }
        compose.onNodeWithText("Редагувати повторення").performClick()
        compose.onNode(hasText("Семінар") and hasClickAction()).performScrollTo().performClick().assertIsSelected()
        compose.onNode(hasText("Тестова лекція") and hasSetTextAction()).performTextReplacement("Fyzika")
        compose.onNodeWithText("Оновити").performScrollTo().performClick()
        try {
            compose.waitUntil(5_000) { runBlocking { data.scheduleRepository.observeRules().first().first { it.id == "metadata-review" }.lessonType == LessonType.SEMINAR } }
        } catch (failure: Throwable) {
            InstrumentationRegistry.getInstrumentation().targetContext.openFileOutput("form-debug.txt", 0).use {
                it.write(compose.onRoot(useUnmergedTree = true).printToString().toByteArray())
            }
            throw failure
        }
        capture("calendar")
        root("Сьогодні")
        compose.onNodeWithText("Семінар · AB-32", substring = true).assertExists()
        compose.onNodeWithContentDescription("Хімія колба").assertExists()
        capture("home")
        // A task's own completion swipe must stay on Home and update only that task.
        compose.onNodeWithText("Повторити рівняння руху").performScrollTo().performTouchInput { swipeRight() }
        compose.waitUntil(5_000) { runBlocking { data.studyContentRepository.observeAllTasks().first().first { it.id == "gesture-task" }.completed } }
        compose.onNodeWithText("Твій день").assertExists()
        compose.activityRule.scenario.recreate()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Семінар · AB-32", substring = true).fetchSemanticsNodes().isNotEmpty() }
        assertEquals("Science", runBlocking { data.studyPlanRepository.observeCourse("FYZI_6B").first()?.iconKey })
        assertEquals(CourseStatus.ENROLLED, runBlocking { data.studyPlanRepository.observeCourse(fifth.id).first()?.status })
    }

    private fun swipeMenu(right: Boolean) {
        compose.onNodeWithTag("root-content").performTouchInput {
            val y = 5f
            swipe(Offset(width * if(right) .25f else .75f, y), Offset(width * if(right) .75f else .25f, y), 350)
        }
        compose.waitForIdle()
    }
    private fun root(label: String) = compose.onAllNodes(hasText(label) and hasClickAction()).onLast().performClick()
    private fun back() { compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }; compose.waitForIdle() }
    private fun capture(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        android.os.SystemClock.sleep(400)
        val image = instrumentation.uiAutomation.takeScreenshot()
        val suffix = InstrumentationRegistry.getArguments().getString("screenshotSuffix").orEmpty()
        instrumentation.targetContext.openFileOutput("personalization-$name$suffix.png", 0).use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        image.recycle()
    }
}
