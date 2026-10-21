package com.kpyruy.takt.core.ui.i18n

import com.kpyruy.takt.core.model.AppLanguage
import com.kpyruy.takt.core.model.ReminderCategory
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Also usable from an alarm receiver: never changes TaktI18n.language or Compose state. */
object NotificationTranslations {
    fun lookup(key: String, language: AppLanguage): String? = TaktI18n.lookup(key, language)
    fun text(key: String, language: AppLanguage): String = TaktI18n.text(key, language)
    fun locale(language: AppLanguage): Locale = TaktI18n.locale(language)
    fun title(category: ReminderCategory, title: String, language: AppLanguage): String {
        val key = when (category) {
            ReminderCategory.REMINDER -> "Reminder notification"
            ReminderCategory.LESSON -> "Lesson"
            ReminderCategory.TASK -> "Task"
            ReminderCategory.EVENT -> "Event"
            else -> category.label
        }
        return "${text(key, language)}: $title"
    }
    fun body(occursAt: Instant, dateOnly: Boolean, courseTitle: String?, language: AppLanguage, zone: ZoneId): String {
        val locale = locale(language)
        val date = occursAt.atZone(zone).format(DateTimeFormatter.ofPattern(if (dateOnly) "d MMM" else "d MMM, HH:mm", locale))
        val time = String.format(locale, text(if (dateOnly) "Due: %s" else "Starts: %s", language), date)
        return listOfNotNull(courseTitle?.takeIf { it.isNotBlank() }, time).joinToString(" · ")
    }
    fun offset(minutes: Long, language: AppLanguage): String {
        val unit = com.kpyruy.takt.core.model.ReminderLeadUnit.entries.lastOrNull { minutes % it.minutes == 0L }
            ?: com.kpyruy.takt.core.model.ReminderLeadUnit.MINUTES
        val count = minutes / unit.minutes
        val label = when (language) {
            AppLanguage.ENGLISH -> when (unit) {
                com.kpyruy.takt.core.model.ReminderLeadUnit.DAYS -> if (count == 1L) "day" else "days"
                com.kpyruy.takt.core.model.ReminderLeadUnit.WEEKS -> if (count == 1L) "week" else "weeks"
                else -> text(unit.label, language)
            }
            AppLanguage.SLOVAK -> when (unit) {
                com.kpyruy.takt.core.model.ReminderLeadUnit.DAYS -> if (count == 1L) "deň" else if (count in 2L..4L) "dni" else "dní"
                com.kpyruy.takt.core.model.ReminderLeadUnit.WEEKS -> if (count == 1L) "týždeň" else if (count in 2L..4L) "týždne" else "týždňov"
                else -> text(unit.label, language)
            }
            else -> text(unit.label, language)
        }
        return "$count $label"
    }
}
