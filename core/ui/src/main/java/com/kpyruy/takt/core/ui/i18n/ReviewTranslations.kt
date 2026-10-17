package com.kpyruy.takt.core.ui.i18n
import com.kpyruy.takt.core.model.AppLanguage
internal object ReviewTranslations {
 private val entries=mapOf(
  "Викладачі й консультації" to ("Teachers and consultations" to "Vyučujúci a konzultácie"),
  "Для викладачів нових предметів також вибери «Предмети»." to ("To include teachers for new courses, also select Courses." to "Na prenos vyučujúcich nových predmetov vyber aj Predmety."),
  "Ім’я викладача" to ("Teacher name" to "Meno vyučujúceho"),
  "Посада" to ("Position" to "Pracovná pozícia"),
  "Фото недоступне" to ("Photo unavailable" to "Fotografia nie je dostupná"),
  "Обрані предмети" to ("Selected courses" to "Vybrané predmety"),
  "Повторювані заняття" to ("Recurring lessons" to "Pravidelné hodiny"),
  "Окремі заняття" to ("One-off lessons" to "Jednorazové hodiny"),
  "Щотижня" to ("Every week" to "Každý týždeň"),
  "Непарні тижні" to ("Odd weeks" to "Nepárne týždne"),
  "Парні тижні" to ("Even weeks" to "Párne týždne"),
  "Додано" to ("Added" to "Pridané"),
  "Змінено" to ("Changed" to "Zmenené"),
  "Видалено" to ("Removed" to "Odstránené"),
  "Власне фото" to ("Custom photo" to "Vlastná fotografia"),
  "Фото не включено в цю копію" to ("Photo not included in this backup" to "Táto záloha neobsahuje fotografiu"),
  "Збережені теми" to ("Saved themes" to "Uložené témy"),
  "Змінено вміст або склад нотаток." to ("Note contents or selection changed." to "Obsah alebo zoznam poznámok sa zmenil."),
  "Навчальний матеріал" to ("Study material" to "Študijný materiál"),
  "Показано перші зміни. Застосовується весь вибраний розділ." to ("First changes shown. The entire selected section will be applied." to "Zobrazené sú prvé zmeny. Použije sa celá vybraná časť."),
 )
 fun lookup(source:String,language:AppLanguage):String?=entries[source]?.let{when(language){AppLanguage.ENGLISH->it.first;AppLanguage.SLOVAK->it.second;else->source}}
}
