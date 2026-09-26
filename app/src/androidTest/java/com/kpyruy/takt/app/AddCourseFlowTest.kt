package com.kpyruy.takt.app

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class AddCourseFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun newCourseCanBeCreatedFromAddMenu() {
        val data = (compose.activity.application as TaktApplication).dataContainer
        val code = "TEST_" + UUID.randomUUID().toString().take(8).uppercase()
        compose.onNodeWithTag("root-add").performClick()
        compose.onNodeWithText("Предмет").performClick()
        compose.onNodeWithTag("course-title").performTextInput("Мій предмет")
        compose.onNodeWithTag("course-code").performTextInput(code)
        compose.onNodeWithTag("course-credits").performTextInput("5")
        compose.onNodeWithTag("save-course").performClick()
        compose.waitUntil(5_000) {
            runBlocking { data.studyPlanRepository.observeCourses().first().any { it.code == code } }
        }
    }
}
