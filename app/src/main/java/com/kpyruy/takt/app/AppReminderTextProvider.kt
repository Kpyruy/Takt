package com.kpyruy.takt.app

import com.kpyruy.takt.core.data.PlannedReminder
import com.kpyruy.takt.core.data.ReminderChannels
import com.kpyruy.takt.core.data.ReminderNotificationText
import com.kpyruy.takt.core.data.ReminderTextProvider
import com.kpyruy.takt.core.model.AppLanguage
import com.kpyruy.takt.core.ui.i18n.NotificationTranslations
import java.time.ZoneId

internal object AppReminderTextProvider : ReminderTextProvider {
    override fun channelName(id: String, language: AppLanguage): String = NotificationTranslations.text(when (id) {
        ReminderChannels.WORK -> "Навчальна робота й задачі"
        ReminderChannels.LESSON -> "Заняття"
        else -> "Події й нагадування"
    }, language)

    override fun notification(reminder: PlannedReminder, language: AppLanguage, zone: ZoneId) = ReminderNotificationText(
        NotificationTranslations.title(reminder.category, reminder.title, language),
        NotificationTranslations.body(reminder.occursAt, reminder.dateOnly, reminder.courseTitle, language, zone))
}
