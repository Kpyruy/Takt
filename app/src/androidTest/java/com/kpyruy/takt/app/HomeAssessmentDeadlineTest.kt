package com.kpyruy.takt.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.kpyruy.takt.core.model.GradeItem
import com.kpyruy.takt.core.model.GradeItemType
import com.kpyruy.takt.core.model.ScheduleRecurrence
import com.kpyruy.takt.core.model.ScheduleRule
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class HomeAssessmentDeadlineTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun datedTestAppearsInUnifiedTasksAndOpensCourseTasks() {
        val repository = (compose.activity.application as TaktApplication).dataContainer.gradeRepository
        val monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val friday = monday.plusDays(4)
        val id = "home-friday-test"
        runBlocking {
            repository.upsertItem(GradeItem(
                id = id,
                courseId = "FYZI_6B",
                title = "Friday entry test",
                type = GradeItemType.TEST,
                earnedPoints = 0.0,
                maxPoints = 20.0,
                dueDate = friday,
                completed = false,
            ))
        }

        try {
            compose.onNodeWithTag("home-tasks-filter").performClick()
            compose.onNodeWithText("Усі").performClick()
            compose.onNodeWithText("Готово").performClick()
            compose.onNodeWithText("Завдання на день").assertDoesNotExist()
            compose.onNodeWithText("Без дати").assertDoesNotExist()
            compose.onNodeWithTag("home-assessment-$id").assertIsDisplayed().performClick()
            compose.onNodeWithText("Задачі").assertIsSelected()
            compose.onNodeWithTag("course-work-$id").assertIsDisplayed()
        } finally {
            runBlocking { repository.deleteItem(id) }
        }
    }

    @Test fun datedTestIsShownInUnifiedTasksBelowLessonsWithCourseCode() {
        val repository = (compose.activity.application as TaktApplication).dataContainer.gradeRepository
        val id = "home-task-placement-test"
        runBlocking {
            repository.upsertItem(GradeItem(
                id = id, courseId = "TPAR_6B", title = "Placement quiz",
                type = GradeItemType.TEST, earnedPoints = 0.0, maxPoints = 20.0,
                dueDate = LocalDate.now(), completed = false, durationMinutes = 45,
            ))
        }
        try {
            val section = compose.onNodeWithText("Задачі")
            val assessment = compose.onNodeWithTag("home-assessment-$id")
            section.assertIsDisplayed()
            assessment.assertIsDisplayed()
            compose.onNodeWithText("TPAR_6B · 45 хв").assertIsDisplayed()
            compose.onNodeWithText("Завдання на день").assertDoesNotExist()
            org.junit.Assert.assertTrue(
                assessment.getUnclippedBoundsInRoot().top > section.getUnclippedBoundsInRoot().top
            )
        } finally {
            runBlocking { repository.deleteItem(id) }
        }
    }

    @Test fun ordinaryTaskAndTestAreBothInTasksAndTypeFilterWorks() {
        val container = (compose.activity.application as TaktApplication).dataContainer
        val date = LocalDate.now().plusDays(1)
        val taskId = "home-unified-task"
        val testId = "home-unified-test"
        runBlocking {
            container.studyContentRepository.upsertTask(com.kpyruy.takt.core.model.StudyTask(
                taskId, "TPAR_6B", "Unified homework", null, date, false,
            ))
            container.gradeRepository.upsertItem(GradeItem(
                testId, "TPAR_6B", "Unified quiz", GradeItemType.TEST,
                0.0, 10.0, dueDate = date, completed = false,
            ))
        }
        try {
            compose.onNodeWithTag("home-tasks-filter").performClick()
            compose.onNodeWithText("Усі").performClick()
            compose.onNodeWithText("Готово").performClick()
            compose.onNodeWithTag("home-task-$taskId").assertIsDisplayed()
            compose.onNodeWithTag("home-assessment-$testId").assertIsDisplayed()
            compose.onNodeWithTag("home-tasks-filter").performClick()
            compose.onNodeWithText("Тест").performScrollTo().performClick()
            compose.onNodeWithText("Тест").assertIsSelected()
            compose.onNodeWithText("Готово").performClick()
            compose.onNodeWithTag("home-task-$taskId").assertDoesNotExist()
            compose.onNodeWithTag("home-assessment-$testId").assertIsDisplayed()
        } finally {
            runBlocking {
                container.studyContentRepository.deleteTask(taskId)
                container.gradeRepository.deleteItem(testId)
            }
        }
    }

    @Test fun completedAssessmentStillAppearsInCalendarOnItsDate() {
        val repository = (compose.activity.application as TaktApplication).dataContainer.gradeRepository
        val id = "calendar-today-test"
        runBlocking {
            repository.upsertItem(GradeItem(
                id = id,
                courseId = "FYZI_6B",
                title = "Calendar assessment",
                type = GradeItemType.TEST,
                earnedPoints = 12.0,
                maxPoints = 20.0,
                dueDate = LocalDate.now(),
                completed = true,
            ))
        }

        try {
            compose.onNodeWithText("Календар").performClick()
            compose.onNodeWithTag("calendar-assessment-$id").assertIsDisplayed().performClick()
            compose.onNodeWithText("Задачі").assertIsSelected()
            compose.onNodeWithTag("course-work-$id").assertIsDisplayed()
        } finally {
            runBlocking { repository.deleteItem(id) }
        }
    }

    @Test fun examDeadlineOpensCourseTasks() {
        val repository = (compose.activity.application as TaktApplication).dataContainer.gradeRepository
        val id = "calendar-exam-route"
        runBlocking {
            repository.upsertItem(GradeItem(
                id = id, courseId = "FYZI_6B", title = "Exam deadline",
                type = GradeItemType.EXAM, earnedPoints = 0.0, maxPoints = 40.0,
                dueDate = LocalDate.now(), completed = false,
            ))
        }
        try {
            compose.onNodeWithText("Календар").performClick()
            compose.onNodeWithTag("calendar-assessment-$id").assertIsDisplayed().performClick()
            compose.onNodeWithText("Задачі").assertIsSelected()
            compose.onNodeWithTag("course-work-$id").assertIsDisplayed()
        } finally {
            runBlocking { repository.deleteItem(id) }
        }
    }

    @Test fun testIconAppearsOnMatchingLessonOnlyOnTestDate() {
        val container = (compose.activity.application as TaktApplication).dataContainer
        val friday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusDays(4)
        val ruleId = "home-test-lesson"
        val testId = "home-test-marker"
        runBlocking {
            container.scheduleRepository.upsertRule(ScheduleRule(
                id = ruleId, courseId = "FYZI_6B", title = "Physics review",
                dayOfWeek = DayOfWeek.FRIDAY, startTime = LocalTime.of(10, 0),
                endTime = LocalTime.of(11, 30), recurrence = ScheduleRecurrence.WEEKLY,
            ))
            container.gradeRepository.upsertItem(GradeItem(
                id = testId, courseId = "FYZI_6B", title = "Friday quiz",
                type = GradeItemType.TEST, earnedPoints = 0.0, maxPoints = 20.0,
                dueDate = friday, completed = false, lessonId = ruleId,
            ))
        }

        try {
            compose.onNodeWithTag("home-day-${friday.toEpochDay()}").performClick()
            compose.onAllNodesWithContentDescription("Тест на цій парі", useUnmergedTree = true)
                .assertCountEquals(1)
            compose.onNodeWithTag("home-week-strip").performTouchInput { swipeLeft() }
            compose.onAllNodesWithContentDescription("Тест на цій парі", useUnmergedTree = true)
                .assertCountEquals(0)
        } finally {
            runBlocking {
                container.gradeRepository.deleteItem(testId)
                container.scheduleRepository.deleteRule(ruleId)
            }
        }
    }
}
