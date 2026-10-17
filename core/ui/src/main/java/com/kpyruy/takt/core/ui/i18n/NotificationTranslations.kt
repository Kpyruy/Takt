package com.kpyruy.takt.core.ui.i18n

import com.kpyruy.takt.core.model.AppLanguage
import com.kpyruy.takt.core.model.ReminderCategory
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Also usable from an alarm receiver: never changes TaktI18n.language or Compose state. */
object NotificationTranslations {
    private val translations = mapOf(
        "Сповіщення" to ("Notifications" to "Upozornenia"),
        "Сповіщення для нагадувань можна налаштувати в «Під себе» → «Сповіщення»." to ("Reminder notifications can be configured in For yourself → Notifications." to "Upozornenia pre pripomienky možno nastaviť v Pre seba → Upozornenia."),
        "Локальні нагадування" to ("Local reminders" to "Lokálne pripomienky"),
        "Усі нагадування вимкнено" to ("All reminders are off" to "Všetky pripomienky sú vypnuté"),
        "Дозволи не потрібні, поки нагадування вимкнено." to ("No permissions are needed while reminders are off." to "Kým sú pripomienky vypnuté, nie sú potrebné povolenia."),
        "Нагадування перед заняттями, подіями й термінами робіт." to ("Reminders before lessons, events and work due dates." to "Pripomienky pred vyučovaním, udalosťami a termínmi povinností."),
        "Дозволи Android" to ("Android permissions" to "Povolenia Androidu"),
        "Не вдалося оновити нагадування." to ("Could not refresh reminders." to "Nepodarilo sa aktualizovať pripomienky."),
        "Спробувати ще раз" to ("Try again" to "Skúsiť znova"),
        "Сповіщення дозволено" to ("Notifications allowed" to "Upozornenia sú povolené"),
        "Сповіщення заблоковано" to ("Notifications blocked" to "Upozornenia sú zablokované"),
        "Дозволити сповіщення" to ("Allow notifications" to "Povoliť upozornenia"),
        "Налаштування сповіщень Android" to ("Android notification settings" to "Nastavenia upozornení Androidu"),
        "Точні нагадування дозволено" to ("Exact reminders allowed" to "Presné pripomienky sú povolené"),
        "Приблизний час: Android може затримати нагадування." to ("Approximate timing: Android may delay reminders." to "Približný čas: Android môže pripomienky oneskoriť."),
        "Дозволити точний час" to ("Allow exact timing" to "Povoliť presný čas"),
        "Частину каналів вимкнено в Android." to ("Some channels are disabled in Android." to "Niektoré kanály sú v Androide vypnuté."),
        "Android може обмежувати доставку через енергозбереження або примусову зупинку." to ("Battery restrictions or force stopping may prevent Android delivery." to "Šetrenie batérie alebo vynútené zastavenie môžu zabrániť doručeniu v Androide."),
        "Навчальні роботи" to ("Academic work" to "Študijné povinnosti"),
        "Усі типи робіт" to ("All work types" to "Všetky typy povinností"),
        "Вибрані типи робіт" to ("Selected work types" to "Vybrané typy povinností"),
        "Вибрати всі типи" to ("Select all types" to "Vybrať všetky typy"),
        "Вимкнути всі типи" to ("Disable all types" to "Vypnúť všetky typy"),
        "Типи й час нагадувань" to ("Types and reminder timing" to "Typy a čas pripomienok"),
        "Звичайні задачі" to ("Ordinary tasks" to "Bežné úlohy"),
        "Інша навчальна робота" to ("Other academic work" to "Iné študijné povinnosti"),
        "Події" to ("Events" to "Udalosti"),
        "Нагадування" to ("Reminders" to "Pripomienky"),
        "Заняття" to ("Lessons" to "Vyučovanie"),
        "Тест" to ("Test" to "Test"),
        "Лабораторна" to ("Lab" to "Laboratórne cvičenie"),
        "Семінар" to ("Seminar" to "Seminár"),
        "Домашня робота" to ("Homework" to "Domáca úloha"),
        "Проєкт" to ("Project" to "Projekt"),
        "Усна відповідь" to ("Oral assessment" to "Ústna odpoveď"),
        "Екзамен" to ("Exam" to "Skúška"),
        "Вимкнено" to ("Off" to "Vypnuté"),
        "Увімкнено" to ("On" to "Zapnuté"),
        "Час нагадувань" to ("Reminder timing" to "Čas pripomienok"),
        "До початку або терміну" to ("Before start or due date" to "Pred začiatkom alebo termínom"),
        "Жодного часу не вибрано" to ("No reminder times selected" to "Nie je vybraný žiadny čas"),
        "Додати інший час" to ("Add custom timing" to "Pridať vlastný čas"),
        "Кількість" to ("Amount" to "Počet"),
        "Одиниця часу" to ("Time unit" to "Jednotka času"),
        "хв" to ("min" to "min"), "год" to ("h" to "h"), "дн" to ("days" to "dní"), "тиж" to ("weeks" to "týždňov"),
        "Додати" to ("Add" to "Pridať"),
        "Видалити час" to ("Remove timing" to "Odstrániť čas"),
        "Готово" to ("Done" to "Hotovo"),
        "Скасувати" to ("Cancel" to "Zrušiť"),
        "Назад" to ("Back" to "Späť"),
        "Час для робіт без часу початку" to ("Time for work without a start time" to "Čas pre povinnosti bez času začiatku"),
        "Для задач і робіт із датою. Тест із прив’язаним заняттям використовує його початок." to ("For tasks and work with a date. A test linked to a lesson uses its start time." to "Pre úlohy a povinnosti s dátumom. Test priradený k vyučovaniu používa jeho čas začiatku."),
        "Введіть додатний час, не більше 365 днів." to ("Enter a positive time, up to 365 days." to "Zadajte kladný čas, najviac 365 dní."),
        "Навчальна робота й задачі" to ("Academic work and tasks" to "Študijné povinnosti a úlohy"),
        "Події й нагадування" to ("Events and reminders" to "Udalosti a pripomienky"),
        "Задача" to ("Task" to "Úloha"),
        "Подія" to ("Event" to "Udalosť"),
        "Термін: %s" to ("Due: %s" to "Termín: %s"),
        "Початок: %s" to ("Starts: %s" to "Začiatok: %s"),
    )

