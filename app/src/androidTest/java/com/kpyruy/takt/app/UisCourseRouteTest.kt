package com.kpyruy.takt.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.kpyruy.takt.core.model.Course
import com.kpyruy.takt.core.model.CourseStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Exercises the active navigation entry and its restored arguments after adopting an offline course. */
class UisCourseRouteTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun openSubjectRemainsAvailableAfterIdMigrationAndActivityRecreation() {
        val data = (compose.activity.application as TaktApplication).dataContainer
        val suffix = System.currentTimeMillis().toString()
        val title = "Перенесення UIS $suffix"
        val code = "MIG_$suffix"
        val oldId = runBlocking { data.studyPlanRepository.addCourse(title, code, 1, 1) }
        compose.onAllNodes(hasText("Предмети") and hasClickAction()).onLast().performClick()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(title))
        compose.onNodeWithText(title).assertExists()
        compose.onNode(hasText(title) and hasClickAction()).performScrollTo().performClick()
        val canonicalId = "uis:990:$suffix"
        val remoteTitle = "UIS · $title"
        runBlocking {
            data.studyPlanRepository.upsertImportedCourses(listOf(Course(canonicalId, code, remoteTitle, 5, 1, CourseStatus.ENROLLED)))
            assertEquals(canonicalId, data.studyPlanRepository.observeCourse(oldId).first()?.id)
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithText(remoteTitle).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Предмет не знайдено").assertDoesNotExist()
        compose.activityRule.scenario.recreate()
        compose.waitUntil(10_000) { compose.onAllNodesWithText(remoteTitle).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Предмет не знайдено").assertDoesNotExist()
    }
    @Test fun openNoteDraftAndTabSurviveMigrationAndRecreation() {
        val data = (compose.activity.application as TaktApplication).dataContainer
        val suffix = System.currentTimeMillis().toString()
        val title = "Чернетка UIS $suffix"
        val code = "DRF_$suffix"
        val oldId = runBlocking {
            data.settingsRepository.setLanguage(com.kpyruy.takt.core.model.AppLanguage.UKRAINIAN)
            data.studyPlanRepository.addCourse(title, code, 1, 1)
        }
        compose.onAllNodes(hasText("Предмети") and hasClickAction()).onLast().performClick()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(title))
        compose.onNodeWithText(title).assertExists()
        compose.onNode(hasText(title) and hasClickAction()).performScrollTo().performClick()
        compose.onNode(hasText("Нотатки") and hasClickAction() and SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.Role, androidx.compose.ui.semantics.Role.Tab)).performClick()
        compose.onNodeWithText("Додати нотатку").performScrollTo().performClick()
        val draftTitle = "Особиста нотатка $suffix"
        compose.onNode(hasText("Заголовок") and hasSetTextAction()).performTextInput(draftTitle)
        compose.onNode(hasText("Нотатка") and hasSetTextAction()).performTextInput("Незбережений текст")
        val canonicalId = "uis:991:$suffix"
        runBlocking {
            data.studyPlanRepository.upsertImportedCourses(listOf(Course(canonicalId, code, "UIS · $title", 5, 1, CourseStatus.ENROLLED)))
            assertEquals(canonicalId, data.studyPlanRepository.observeCourse(oldId).first()?.id)
        }
        compose.waitForIdle()
        compose.onNode(hasText("Додати нотатку") and !hasClickAction()).assertExists()
        compose.onNode(hasText("Заголовок") and hasSetTextAction()).assertTextContains(draftTitle)
        compose.onNode(hasText("Нотатка") and hasSetTextAction()).assertTextContains("Незбережений текст")
        compose.activityRule.scenario.recreate()
        compose.waitUntil(10_000) { compose.onAllNodes(hasText("Додати нотатку") and !hasClickAction()).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasText("Заголовок") and hasSetTextAction()).assertTextContains(draftTitle)
        compose.onNode(hasText("Нотатка") and hasSetTextAction()).assertTextContains("Незбережений текст")
        compose.onNodeWithText("Зберегти").performScrollTo().performClick()
        compose.waitUntil(5_000) {
            runBlocking { data.studyContentRepository.observeNotes(canonicalId).first().count { it.title == draftTitle } == 1 }
        }
        compose.onNodeWithText("Додати нотатку").assertExists()
        assertEquals("Незбережений текст", runBlocking { data.studyContentRepository.observeNotes(canonicalId).first().single { it.title == draftTitle }.content })
    }
    @Test fun createNoteOpenedBeforeMigrationSavesUnderCanonicalId() {
        val data = (compose.activity.application as TaktApplication).dataContainer
        val suffix = System.currentTimeMillis().toString()
        val title = "Створення UIS $suffix"
        val code = "CRT_$suffix"
        val oldId = runBlocking {
            data.settingsRepository.setLanguage(com.kpyruy.takt.core.model.AppLanguage.UKRAINIAN)
            data.studyPlanRepository.addCourse(title, code, 1, 1)
        }
        compose.onNodeWithTag("root-add").performClick()
        compose.onNode(hasText("Нотатка") and hasClickAction()).performClick()
        compose.onNode(hasText(title) and hasClickAction()).performScrollTo().performClick()
        val draftTitle = "Нотатка з глобального меню $suffix"
        compose.onNode(hasText("Заголовок") and hasSetTextAction()).performTextInput(draftTitle)
        compose.onNode(hasText("Нотатка") and hasSetTextAction()).performTextInput("Текст перед перенесенням ID")
        val canonicalId = "uis:992:$suffix"
        runBlocking {
            data.studyPlanRepository.upsertImportedCourses(listOf(Course(canonicalId, code, "UIS · $title", 5, 1, CourseStatus.ENROLLED)))
            assertEquals(canonicalId, data.studyPlanRepository.observeCourse(oldId).first()?.id)
        }
        compose.waitForIdle()
        compose.onNode(hasText("Заголовок") and hasSetTextAction()).assertTextContains(draftTitle)
        compose.onNode(hasText("Нотатка") and hasSetTextAction()).assertTextContains("Текст перед перенесенням ID")
        compose.activityRule.scenario.recreate()
        compose.waitUntil(10_000) { compose.onAllNodes(hasText("Заголовок") and hasSetTextAction()).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasText("Заголовок") and hasSetTextAction()).assertTextContains(draftTitle)
        compose.onNodeWithText("Зберегти").performScrollTo().performClick()
        compose.waitUntil(5_000) { runBlocking { data.studyContentRepository.observeNotes(canonicalId).first().count { it.title == draftTitle } == 1 } }
        assertEquals(canonicalId, runBlocking { data.database.studyContentDao().getNotesSnapshot().single { it.title == draftTitle }.courseId })
        assertEquals("Текст перед перенесенням ID", runBlocking { data.studyContentRepository.observeNotes(canonicalId).first().single { it.title == draftTitle }.content })
    }
}
