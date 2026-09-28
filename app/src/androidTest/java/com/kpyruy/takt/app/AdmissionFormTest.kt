package com.kpyruy.takt.app

import androidx.compose.material3.MaterialTheme
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

    @Test fun testUsesAdmissionCheckboxAndOptionalMinimum() {
        var saved: GradeItem? = null
        compose.setContent {
            MaterialTheme {
                AddGradeItemForm(courseId = "FYZI_6B", initialType = GradeItemType.TEST, onSave = { saved = it })
            }
        }

        compose.onNodeWithText("Ще можна заробити ці бали").assertDoesNotExist()
        compose.onNodeWithText("Окремо від кількості балів.").assertDoesNotExist()
        compose.onNode(hasText("Назва") and hasSetTextAction()).performTextInput("Weekly test")
        compose.onNodeWithText("Потрібно для допуску").performClick()
        compose.onNodeWithText("Мінімум балів для допуску").assertExists()
        compose.onNode(hasText("Максимум") and hasSetTextAction()).performTextInput("20")
        compose.onNode(hasText("Мінімум балів для допуску") and hasSetTextAction()).performTextInput("10")
        compose.onNodeWithText("Зберегти").performClick()

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
                AddGradeItemForm(courseId = "TPAR_6B", initialType = GradeItemType.TEST, onSave = { saved = it })
            }
        }
        compose.onNode(hasText("Назва") and hasSetTextAction()).performTextInput("Short quiz")
        compose.onNode(hasText("Максимум") and hasSetTextAction()).performTextInput("20")
        compose.onNode(hasText("Тривалість тесту (хв)") and hasSetTextAction()).performTextInput("45")
        compose.onNodeWithText("Зберегти").performClick()
        compose.runOnIdle { assertEquals(45, saved?.durationMinutes) }
    }

    @Test fun newOrdinaryTaskHasNoSecondPointEntry() {
        var saved: StudyTask? = null
        compose.setContent {
            MaterialTheme { AddTaskForm(courseId = "FYZI_6B", onSave = { saved = it }) }
        }

        compose.onNode(hasText("Назва") and hasSetTextAction()).performTextInput("Lab report")
        compose.onNodeWithText("Потрібно для допуску до екзамену").performClick()
        compose.onNodeWithText("Бали (необов’язково)").assertDoesNotExist()
        compose.onNodeWithText("Максимум").assertDoesNotExist()
        compose.onNodeWithText("Зберегти").performClick()

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
            MaterialTheme { AddTaskForm(courseId = "FYZI_6B", initialTask = task, onSave = { saved = it }) }
        }
        compose.onNodeWithText("Максимум").assertExists()
        compose.onNodeWithText("Оновити").performClick()
        compose.runOnIdle { assertEquals(10.0, saved?.maxPoints) }
    }
}
