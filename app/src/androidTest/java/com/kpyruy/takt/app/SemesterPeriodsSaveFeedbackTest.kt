package com.kpyruy.takt.app

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.kpyruy.takt.core.model.AssessmentPhaseMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/** Opt-in save-flow check on a disposable emulator. */
class SemesterPeriodsSaveFeedbackTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun savedPeriodsShowConfirmation() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("periodsReview") == "true")
        val data = (compose.activity.application as TaktApplication).dataContainer
        runBlocking {
            data.seedIfNeeded()
            data.settingsRepository.setSemesterPeriods(emptyMap())
        }

        compose.onNodeWithContentDescription("Налаштування").performClick()
        compose.onNodeWithText("Періоди навчання").performClick()
        compose.onNodeWithText("Екзамени").performClick()
        compose.onNodeWithText("Зберегти періоди").performClick()
        compose.onNodeWithText("Періоди збережено").assertExists()

        val current = runBlocking { data.settingsRepository.settings.first().currentSemester }
            ?: runBlocking { data.settingsRepository.settings.first().effectiveCurrentSemester(data.studyPlanRepository.observeCourses().first()) }
        assertEquals(AssessmentPhaseMode.EXAM,
            runBlocking { data.settingsRepository.settings.first().semesterPeriods[current]?.assessmentMode })
    }
}
