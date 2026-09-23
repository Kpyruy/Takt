package com.kpyruy.takt.app

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.kpyruy.takt.core.model.*
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/** Opt-in fixture writes are intended only for the disposable review AVD. */
class PulseFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun selectedDesignsUseRealDataAndActions() {
        assumeTrue("Use -e pulseReview true on a disposable review emulator",
            InstrumentationRegistry.getArguments().getString("pulseReview") == "true")
        val data = (compose.activity.application as TaktApplication).dataContainer
        val courseId = "FYZI_6B"
        val dark = InstrumentationRegistry.getArguments().getString("dark") == "true"
        runBlocking {
            data.seedIfNeeded()
            data.settingsRepository.setThemeFamily(ThemeFamily.BLUE)
            data.settingsRepository.setThemeMode(if (dark) AppThemeMode.DARK else AppThemeMode.LIGHT)
            data.studyPlanRepository.setGradingType(courseId, CourseGradingType.EXAM_LETTER)
            data.studyPlanRepository.observeCourses().first().filter { it.semester <= 2 }.forEach {
                data.studyPlanRepository.updateStatus(it.id, CourseStatus.FULFILLED)
            }
            // Remove prior review schedule entries before recreating the deterministic day.
            data.scheduleRepository.observeRules().first().forEach { data.scheduleRepository.deleteRule(it.id) }
            data.scheduleRepository.observeOneOffEvents().first().forEach { data.scheduleRepository.deleteOneOffEvent(it.id) }
            listOf(
                ScheduleRule("review-math", "MATM2_6B", "Matematika II", LocalDate.now().dayOfWeek, LocalTime.of(8,0), LocalTime.of(9,30), ScheduleRecurrence.WEEKLY, "BC-11"),
                ScheduleRule("review-physics", courseId, "Fyzika", LocalDate.now().dayOfWeek, LocalTime.of(10,30), LocalTime.of(12,0), ScheduleRecurrence.WEEKLY, "AB-32"),
                ScheduleRule("review-tpar", "TPAR_6B", "Technické prostriedky", LocalDate.now().dayOfWeek, LocalTime.of(13,15), LocalTime.of(14,45), ScheduleRecurrence.WEEKLY, "CD-08"),
            ).forEach { data.scheduleRepository.upsertRule(it) }
            data.studyContentRepository.observeAllTasks().first().forEach { data.studyContentRepository.deleteTask(it.id) }
            repeat(5) { i -> data.studyContentRepository.upsertTask(StudyTask(
                "pulse-test-task-$i", courseId, when(i) { 4 -> "Лабораторна №2"; 0 -> "Лабораторна №1"; 1 -> "Контрольна робота"; else -> "Практична робота ${i + 1}" },
                if (i == 4) "Здати звіт викладачу" else null, if (i == 4) LocalDate.now() else null,
                completed = i < 4, requiredForExam = true,
            )) }
            data.studyContentRepository.upsertTask(StudyTask("review-untimed", courseId, "Повторити рівняння руху", null, null, false))
            data.gradeRepository.observeRecentItems(Int.MAX_VALUE).first().forEach { data.gradeRepository.deleteItem(it.id) }
            data.gradeRepository.upsertItem(GradeItem("pulse-test-coursework", courseId, "Робота за семестр", GradeItemType.TEST, 24.0, 30.0))
            data.gradeRepository.upsertItem(GradeItem("pulse-test-exam", courseId, "Підсумковий іспит", GradeItemType.EXAM, 0.0, 70.0, completed = false))
            data.gradeRepository.upsertItem(GradeItem("review-tpar-points", "TPAR_6B", "Семестрові роботи", GradeItemType.TEST, 18.0, 20.0))
            data.gradeRepository.upsertItem(GradeItem("review-stats-points", "ZAST_6B", "Практичні роботи", GradeItemType.TEST, 16.0, 20.0))
            data.gradeRepository.upsertItem(GradeItem("review-sport-points", "TEVE1_6B", "За семестр", GradeItemType.TEST, 8.0, 10.0))
            data.examRepository.upsertExamInfo(ExamInfo(courseId, "pulse-test-exam", LocalDate.of(2027,1,22), LocalTime.of(9,0), null, "AB-32"))
        }
        compose.waitUntil(15_000) { compose.onAllNodesWithText("Fyzika").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Твій день").assertIsDisplayed()
        capture("home")
        clickRoot("Календар")
        compose.onNode(hasText("День") and hasClickAction()).assertIsDisplayed()
        capture("calendar")
        clickRoot("Предмети")
        compose.onNodeWithText("Знайти предмет").assertIsDisplayed()
        capture("subjects")
        compose.onNode(hasText("Fyzika") and hasClickAction()).performScrollTo().performClick()
        compose.onNodeWithText("Набрано за семестр").assertIsDisplayed()
        compose.onNodeWithText("24").assertIsDisplayed()
        capture("course")
        compose.onNode(hasText("Екзамен") and hasClickAction()).performScrollTo().performClick()
        compose.onNodeWithText("68").assertExists()
        capture("exam")
        compose.onAllNodes(hasText("B") and hasClickAction()).onFirst().performScrollTo().performClick()
        compose.onNodeWithText("59").assertExists()
        compose.onAllNodes(hasText("A") and hasClickAction()).onFirst().performClick()
        compose.onNodeWithText("68").assertExists()
        // Back returns from the exam to the subject before leaving the subject.
        back()
        compose.onNodeWithText("Набрано за семестр").assertExists()
        back()
        clickRoot("Прогрес")
        compose.onNodeWithTag("semester-1-completed").assertExists()
        compose.onNodeWithTag("semester-3-active").assertExists()
        capture("progress")
        compose.onNode(hasText("Допуск з Fyzika") and hasClickAction()).performScrollTo().performClick()
        compose.onNode(hasText("Завдання") and isSelected()).assertExists()
        compose.onNodeWithText("Лабораторна №2").assertExists()
        // Completion changes admission, not semester points.
        compose.onNodeWithContentDescription("Виконано: Лабораторна №2").performScrollTo().performClick()
        compose.onNode(hasText("Огляд") and hasClickAction()).performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Умови допуску виконано").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("24").assertExists()
        back()
        // An earlier required assessment must outrank a later task in the journey.
        runBlocking {
            data.studyContentRepository.upsertTask(StudyTask("review-later", courseId, "Пізніше завдання", null, LocalDate.now().plusDays(10), false, true))
            data.gradeRepository.upsertItem(GradeItem("review-required-assessment", "TPAR_6B", "Обов’язковий тест", GradeItemType.TEST, 0.0, 10.0, dueDate = LocalDate.now(), completed = false, requiredForExam = true))
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Допуск з Technické prostriedky automatizovaného riadenia").fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasText("Допуск з Technické prostriedky automatizovaného riadenia") and hasClickAction()).performScrollTo().performClick()
        compose.onNode(hasText("Бали") and isSelected()).assertExists()
        back()
        // A grade-only admission blocker opens grade entry instead of an empty task list.
        runBlocking {
            data.studyContentRepository.setTaskCompleted("review-later", true)
            data.gradeRepository.upsertItem(GradeItem("review-physics-required", courseId, "Допусковий тест", GradeItemType.TEST, 0.0, 10.0, completed = false, requiredForExam = true))
        }
        clickRoot("Предмети")
        compose.onNode(hasText("Fyzika") and hasClickAction()).performScrollTo().performClick()
        compose.onNode(hasText("Екзамен") and hasClickAction()).performScrollTo().performClick()
        compose.onNodeWithTag("exam-admission").performScrollTo().performClick()
        compose.onNode(hasText("Бали") and isSelected()).assertExists()
        back()
        clickRoot("Сьогодні")
        compose.onNodeWithContentDescription("Додати або налаштувати").performClick()
        compose.onNodeWithText("Усі способи додавання").performClick()
        compose.onNodeWithText("Швидко").assertExists()
    }

    private fun clickRoot(label: String) = compose.onAllNodes(hasText(label) and hasClickAction()).onLast().performClick()
    private fun back() { compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }; compose.waitForIdle() }
    private fun capture(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        android.os.SystemClock.sleep(500)
        val image = instrumentation.uiAutomation.takeScreenshot()
        val suffix = InstrumentationRegistry.getArguments().getString("screenshotSuffix").orEmpty()
        instrumentation.targetContext.openFileOutput("selected-$name$suffix.png", 0).use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        image.recycle()
    }
}
