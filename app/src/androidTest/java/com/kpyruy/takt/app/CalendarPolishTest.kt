package com.kpyruy.takt.app

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.kpyruy.takt.core.model.*
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CalendarPolishTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun calendarWeekStripPagesInBothDayAndWeekViews() {
        val today = LocalDate.now()
        compose.onNodeWithText("Календар").performClick()
        compose.onNodeWithTag("calendar-week-strip").performTouchInput { swipeLeft() }
        waitForSelectedDay(today.plusWeeks(1))
        compose.onNode(hasText("Тиждень") and hasClickAction()).performClick()
        compose.onNodeWithTag("calendar-week-strip").performTouchInput { swipeLeft() }
        waitForSelectedDay(today.plusWeeks(2))
    }

    @Test fun monthShowsIndependentLessonWorkAndExamMarkers() {
        val data = (compose.activity.application as TaktApplication).dataContainer
        val today = LocalDate.now()
        val lessonId = "month-marker-lesson"
        val taskId = "month-marker-task"
        val examId = "month-marker-exam"
        runBlocking {
            data.scheduleRepository.upsertOneOffEvent(OneOffScheduleEvent(
                lessonId, "TPAR_6B", "Marker seminar", today,
                LocalTime.of(10, 0), LocalTime.of(11, 0), type = OneOffScheduleEventType.EXTRA,
                lessonType = LessonType.SEMINAR,
            ))
            data.studyContentRepository.upsertTask(StudyTask(taskId, "TPAR_6B", "Marker task", null, today, false))
            data.gradeRepository.upsertItem(GradeItem(examId, "TPAR_6B", "Marker exam",
                GradeItemType.EXAM, 0.0, 40.0, dueDate = today, completed = false))
        }
        try {
            compose.onNodeWithText("Календар").performClick()
            compose.onNode(hasText("Місяць") and hasClickAction()).performClick()
            val epoch = today.toEpochDay()
            compose.onNodeWithTag("month-marker-lesson-$epoch", useUnmergedTree = true).assertExists()
            compose.onNodeWithTag("month-marker-work-$epoch", useUnmergedTree = true).assertExists()
            compose.onNodeWithTag("month-marker-exam-$epoch", useUnmergedTree = true).assertExists()
            compose.onNodeWithText("Пари").assertExists()
            compose.onNodeWithText("Задачі").assertExists()
            compose.onNodeWithText("Іспити").assertExists()
        } finally {
            runBlocking {
                data.scheduleRepository.deleteOneOffEvent(lessonId)
                data.studyContentRepository.deleteTask(taskId)
                data.gradeRepository.deleteItem(examId)
            }
        }
    }

    @Test fun homePeriodSelectionSurvivesLeavingHomeAndProgressHasNoNearestStep() {
        val data = (compose.activity.application as TaktApplication).dataContainer
        try {
            compose.onNodeWithTag("home-tasks-filter").performClick()
            compose.onNodeWithText("7 днів").performClick()
            compose.onNodeWithText("Готово").performScrollTo().performClick()
            compose.onNodeWithText("Найближчі 7 днів").assertExists()
            assertEquals(HomeWorkPeriod.SEVEN_DAYS,
                runBlocking { data.settingsRepository.settings.first().homeWorkFilter.period })
            compose.onNodeWithText("Прогрес").performClick()
            compose.onNodeWithText("Найближчий крок").assertDoesNotExist()
            compose.onNodeWithText("Сьогодні").performClick()
            compose.onNodeWithText("Найближчі 7 днів").assertExists()
        } finally {
            runBlocking { data.settingsRepository.setHomeWorkFilter(HomeWorkFilter()) }
        }
    }

    private fun waitForSelectedDay(date: LocalDate) {
        compose.waitUntil(6_000) {
            compose.onAllNodesWithTag("calendar-day-${date.toEpochDay()}")
                .fetchSemanticsNodes().any {
                    runCatching { it.config[SemanticsProperties.Selected] }.getOrDefault(false)
                }
        }
    }
}
