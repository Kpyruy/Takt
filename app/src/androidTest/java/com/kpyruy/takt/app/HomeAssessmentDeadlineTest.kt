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

    @Test fun testAppearsOnItsFridayAndOpensCourseTasks() {
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
            compose.onNodeWithTag("home-day-${monday.toEpochDay()}").performClick()
            compose.onNodeWithTag("home-assessment-$id").assertDoesNotExist()
            compose.onNodeWithTag("home-day-${friday.toEpochDay()}").performClick()
            compose.onNodeWithTag("home-assessment-$id").assertIsDisplayed().performClick()
            compose.onNodeWithText("Задачі").assertIsSelected()
            compose.onNodeWithTag("course-work-$id").assertIsDisplayed()
        } finally {
            runBlocking { repository.deleteItem(id) }
        }
    }

    @Test fun datedTestIsShownInDayTasksBelowLessonsWithCourseCode() {
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
            val section = compose.onNodeWithText("Завдання на день")
            val assessment = compose.onNodeWithTag("home-assessment-$id")
            section.assertIsDisplayed()
            assessment.assertIsDisplayed()
            compose.onNodeWithText("TPAR_6B · 45 хв").assertIsDisplayed()
            org.junit.Assert.assertTrue(
                assessment.getUnclippedBoundsInRoot().top > section.getUnclippedBoundsInRoot().top
            )
        } finally {
            runBlocking { repository.deleteItem(id) }
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

    @Test fun examDeadlineStillOpensGrades() {
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
            compose.onNodeWithText("Бали").assertIsSelected()
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
