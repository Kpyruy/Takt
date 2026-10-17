package com.kpyruy.takt.core.ui.i18n

import com.kpyruy.takt.core.model.AppLanguage

internal object TeachingTranslations {
    private val entries = mapOf(
        "Викладачі" to ("Teaching team" to "Vyučujúci"),
        "Оновити викладачів" to ("Refresh teaching team" to "Obnoviť vyučujúcich"),
        "Викладачі з’являться після імпорту предмета з UIS." to ("Teaching staff will appear after importing this subject from UIS." to "Vyučujúci sa zobrazia po importe predmetu z UIS."),
        "Підключи UIS, щоб переглядати викладачів і контакти." to ("Connect UIS to view teaching staff and contacts." to "Pripoj UIS na zobrazenie vyučujúcich a kontaktov."),
        "Відкрити налаштування" to ("Open settings" to "Otvoriť nastavenia"),
        "У програмі предмета викладачів не вказано." to ("No teaching staff are listed in the syllabus." to "V informačnom liste nie sú uvedení vyučujúci."),
        "Профіль викладача" to ("Lecturer profile" to "Profil vyučujúceho"),
        "Екзаменатор" to ("Examiner" to "Skúšajúci"),
        "Практичні заняття" to ("Tutorials" to "Cvičenia"),
        "Лекції" to ("Lectures" to "Prednášky"),
        "Відповідальний за предмет" to ("Course supervisor" to "Garant predmetu"),
        "Підтвердь доступ до UIS, щоб завантажити контакти." to ("Confirm UIS access to load contacts." to "Potvrď prístup do UIS na načítanie kontaktov."),
        "Увійди в UIS або підтвердь код у налаштуваннях." to ("Sign in to UIS or enter the verification code in settings." to "Prihlás sa do UIS alebo potvrď overovací kód v nastaveniach."),
        "Не вдалося завантажити контакти. Перевір з’єднання та спробуй ще раз." to ("Could not load contacts. Check your connection and try again." to "Kontakty sa nepodarilo načítať. Skontroluj pripojenie a skús to znova."),
        "Контакти викладачів UIS" to ("UIS teaching contacts" to "Kontakty vyučujúcich v UIS"),
        "Підтвердити доступ" to ("Confirm access" to "Potvrdiť prístup"),
        "Спробувати ще раз" to ("Try again" to "Skúsiť znova"),
        "Контакти" to ("Contacts" to "Kontakty"),
        "Університетська пошта" to ("University email" to "Univerzitný e-mail"),
        "Пошта UIS" to ("UIS email" to "E-mail v UIS"),
        "Пошту не вказано в UIS." to ("No email address is listed in UIS." to "E-mailová adresa nie je uvedená v UIS."),
        "Телефон кабінету" to ("Office phone" to "Telefón do kancelárie"),
        "Не знайдено застосунку для пошти." to ("No email app was found." to "Nenašla sa aplikácia na e-mail."),
        "Кабінет і консультації" to ("Office and consultations" to "Kancelária a konzultácie"),
        "Кабінет" to ("Office" to "Kancelária"),
        "Не вказано в UIS" to ("Not listed in UIS" to "Neuvedené v UIS"),
        "Адреса" to ("Address" to "Adresa"),
        "Консультаційні години" to ("Consultation hours" to "Konzultačné hodiny"),
        "Не вказані в UIS" to ("Not listed in UIS" to "Neuvedené v UIS"),
        "Остання перевірка університетської пошти" to ("University mailbox last checked" to "Posledná kontrola univerzitnej schránky"),
        "За даними UIS" to ("As reported by UIS" to "Podľa údajov UIS"),
        "Профіль у UIS" to ("Profile in UIS" to "Profil v UIS"),
    )
    fun lookup(key: String, language: AppLanguage): String? = entries[key]?.let {
        if (language == AppLanguage.SLOVAK) it.second else it.first
    }
}
