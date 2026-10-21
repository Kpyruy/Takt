package com.kpyruy.takt.app

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.kpyruy.takt.core.model.AppLanguage
import com.kpyruy.takt.core.model.GradeItem
import com.kpyruy.takt.core.model.GradeItemType
import com.kpyruy.takt.core.model.StudyTask
import com.kpyruy.takt.core.ui.i18n.TaktI18n
import com.kpyruy.takt.feature.subjects.AddGradeItemForm
import com.kpyruy.takt.feature.subjects.AddTaskForm
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Homework shares the work editor layout without changing persisted task ownership/IDs. */
class HomeworkEditorUiTest {
    @get:Rule val compose = createComposeRule()
    private fun click(tag: String) {
        val node = compose.onNodeWithTag(tag).assertExists()
        runCatching { node.performScrollTo() }
        node.performSemanticsAction(SemanticsActions.OnClick) { assertTrue(it()) }
    }
    @org.junit.After fun resetLanguage() { TaktI18n.use(AppLanguage.UKRAINIAN) }

    @Test fun ordinaryHomeworkUsesCompactDetailsAndSavesWithoutPoints() {
        var saved: StudyTask? = null
        compose.setContent { TaktI18n.use(AppLanguage.ENGLISH); MaterialTheme {
            AddTaskForm("course", modifier = Modifier.verticalScroll(rememberScrollState()), onSave = { saved = it })
        } }
        compose.onNodeWithText("Homework").assertExists()
        compose.onNodeWithTag("grade-details-trigger").assertExists()
        compose.onNode(hasText("Description") and hasSetTextAction()).assertDoesNotExist()
        compose.onNodeWithTag("task-title").performTextInput("Practice set")
        click("grade-details-trigger")
        compose.onNodeWithTag("task-description").performScrollTo().performTextInput("Keep all working")
        click("grade-details-tab-RULES")
        click("task-required")
        click("grade-details-done")
        click("task-save")
        compose.runOnIdle {
            assertEquals("Practice set", saved!!.title)
            assertEquals("Keep all working", saved!!.description)
            assertNull(saved!!.maxPoints)
            assertTrue(saved!!.requiredForExam)
            assertFalse(saved!!.completed)
        }
    }

    @Test fun existingScoredHomeworkRetainsIdentityAndUnsavedResults() {
        val original = StudyTask("existing", "course", "Practice set", "Original description", null,
            true, true, 4.0, 10.0, 5.0)
        var saved: StudyTask? = null
        compose.setContent { TaktI18n.use(AppLanguage.ENGLISH); MaterialTheme {
            AddTaskForm("course", initialTask = original, modifier = Modifier.verticalScroll(rememberScrollState()), onSave = { saved = it })
        } }
        compose.onNodeWithTag("task-maximum").performTextReplacement("12")
        click("grade-details-trigger")
        click("grade-details-tab-RESULT")
        compose.onNodeWithTag("task-earned").performTextReplacement("7")
        click("grade-details-done")
        assertNull(saved)
        click("task-save")
        compose.runOnIdle { assertEquals(original.copy(maxPoints = 12.0, earnedPoints = 7.0), saved) }
    }

    @Test fun seminarIsNotOfferedButHistoricalSeminarRemainsEditable() {
        val original = GradeItem("old-seminar", "course", "Discussion", GradeItemType.SEMINAR, 4.0, 10.0)
        var saved: GradeItem? = null
        compose.setContent { TaktI18n.use(AppLanguage.ENGLISH); MaterialTheme {
            AddGradeItemForm("course", initialItem = original, modifier = Modifier.verticalScroll(rememberScrollState()), onSave = { saved = it })
        } }
        click("grade-type-trigger")
        compose.onNodeWithTag("grade-type-SEMINAR").assertDoesNotExist()
        click("grade-type-HOMEWORK")
        click("grade-save")
        compose.runOnIdle { assertEquals(original.copy(type = GradeItemType.HOMEWORK), saved) }
    }
}
