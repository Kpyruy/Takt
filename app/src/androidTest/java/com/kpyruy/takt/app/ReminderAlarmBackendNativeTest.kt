package com.kpyruy.takt.app

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.kpyruy.takt.core.data.*
import com.kpyruy.takt.core.model.ReminderCategory
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

/** Exercises native AlarmManager in whichever exact-access state root selected, without prompting. */
class ReminderAlarmBackendNativeTest {
    @Test fun platformAlarmArmsImmutableIdentityAndCancellationRemovesPendingIntent() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val backend = AndroidReminderAlarmBackend(context)
        val id = "native-backend-${System.nanoTime()}"
        val now = Instant.now()
        val reminder = PlannedReminder(id, "native-test", ReminderCategory.REMINDER, "Native alarm", null,
            now.plusSeconds(7200), now.plusSeconds(3600), 60, false)
        fun token() = PendingIntent.getBroadcast(context, 0, Intent(context, ReminderAlarmReceiver::class.java)
            .setAction(AndroidReminderAlarmBackend.ACTION_DELIVER)
            .setData(Uri.Builder().scheme("takt-reminder").authority(context.packageName).appendPath(id).build()),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        try {
            val exact = backend.schedule(reminder, reminder.fireAt)
            assertEquals(backend.permissions().exactAlarmsAllowed, exact)
            assertNotNull(token())
        } finally { backend.cancel(id) }
        assertNull(token())
    }
}
