package com.kpyruy.takt.app

import android.accessibilityservice.AccessibilityService
import android.app.NotificationManager
import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.kpyruy.takt.core.data.*
import com.kpyruy.takt.core.model.*
import java.time.Duration
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/** Explicit opt-in: a real OS alarm, real background BroadcastReceiver and NotificationManager. */
class ReminderDeliveryEndToEndTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun realAlarmPostsWhileActivityIsInBackground() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        assumeTrue(instrumentation.arguments.getString("reminderSystemAlarm") == "true")
        val app = compose.activity.application as TaktApplication
        val data = app.dataContainer
        val context = instrumentation.targetContext
        val permissions = AndroidReminderPermissions.read(context)
        assumeTrue(permissions.notificationsAllowed && permissions.exactAlarmsAllowed)
        val previous = data.settingsRepository.settings.value.reminders
        val id = "os-reminder-${System.nanoTime()}"
        val now = ZonedDateTime.now()
        var fire = now.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1)
        if (Duration.between(now, fire).seconds < 15) fire = fire.plusMinutes(1)
        val eventAt = fire.plusMinutes(1)
        val event = OneOffScheduleEvent(id, null, "Takt notification verification", eventAt.toLocalDate(),
            eventAt.toLocalTime(), eventAt.plusMinutes(30).toLocalTime(), "Review", OneOffScheduleEventType.EXTRA, LessonType.EVENT)
        val manager = context.getSystemService(NotificationManager::class.java)
        var reminder: PlannedReminder? = null
        try {
            data.settingsRepository.setReminders(ReminderSettings(enabled = true,
                rules = ReminderCategory.entries.associateWith { ReminderRule(it == ReminderCategory.EVENT, listOf(1)) }))
            data.scheduleRepository.upsertOneOffEvent(event)
            reminder = app.reminderCoordinator.refreshNow().single { it.sourceKey.contains(id) }
            assertEquals(fire.toInstant(), reminder.fireAt)
            assertTrue("Background transition must succeed", instrumentation.uiAutomation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME))
            val tag = AndroidReminderNotificationDelivery.TAG_PREFIX + reminder.id
            val deadline = SystemClock.elapsedRealtime() + 90_000
            while (manager.activeNotifications.none { it.tag == tag } && SystemClock.elapsedRealtime() < deadline) {
                SystemClock.sleep(200)
            }
            val posted = manager.activeNotifications.singleOrNull { it.tag == tag }
            assertNotNull("The OS alarm must deliver without foreground Activity or direct deliver() call", posted)
            assertNotNull(posted!!.notification.contentIntent)
            assertEquals(ReminderChannels.EVENT, posted.notification.channelId)
            assertFalse("Delivered alarm must be consumed", app.reminderCoordinator.readPlannedAlarms().any { it.id == reminder.id })
            assertTrue(instrumentation.uiAutomation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS))
            SystemClock.sleep(450)
            val picture = instrumentation.uiAutomation.takeScreenshot()
            context.openFileOutput("reminder-os-delivered.png", 0).use { picture.compress(Bitmap.CompressFormat.PNG, 100, it) }
            picture.recycle()
            instrumentation.uiAutomation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
        } finally {
            reminder?.let { manager.cancel(AndroidReminderNotificationDelivery.TAG_PREFIX + it.id, 0) }
            data.scheduleRepository.deleteOneOffEvent(id)
            data.settingsRepository.setReminders(previous)
            app.reminderCoordinator.refreshNow()
        }
    }
}