    fun lookup(key: String, language: AppLanguage): String? = translations[key]?.let {
        when (language) { AppLanguage.UKRAINIAN -> key; AppLanguage.ENGLISH -> it.first; AppLanguage.SLOVAK -> it.second }
    }
    fun text(key: String, language: AppLanguage): String = lookup(key, language) ?: key
    fun locale(language: AppLanguage): Locale = when (language) {
        AppLanguage.UKRAINIAN -> Locale.forLanguageTag("uk")
        AppLanguage.ENGLISH -> Locale.ENGLISH
        AppLanguage.SLOVAK -> Locale.forLanguageTag("sk")
    }
    fun title(category: ReminderCategory, title: String, language: AppLanguage): String {
        val singular = when (category) {
            ReminderCategory.REMINDER -> when (language) { AppLanguage.ENGLISH -> "Reminder"; AppLanguage.SLOVAK -> "Pripomienka"; else -> "Нагадування" }
            ReminderCategory.LESSON -> when (language) { AppLanguage.ENGLISH -> "Lesson"; AppLanguage.SLOVAK -> "Vyučovanie"; else -> "Заняття" }
            else -> null
        }
        val label = when (category) { ReminderCategory.TASK -> "Задача"; ReminderCategory.EVENT -> "Подія"; else -> category.label }
        return "${singular ?: text(label, language)}: $title"
    }
    fun body(occursAt: Instant, dateOnly: Boolean, courseTitle: String?, language: AppLanguage, zone: ZoneId): String {
        val locale = locale(language)
        val date = occursAt.atZone(zone).format(DateTimeFormatter.ofPattern(if (dateOnly) "d MMM" else "d MMM, HH:mm", locale))
        val time = String.format(locale, text(if (dateOnly) "Термін: %s" else "Початок: %s", language), date)
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
            else -> unit.label
        }
        return "$count $label"
    }
}
