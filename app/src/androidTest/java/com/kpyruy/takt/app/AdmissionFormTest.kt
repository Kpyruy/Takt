package com.kpyruy.takt.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.kpyruy.takt.core.model.GradeItem
import com.kpyruy.takt.core.model.GradeItemType
import com.kpyruy.takt.core.model.StudyTask
import com.kpyruy.takt.feature.subjects.AddGradeItemForm
import com.kpyruy.takt.feature.subjects.AddTaskForm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AdmissionFormTest {
    @get:Rule val compose = createComposeRule()
    @org.junit.Before fun language() = com.kpyruy.takt.core.ui.i18n.TaktI18n.use(com.kpyruy.takt.core.model.AppLanguage.UKRAINIAN)

    @Test fun testUsesAdmissionCheckboxAndOptionalMinimum() {
        var saved: GradeItem? = null
        compose.setContent {
            MaterialTheme {
                AddGradeItemForm(modifier = Modifier.verticalScroll(rememberScrollState()), courseId = "FYZI_6B", initialType = GradeItemType.TEST, onSave = { saved = it })
            }
        }

        compose.onNodeWithText("Ще можна заробити ці бали").assertDoesNotExist()
        compose.onNodeWithText("Окремо від кількості балів.").assertDoesNotExist()
        compose.onNode(hasText("Назва") and hasSetTextAction()).performScrollTo().performTextInput("Weekly test")
        compose.onNodeWithTag("grade-maximum").performScrollTo().performTextInput("20")
        compose.onNodeWithTag("grade-details-trigger").performScrollTo().performClick()
        compose.onNodeWithTag("grade-details-tab-RULES").performClick()
        compose.onNodeWithTag("grade-required").performClick()
        compose.onNodeWithText("Мінімум").assertExists()
        compose.onNodeWithTag("grade-minimum").performScrollTo().performTextInput("10")
        doneDetails()
        compose.onNodeWithTag("grade-save").performScrollTo().performClick()

        compose.runOnIdle {
            assertEquals(10.0, saved?.minimumPointsForExam)
            assertTrue(saved!!.requiredForExam)
            assertFalse(saved!!.completed)
        }
    }

    @Test fun testDurationIsSavedInMinutesAndRestoredForEditing() {
        var saved: GradeItem? = null
        compose.setContent {
            MaterialTheme {
                AddGradeItemForm(modifier = Modifier.verticalScroll(rememberScrollState()), courseId = "TPAR_6B", initialType = GradeItemType.TEST, onSave = { saved = it })
            }
        }
        compose.onNode(hasText("Назва") and hasSetTextAction()).performScrollTo().performTextInput("Short quiz")
        compose.onNodeWithTag("grade-maximum").performScrollTo().performTextInput("20")
        compose.onNodeWithTag("grade-details-trigger").performScrollTo().performClick()
        compose.onNodeWithTag("grade-details-tab-WHEN").performClick()
        compose.onNodeWithTag("grade-duration").performScrollTo().performTextInput("45")
        doneDetails()
        compose.onNodeWithTag("grade-save").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(45, saved?.durationMinutes) }
    }

    @Test fun ordinaryHomeworkCanSaveWithoutPointsFromTheSharedDetailsLayout() {
        var saved: StudyTask? = null
        compose.setContent {
            com.kpyruy.takt.core.ui.i18n.TaktI18n.use(com.kpyruy.takt.core.model.AppLanguage.UKRAINIAN)
            MaterialTheme { AddTaskForm(modifier = Modifier.verticalScroll(rememberScrollState()), courseId = "FYZI_6B", onSave = { saved = it }) }
        }
        compose.onNodeWithTag("task-title").performTextInput("Lab report")
        compose.onNodeWithTag("task-maximum").assertExists()
        compose.onNodeWithTag("grade-details-trigger").performScrollTo().performClick()
        compose.onNodeWithTag("grade-details-tab-RULES").performClick()
        compose.onNodeWithTag("task-required").performClick()
        doneDetails()
        compose.onNodeWithTag("task-save").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(null, saved?.earnedPoints)
            assertEquals(null, saved?.maxPoints)
            assertTrue(saved!!.requiredForExam)
            assertFalse(saved!!.completed)
            assertFalse(saved!!.meetsAdmissionRequirement)
        }
    }

    @Test fun legacyScoredTaskKeepsEditablePoints() {
        val task = StudyTask("legacy", "FYZI_6B", "Lab report", null, null, true,
            earnedPoints = 4.0, maxPoints = 10.0, minimumPointsForExam = 5.0)
        var saved: StudyTask? = null
        compose.setContent {
            MaterialTheme { AddTaskForm(modifier = Modifier.verticalScroll(rememberScrollState()), courseId = "FYZI_6B", initialTask = task, onSave = { saved = it }) }
        }
        compose.onNodeWithText("Максимум").assertExists()
        compose.onNodeWithText("Оновити").performClick()
        compose.runOnIdle { assertEquals(10.0, saved?.maxPoints) }
    }

    private fun doneDetails() {
        compose.onNodeWithTag("grade-details-done").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("grade-details").fetchSemanticsNodes().isEmpty() }
        compose.waitForIdle()
    }
}
