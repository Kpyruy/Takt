package com.kpyruy.takt.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.kpyruy.takt.core.model.Course
import com.kpyruy.takt.core.model.CourseStatus
import com.kpyruy.takt.core.ui.components.CourseLinkSelector
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ActiveCourseChoicesTest {
    @get:Rule val compose = createComposeRule()

    private val courses = listOf(
        Course("done", "DONE", "Completed course", 5, 1, CourseStatus.FULFILLED),
        Course("active", "ACTIVE", "Active course", 5, 3, CourseStatus.ENROLLED),
        Course("planned", "PLAN", "Planned course", 5, 4, CourseStatus.PLANNED),
    )

    @Test fun fullFormChoiceShowsOnlyActiveCourses() {
        var selected: String? = null
        compose.setContent {
            MaterialTheme {
                SelectCourseSheet(courses, onDismiss = {}, onSelected = { selected = it.id })
            }
        }

        compose.onNodeWithText("Active course").assertExists().performClick()
        compose.onNodeWithText("Completed course").assertDoesNotExist()
        compose.onNodeWithText("Planned course").assertDoesNotExist()
        compose.runOnIdle { assertEquals("active", selected) }
    }

    @Test fun lessonLinkChoiceShowsOnlyActiveCoursesAndKeepsExistingLabel() {
        compose.setContent {
            MaterialTheme {
                CourseLinkSelector(courses, selectedId = "done", onChange = {})
            }
        }

        compose.onNodeWithText("Completed course").performClick()
        compose.onAllNodesWithText("Completed course").assertCountEquals(1)
        compose.onNodeWithText("Active course").assertExists()
        compose.onNodeWithText("Planned course").assertDoesNotExist()
    }

    @Test fun quickAddChipsShowOnlyActiveCourses() {
        compose.setContent {
            MaterialTheme {
                QuickAddSheet(
                    courses = courses,
                    onDismiss = {},
                    onSaveTask = {},
                    onSaveLesson = {},
                    onSaveNote = {},
                    onOpenFull = {},
                )
            }
        }

        compose.onNodeWithText("Active course").assertExists()
        compose.onNodeWithText("Completed course").assertDoesNotExist()
        compose.onNodeWithText("Planned course").assertDoesNotExist()
    }
}
