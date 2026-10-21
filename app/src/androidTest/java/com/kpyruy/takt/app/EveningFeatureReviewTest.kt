package com.kpyruy.takt.app

import android.graphics.Bitmap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.kpyruy.takt.core.model.*
import com.kpyruy.takt.core.ui.i18n.t
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Actual app routes, not isolated lookalikes. Emulator fixture opt-in applies through seed. */
class EveningFeatureReviewTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val data get() = (compose.activity.application as TaktApplication).dataContainer
    @Test fun eventAvatarAndNotificationSettingsAreUsableAndDurable() {
        runBlocking {
            data.seedLiquidGlassReview()
            data.settingsRepository.setReminders(ReminderSettings())
            val args = InstrumentationRegistry.getArguments()
            data.settingsRepository.setLanguage(AppLanguage.valueOf(args.getString("reviewLanguage") ?: "SLOVAK"))
            data.settingsRepository.setThemeMode(AppThemeMode.valueOf(args.getString("reviewTheme") ?: "DARK"))
            data.settingsRepository.setThemeFamily(ThemeFamily.PURPLE)
            data.studyPlanRepository.setIcon("glass-physics", "Speed")
            data.awaitRestoredPlanning()
        }
        compose.waitForIdle(); dismissConnectionPrompt()
        root("Предмети")
        compose.onNode(hasText("Фізика") and hasClickAction()).performClick()
        compose.onNodeWithTag("subject-header-avatar").assertWidthIsAtLeast(androidx.compose.ui.unit.Dp(56f))
        capture("avatar-square")
        compose.onNodeWithTag("subject-header-avatar").performClick()
        compose.onNodeWithText(t("Subject icon")).assertExists()
        nativeBack()
        compose.onNodeWithTag("subject-header-back").performClick()
        compose.onNodeWithTag("root-add").performClick()
        compose.onNodeWithTag("global-add-EVENT").performScrollTo().performClick()
        compose.onNodeWithTag("oneoff-title").performTextInput("Шаховий турнір")
        hideKeyboard()
        compose.onNodeWithTag("lesson-type-trigger").performClick()
        compose.onNodeWithTag("lesson-type-EVENT").assertIsSelected()
        capture("event-types")
        compose.onNodeWithTag("lesson-type-EVENT").performClick()
        compose.onNodeWithText(t("Extra class")).assertDoesNotExist()
        compose.onNodeWithTag("oneoff-room").performScrollTo().performTextInput("Шаховий клуб")
        hideKeyboard(); capture("event-form")
        compose.onNodeWithTag("oneoff-save").performScrollTo().performClick()
        compose.waitUntil(5_000) { runBlocking { data.scheduleRepository.observeOneOffEvents().first().any { it.title == "Шаховий турнір" } } }
        val saved = runBlocking { data.scheduleRepository.observeOneOffEvents().first().single { it.title == "Шаховий турнір" } }
        assertEquals(LessonType.EVENT, saved.lessonType); assertNull(saved.courseId)
        root("Предмети")
        compose.onNodeWithTag("subjects-settings").performClick()
        compose.onNode(hasText(t("Notifications")) and hasClickAction()).performScrollTo().performClick()
        compose.onNodeWithTag("reminder_global_switch").assertIsOff()
        compose.onNodeWithTag("reminder_request_notifications").assertDoesNotExist()
        capture("notifications-off")
        compose.onNodeWithTag("reminder_global_switch").performClick()
        compose.onNodeWithTag("reminder_permissions").assertExists()
        capture("notification-permissions")
        compose.onNodeWithTag("reminder_academic_types").performScrollTo().performClick()
        compose.onNodeWithTag("reminder_edit_TEST").performScrollTo().performClick()
        compose.onNodeWithTag("reminder_preset_TEST_10080").performScrollTo().performClick()
        compose.waitUntil(5_000) { runBlocking { data.settingsRepository.settings.first().reminders.rule(ReminderCategory.TEST).offsetsMinutes.contains(10080L) } }
        capture("test-week-and-day")
        nativeBack(); nativeBack()
        compose.onNodeWithTag("reminder_edit_LESSON").performScrollTo().performClick()
        capture("lesson-30-minutes")
        nativeBack()
        compose.onNodeWithTag("reminder_global_switch").performScrollTo().performClick()
        compose.waitUntil(5_000) { !data.settingsRepository.settings.value.reminders.enabled }
        assertEquals(listOf(10080L, 1440L), data.settingsRepository.settings.value.reminders.rule(ReminderCategory.TEST).offsetsMinutes)
        assertEquals(listOf(30L), data.settingsRepository.settings.value.reminders.rule(ReminderCategory.LESSON).offsetsMinutes)
        runBlocking { data.reminderCoordinator.refreshNow() }
        assertTrue(runBlocking { data.reminderCoordinator.readPlannedAlarms().isEmpty() })
    }
    private fun root(key: String) = compose.onAllNodes(hasText(t(key)) and hasClickAction()).onLast().performClick()
    private fun hideKeyboard() {
        // Text input can finish before Android has published the new IME insets.
        instrumentation.waitForIdleSync()
        android.os.SystemClock.sleep(300)
        if (compose.activity.window.decorView.rootWindowInsets.isVisible(android.view.WindowInsets.Type.ime())) {
            nativeBack()
        }
    }
    private fun nativeBack() {
        instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.waitForIdle(); instrumentation.waitForIdleSync()
    }
    private fun dismissConnectionPrompt() {
        val id = compose.activity.resources.getIdentifier("m3c_snackbar_dismiss", "string", compose.activity.packageName)
        if (id == 0) return
        val labels = listOf(java.util.Locale.ENGLISH, java.util.Locale("sk"), java.util.Locale("uk")).map {
            compose.activity.createConfigurationContext(android.content.res.Configuration(compose.activity.resources.configuration).apply { setLocale(it) }).getString(id)
        }
        val nodes = compose.onAllNodes(labels.map { hasContentDescription(it) }.reduce { a,b -> a or b })
        compose.waitUntil(7_000) { nodes.fetchSemanticsNodes().isNotEmpty() };nodes.onFirst().performClick()
    }
    private fun capture(name: String) {
        compose.mainClock.advanceTimeBy(700);compose.waitForIdle();instrumentation.waitForIdleSync();android.os.SystemClock.sleep(400)
        val b = instrumentation.uiAutomation.takeScreenshot()
        val prefix = InstrumentationRegistry.getArguments().getString("reviewPrefix") ?: "evening"
        instrumentation.targetContext.openFileOutput("$prefix-$name.png", 0).use { b.compress(Bitmap.CompressFormat.PNG,100,it) };b.recycle()
    }
}
