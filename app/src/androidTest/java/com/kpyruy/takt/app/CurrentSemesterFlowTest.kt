package com.kpyruy.takt.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals

/** Changes settings only on a disposable review emulator. */
class CurrentSemesterFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun restoringDocumentsBackupKeepsDetectedUisSemester() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("semesterReview") == "true")
        val data = (compose.activity.application as TaktApplication).dataContainer
        runBlocking {
            data.seedIfNeeded()
            data.settingsRepository.setCurrentSemester(null)
            data.settingsRepository.setUisCurrentSemester(3)
            val backup = data.backupRepository.exportJson()
            data.backupRepository.importJson(backup)
            assertEquals(3, data.settingsRepository.settings.value.uisCurrentSemester)
            assertEquals(3, data.settingsRepository.settings.value.effectiveCurrentSemester(
                data.studyPlanRepository.observeCourses().first()))
        }
    }

    @Test fun oneCurrentSemesterWithRetakesFromOtherTerms() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("semesterReview") == "true")
        val data = (compose.activity.application as TaktApplication).dataContainer
        runBlocking {
            data.seedIfNeeded()
            data.settingsRepository.setCurrentSemester(null)
            data.settingsRepository.setUisCurrentSemester(3)
        }

        compose.onAllNodes(hasText("Прогрес") and hasClickAction()).onLast().performClick()
        compose.onNodeWithText("Поточний семестр · 3").assertExists()
        compose.onNodeWithTag("current-semester-selector").performClick()
        compose.onNodeWithTag("select-current-semester-5").performClick()
        compose.waitUntil(5_000) { runBlocking { data.settingsRepository.settings.first().currentSemester == 5 } }

        compose.onNodeWithTag("progress-screen").performScrollToIndex(5)
        compose.onNodeWithTag("semester-5-active").assertExists()
        compose.onNodeWithTag("progress-screen").performScrollToIndex(3)
        compose.onNodeWithTag("semester-3-future").assertExists()
        compose.onNodeWithText("Поточний семестр · 5").assertExists()
        compose.onNodeWithTag("progress-screen")
            .performScrollToNode(hasTestTag("current-semester-selector"))
        compose.onNodeWithTag("current-semester-selector").performClick()
        compose.onNodeWithTag("select-current-semester-auto").performClick()
        compose.waitUntil(5_000) { data.settingsRepository.settings.value.currentSemester == null }
        compose.onNodeWithText("Поточний семестр · 3").assertExists()
    }
}
