package com.kpyruy.takt.core.data

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import java.time.Instant

data class ReminderPermissionStatus(val notificationsAllowed: Boolean, val exactAlarmsAllowed: Boolean,
    val blockedChannels: Set<String> = emptySet())

object AndroidReminderPermissions {
    fun read(context: Context): ReminderPermissionStatus {
        val app = context.applicationContext
        val notifications = app.getSystemService(NotificationManager::class.java)
        return ReminderPermissionStatus(
            notifications.areNotificationsEnabled() && (Build.VERSION.SDK_INT < 33 ||
                app.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED),
            Build.VERSION.SDK_INT < 31 || app.getSystemService(AlarmManager::class.java).canScheduleExactAlarms(),
            ReminderChannels.ids.filter { notifications.getNotificationChannel(it)?.importance == NotificationManager.IMPORTANCE_NONE }.toSet(),
        )
    }
    fun notificationSettingsIntent(context: Context): Intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    @android.annotation.TargetApi(31)
    fun exactAlarmSettingsIntent(context: Context): Intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
        Uri.parse("package:${context.packageName}"))
}

interface ReminderAlarmBackend {
    fun permissions(): ReminderPermissionStatus
    /** Returns true for exact scheduling; a revoked permission safely falls back to inexact. */
    fun schedule(reminder: PlannedReminder, triggerAt: Instant): Boolean
    /** One permission decision per reconciliation; old backends remain source-compatible. */
    fun schedule(reminder: PlannedReminder, triggerAt: Instant, exactAllowed: Boolean): Boolean = schedule(reminder, triggerAt)
    fun cancel(id: String)
    fun scheduleMaintenance(at: Instant)
    fun cancelMaintenance()
}

class AndroidReminderAlarmBackend(context: Context) : ReminderAlarmBackend {
    private val app = context.applicationContext
    private val alarms = app.getSystemService(AlarmManager::class.java)
    override fun permissions() = AndroidReminderPermissions.read(app)

    @android.annotation.SuppressLint("MissingPermission", "ScheduleExactAlarm") // Checked below; revocation falls back safely.
    override fun schedule(reminder: PlannedReminder, triggerAt: Instant): Boolean =
        schedule(reminder, triggerAt, permissions().exactAlarmsAllowed)

    @android.annotation.SuppressLint("MissingPermission", "ScheduleExactAlarm")
    override fun schedule(reminder: PlannedReminder, triggerAt: Instant, exactAllowed: Boolean): Boolean {
        val pending = alarmIntent(reminder.id, reminder.fireAt.toEpochMilli(), PendingIntent.FLAG_UPDATE_CURRENT)!!
        if (exactAllowed) {
            try {
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt.toEpochMilli(), pending)
                return true
            } catch (_: SecurityException) { /* Permission changed between check and scheduling. */ }
        }
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt.toEpochMilli(), pending)
        return false
    }
    override fun cancel(id: String) {
        alarmIntent(id, 0, PendingIntent.FLAG_NO_CREATE)?.let { alarms.cancel(it); it.cancel() }
    }
    override fun scheduleMaintenance(at: Instant) {
        val pending = maintenanceIntent(PendingIntent.FLAG_UPDATE_CURRENT)!!
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), pending)
    }
    override fun cancelMaintenance() {
        maintenanceIntent(PendingIntent.FLAG_NO_CREATE)?.let { alarms.cancel(it); it.cancel() }
    }
    private fun alarmIntent(id: String, fireAt: Long, flags: Int): PendingIntent? = PendingIntent.getBroadcast(app, 0,
        Intent(app, ReminderAlarmReceiver::class.java).setAction(ACTION_DELIVER)
            .setData(Uri.Builder().scheme("takt-reminder").authority(app.packageName).appendPath(id).build())
            .putExtra(EXTRA_ID, id).putExtra(EXTRA_FIRE_AT, fireAt), flags or PendingIntent.FLAG_IMMUTABLE)
    private fun maintenanceIntent(flags: Int): PendingIntent? = PendingIntent.getBroadcast(app, 0,
        Intent(app, ReminderRefreshReceiver::class.java).setAction(ACTION_MAINTENANCE), flags or PendingIntent.FLAG_IMMUTABLE)

    companion object {
        const val ACTION_DELIVER = "com.kpyruy.takt.REMINDER_DELIVER"
        const val ACTION_MAINTENANCE = "com.kpyruy.takt.REMINDER_MAINTENANCE"
        const val EXTRA_ID = "reminder_id"
        const val EXTRA_FIRE_AT = "fire_at"
    }
}
