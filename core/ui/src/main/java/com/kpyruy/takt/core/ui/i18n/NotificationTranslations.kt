package com.kpyruy.takt.core.ui.i18n

import com.kpyruy.takt.core.model.AppLanguage
import com.kpyruy.takt.core.model.ReminderCategory
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.kpyruy.takt.core.model.UiKey
import com.kpyruy.takt.core.model.uiMessage
import com.kpyruy.takt.core.model.uiArg

/** Also usable from an alarm receiver: never changes TaktI18n.language or Compose state. */
object NotificationTranslations {
    fun lookup(key: String, language: AppLanguage): String? = TaktI18n.lookup(key, language)
    fun text(key: String, language: AppLanguage): String = TaktI18n.text(key, language)
    fun locale(language: AppLanguage): Locale = TaktI18n.locale(language)
    fun title(category: ReminderCategory, title: String, language: AppLanguage): String {
        val key = when (category) {
            ReminderCategory.REMINDER -> UiKey.TEXT_REMINDER_NOTIFICATION
            ReminderCategory.LESSON -> UiKey.TEXT_LESSON
            ReminderCategory.TASK -> UiKey.TEXT_TASK
            ReminderCategory.EVENT -> UiKey.TEXT_EVENT
            else -> category.label
        }
        return text(uiMessage(UiKey.FORMAT_NOTIFICATION_TITLE, uiArg(key), title), language)
    }
    fun body(occursAt: Instant, dateOnly: Boolean, courseTitle: String?, language: AppLanguage, zone: ZoneId): String {
        val locale = locale(language)
        val date = occursAt.atZone(zone).format(DateTimeFormatter.ofPattern(if (dateOnly) "d MMM" else "d MMM, HH:mm", locale))
        val time = text(uiMessage(if (dateOnly) UiKey.NOTIFICATION_DUE else UiKey.NOTIFICATION_STARTS, date), language)
        return listOfNotNull(courseTitle?.takeIf { it.isNotBlank() }, time).joinToString(" · ")
    }
    fun offset(minutes: Long, language: AppLanguage): String {
        val unit = com.kpyruy.takt.core.model.ReminderLeadUnit.entries.lastOrNull { minutes % it.minutes == 0L }
            ?: com.kpyruy.takt.core.model.ReminderLeadUnit.MINUTES
        val count = minutes / unit.minutes
        return formatLeadCount(count, unit, language)
    }
}
