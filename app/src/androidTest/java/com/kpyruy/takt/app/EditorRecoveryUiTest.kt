package com.kpyruy.takt.app

import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.activity.compose.setContent
import com.kpyruy.takt.core.data.*
import com.kpyruy.takt.core.model.*
import com.kpyruy.takt.core.ui.components.TaktGlassBackground
import com.kpyruy.takt.core.ui.i18n.TaktI18n
import com.kpyruy.takt.core.ui.theme.TaktTheme
import com.kpyruy.takt.feature.settings.SettingsScreen
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class EditorRecoveryUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val data get() = (compose.activity.application as TaktApplication).dataContainer
    private fun prepare() {
        runBlocking {
            data.seedLiquidGlassReview()
            data.settingsRepository.setLanguage(AppLanguage.UKRAINIAN)
            data.settingsRepository.setAppearance(data.settingsRepository.settings.value.copy(cardAppearance = CardAppearance.GLASS))
        }
    }
    private fun showSettings(repository: AppSettingsRepository = data.settingsRepository) {
        prepare()
        compose.activityRule.scenario.onActivity { activity -> activity.setContent {
            TaktI18n.use(AppLanguage.UKRAINIAN)
            TaktTheme(AppSettings(language = AppLanguage.UKRAINIAN)) { TaktGlassBackground {
                SettingsScreen(repository, data.backupRepository, data.documentStore, data.studyPlanRepository,
                    data.universityAccountRepository, { _, reply -> reply(DeviceAuthenticationResult.CANCELLED) },
                    (compose.activity.application as TaktApplication).appReleaseRepository, {})
            } }
        } }
        compose.onNodeWithText("Вигляд").performScrollTo().performClick()
        compose.onNodeWithTag("appearance-card-ELEVATED").performScrollTo().performClick()
    }
    @Test fun appearanceRouteAndUnsavedCardDraftSurviveSavedStateRestoration() {
        prepare()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithContentDescription("Налаштування").performClick()
        compose.onNodeWithText("Вигляд").performScrollTo().performClick()
        compose.onNodeWithTag("appearance-card-ELEVATED").performScrollTo().performClick()
        assertEquals(CardAppearance.GLASS, data.settingsRepository.settings.value.cardAppearance)
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Твій вигляд").assertExists()
        compose.onNodeWithTag("appearance-card-ELEVATED").performScrollTo().assertIsSelected()
        assertEquals(CardAppearance.GLASS, data.settingsRepository.settings.value.cardAppearance)
        compose.onNodeWithText("Застосувати вигляд").performClick()
        compose.waitUntil(5000) { data.settingsRepository.settings.value.cardAppearance == CardAppearance.ELEVATED }
    }
    @Test fun failedAppearanceSaveKeepsDraftAndAllowsExplicitRetry() {
        var fail = true
        val failing = object : AppSettingsRepository by data.settingsRepository {
            override suspend fun setAppearance(appearance: AppSettings) {
                if (fail) throw java.io.IOException("Injected storage failure")
                data.settingsRepository.setAppearance(appearance)
            }
        }
        showSettings(failing)
        compose.onNodeWithText("Застосувати вигляд").performClick()
        compose.onNodeWithTag("save-error-dialog").assertExists()
        assertEquals(CardAppearance.GLASS, data.settingsRepository.settings.value.cardAppearance)
        compose.onNodeWithTag("save-error-dismiss").performClick()
        compose.onNodeWithTag("appearance-card-ELEVATED").performScrollTo().assertIsSelected()
        fail = false
        compose.onNodeWithText("Застосувати вигляд").performClick()
        compose.waitUntil(5000) { data.settingsRepository.settings.value.cardAppearance == CardAppearance.ELEVATED }
    }
    @Test fun movingLessonKeepsEditorAndUnsavedRoomAcrossActivityRecreation() {
        prepare()
        val date = java.time.LocalDate.now()
        runBlocking {
            data.scheduleRepository.upsertRule(ScheduleRule("editor-rule", "glass-physics", "Editor lesson",
                date.dayOfWeek, java.time.LocalTime.of(13, 0), java.time.LocalTime.of(14, 0), ScheduleRecurrence.WEEKLY))
            data.settingsRepository.setSemesterPeriods(emptyMap())
            data.awaitRestoredPlanning()
        }
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("home-lesson-editor-rule").performScrollTo()
            .performSemanticsAction(SemanticsActions.OnLongClick) { assertTrue(it()) }
        compose.onNodeWithText("Перенести цю пару").performClick()
        compose.onNode(hasText("Аудиторія") and hasSetTextAction()).performScrollTo().performTextReplacement("Draft room")
        compose.activityRule.scenario.recreate()
        compose.onNode(hasText("Аудиторія") and hasSetTextAction()).assertTextContains("Draft room")
        assertTrue(runBlocking { data.scheduleRepository.observeExceptions().first() }.none { it.ruleId == "editor-rule" })
        compose.onNodeWithText("Перенести", useUnmergedTree = true).performScrollTo().performClick()
        compose.waitUntil(5000) { runBlocking { data.scheduleRepository.observeExceptions().first() }.any { it.ruleId == "editor-rule" } }
        compose.onNodeWithText("Перенести пару").assertDoesNotExist()
        compose.onNodeWithText("Перенести цю пару").assertDoesNotExist()
        assertEquals("Draft room", runBlocking { data.scheduleRepository.observeExceptions().first() }.single { it.ruleId == "editor-rule" }.replacementRoom)
    }

    @Test fun recurringLessonEditKeepsItsDraftAcrossActivityRecreation() {
        prepare()
        val date = java.time.LocalDate.now()
        runBlocking {
            data.scheduleRepository.upsertRule(ScheduleRule("edit-rule", "glass-physics", "Edit lesson", date.dayOfWeek,
                java.time.LocalTime.of(15, 0), java.time.LocalTime.of(16, 0), ScheduleRecurrence.WEEKLY))
            data.settingsRepository.setSemesterPeriods(emptyMap())
            data.awaitRestoredPlanning()
        }
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("home-lesson-edit-rule").performScrollTo()
            .performSemanticsAction(SemanticsActions.OnLongClick) { assertTrue(it()) }
        compose.onNodeWithText("Редагувати").performClick()
        compose.onNodeWithTag("lesson-room").performScrollTo().performTextReplacement("Edited room")
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("lesson-room").assertTextContains("Edited room")
        assertNotEquals("Edited room", runBlocking { data.scheduleRepository.observeRules().first() }.single { it.id == "edit-rule" }.room)
    }

}
