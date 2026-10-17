package com.kpyruy.takt.core.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.kpyruy.takt.core.model.AppLanguage
import com.kpyruy.takt.core.model.ReminderCategory
import java.time.ZoneId

object ReminderChannels {
    const val WORK = "takt_local_work_v1"
    const val LESSON = "takt_local_lessons_v1"
    const val EVENT = "takt_local_events_v1"
    val ids = listOf(WORK, LESSON, EVENT)
    fun forCategory(category: ReminderCategory): String = when (category) {
        ReminderCategory.LESSON -> LESSON
        ReminderCategory.EVENT, ReminderCategory.REMINDER -> EVENT
        else -> WORK
    }
}

data class ReminderNotificationText(val title: String, val body: String)

/** Injected by the app: data does not depend on Compose or mutate its global locale. */
interface ReminderTextProvider {
    fun channelName(id: String, language: AppLanguage): String
    fun notification(reminder: PlannedReminder, language: AppLanguage, zone: ZoneId): ReminderNotificationText
}

interface ReminderNotificationDelivery {
    fun bootstrapChannels(language: AppLanguage)
    fun post(reminder: PlannedReminder, language: AppLanguage, zone: ZoneId): Boolean
    fun cancel(id: String)
    fun cancelAllReminders()
}

class AndroidReminderNotificationDelivery(context: Context, private val text: ReminderTextProvider) : ReminderNotificationDelivery {
    private val app = context.applicationContext
    private val manager = app.getSystemService(NotificationManager::class.java)
    override fun bootstrapChannels(language: AppLanguage) {
        // Re-creating a channel updates its localized name while preserving user sound/importance choices.
        manager.createNotificationChannels(ReminderChannels.ids.map { id ->
            NotificationChannel(id, text.channelName(id, language), NotificationManager.IMPORTANCE_DEFAULT)
        })
    }
    @android.annotation.SuppressLint("MissingPermission") // Central permission helper checks POST_NOTIFICATIONS; notify also catches revocation.
    override fun post(reminder: PlannedReminder, language: AppLanguage, zone: ZoneId): Boolean {
        if (!AndroidReminderPermissions.read(app).notificationsAllowed) return false
        bootstrapChannels(language)
        val channel = ReminderChannels.forCategory(reminder.category)
        if (manager.getNotificationChannel(channel)?.importance == NotificationManager.IMPORTANCE_NONE) return false
        // The ordinary launcher activity retains the existing device lock gate. No unlock extras/deep-link bypass.
        val launch = app.packageManager.getLaunchIntentForPackage(app.packageName) ?: return false
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val contentIntent = PendingIntent.getActivity(app, 0, launch, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val message = text.notification(reminder, language, zone)
        val notification = Notification.Builder(app, channel)
            .setSmallIcon(com.kpyruy.takt.core.data.R.drawable.takt_reminder_small)
            .setContentTitle(message.title).setContentText(message.body)
            .setStyle(Notification.BigTextStyle().bigText(message.body))
            .setContentIntent(contentIntent).setAutoCancel(true).setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_REMINDER)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setWhen(reminder.occursAt.toEpochMilli()).build()
        return try { manager.notify(tag(reminder.id), 0, notification); true } catch (_: SecurityException) { false }
    }
    override fun cancel(id: String) { manager.cancel(tag(id), 0) }
    override fun cancelAllReminders() {
        manager.activeNotifications.filter { it.tag?.startsWith(TAG_PREFIX) == true }
            .forEach { manager.cancel(it.tag, it.id) }
    }
    private fun tag(id: String) = TAG_PREFIX + id
    companion object { const val TAG_PREFIX = "takt-reminder:" }
}
