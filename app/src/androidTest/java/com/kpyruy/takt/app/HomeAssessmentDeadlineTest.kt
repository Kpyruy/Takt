package com.kpyruy.takt.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.kpyruy.takt.core.model.GradeItem
import com.kpyruy.takt.core.model.GradeItemType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class HomeAssessmentDeadlineTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun testAppearsOnItsFridayAndOpensCourseGrades() {
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
            compose.onNodeWithText("Бали").assertIsSelected()
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
            compose.onNodeWithText("Бали").assertIsSelected()
        } finally {
            runBlocking { repository.deleteItem(id) }
        }
    }
}
