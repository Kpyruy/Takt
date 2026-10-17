package com.kpyruy.takt.app

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.kpyruy.takt.core.model.*
import com.kpyruy.takt.core.ui.i18n.TaktI18n
import com.kpyruy.takt.core.ui.theme.TaktTheme
import com.kpyruy.takt.feature.settings.NotificationsSettingsPage
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.After

class ReminderSettingsUiTest {
    @get:Rule val compose = createComposeRule()
    private val originalLanguage = TaktI18n.language
    @After fun restoreUiLanguage() { TaktI18n.use(originalLanguage) }
    @Test fun offDoesNotExposePermissionActionsAndTypeEditorKeepsMultipleOffsets() {
        var config by mutableStateOf(ReminderSettings())
        TaktI18n.use(AppLanguage.ENGLISH)
        compose.setContent { TaktTheme { NotificationsSettingsPage(config, { config = it(config) }, {}) } }
        compose.onNodeWithTag("reminder_permissions").assertDoesNotExist()
        compose.onNodeWithTag("reminder_request_notifications").assertDoesNotExist()
        compose.onNodeWithTag("reminder_academic_types").performScrollTo().performClick()
        compose.onNodeWithTag("reminder_edit_TEST").performScrollTo().performClick()
        compose.onNodeWithTag("reminder_preset_TEST_10080").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(listOf(10080L, 1440L), config.rule(ReminderCategory.TEST).offsetsMinutes) }
        compose.onNodeWithTag("reminder_custom_TEST").performScrollTo().performClick()
        compose.onNodeWithTag("reminder_custom_amount").performTextInput("45")
        compose.onNodeWithTag("reminder_custom_add").performClick()
        compose.runOnIdle { assertEquals(listOf(10080L, 1440L, 45L), config.rule(ReminderCategory.TEST).offsetsMinutes) }
        compose.onNodeWithTag("reminder_remove_TEST_1440").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(listOf(10080L, 45L), config.rule(ReminderCategory.TEST).offsetsMinutes); assertFalse(config.enabled) }
    }
    @Test fun enablingIsExplicitAndLessonEventReminderSwitchesRemainIndependent() {
        var config by mutableStateOf(ReminderSettings())
        TaktI18n.use(AppLanguage.ENGLISH)
        compose.setContent { TaktTheme { NotificationsSettingsPage(config, { config = it(config) }, {}) } }
        compose.onNodeWithTag("reminder_global_switch").performClick()
        compose.onNodeWithTag("reminder_permissions").assertExists()
        compose.onNodeWithTag("reminder_rule_LESSON").performScrollTo().performClick()
        compose.runOnIdle {
            assertTrue(config.enabled)
            assertFalse(config.rule(ReminderCategory.LESSON).enabled)
            assertTrue(config.rule(ReminderCategory.EVENT).enabled)
            assertTrue(config.rule(ReminderCategory.REMINDER).enabled)
        }
        compose.onNodeWithTag("reminder_global_switch").performScrollTo().performClick()
        compose.onNodeWithTag("reminder_permissions").assertDoesNotExist()
        compose.runOnIdle { assertFalse(config.enabled) }
    }
    @Test fun delayedPersistenceKeepsTwoOffsetChangesAndIndependentToggle() {
        var config by mutableStateOf(ReminderSettings())
        val pending = mutableListOf<(ReminderSettings) -> ReminderSettings>()
        TaktI18n.use(AppLanguage.ENGLISH)
        compose.setContent { TaktTheme { NotificationsSettingsPage(config, { pending += it }, {}) } }
        compose.onNodeWithTag("reminder_academic_types").performScrollTo().performClick()
        compose.onNodeWithTag("reminder_edit_TEST").performScrollTo().performClick()
        // No state is published between clicks: the real durable writer can still be suspended.
        compose.onNodeWithTag("reminder_preset_TEST_10080").performScrollTo().performClick()
        compose.onNodeWithTag("reminder_preset_TEST_60").performScrollTo().performClick()
        compose.onNodeWithTag("reminder_rule_TEST").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(3, pending.size)
            pending.forEach { config = it(config) }
            assertEquals(listOf(10080L, 1440L, 60L), config.rule(ReminderCategory.TEST).offsetsMinutes)
            assertFalse(config.rule(ReminderCategory.TEST).enabled)
            assertTrue(config.rule(ReminderCategory.EVENT).enabled)
            assertFalse(config.enabled)
        }
    }

    @Test fun repeatedTogglesUseLatestStateBeforePublication() {
        var config by mutableStateOf(ReminderSettings())
        val pending = mutableListOf<(ReminderSettings) -> ReminderSettings>()
        compose.setContent { TaktTheme { NotificationsSettingsPage(config, { pending += it }, {}) } }
        repeat(2) { compose.onNodeWithTag("reminder_global_switch").performScrollTo().performClick() }
        repeat(2) { compose.onNodeWithTag("reminder_rule_LESSON").performScrollTo().performClick() }
        repeat(2) { compose.onNodeWithTag("reminder_academic_switch").performScrollTo().performClick() }
        compose.runOnIdle {
            assertEquals(6, pending.size)
            pending.forEach { config = it(config) }
            assertFalse(config.enabled)
            assertTrue(config.academicEnabled)
            assertTrue(config.rule(ReminderCategory.LESSON).enabled)
        }
    }

}
