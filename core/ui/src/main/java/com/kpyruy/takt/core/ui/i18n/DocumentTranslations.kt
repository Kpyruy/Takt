package com.kpyruy.takt.core.ui.i18n

import com.kpyruy.takt.core.model.AppLanguage

internal object DocumentTranslations {
    private val entries = mapOf(
        "Документи предмета" to ("Course documents" to "Dokumenty predmetu"),
        "Матеріали з документ-сервера UIS" to ("Materials from the UIS document server" to "Materiály z dokumentového servera UIS"),
        "Підключи UIS, щоб бачити документи предмета." to ("Connect UIS to see course documents." to "Pripoj UIS na zobrazenie dokumentov predmetu."),
        "Документи UIS" to ("UIS documents" to "Dokumenty UIS"),
        "Не вдалося завантажити документи. Перевір з’єднання." to ("Could not load documents. Check your connection." to "Dokumenty sa nepodarilo načítať. Skontroluj pripojenie."),
        "Не вдалося відкрити або зберегти документ. Спробуй ще раз." to ("Could not open or save the document. Try again." to "Dokument sa nepodarilo otvoriť alebo uložiť. Skús to znova."),
        "У папці предмета поки немає файлів." to ("There are no files in this course folder yet." to "V priečinku predmetu zatiaľ nie sú žiadne súbory."),
        "Папку цього предмета не знайдено на UIS." to ("This course folder was not found in UIS." to "Priečinok tohto predmetu sa v UIS nenašiel."),
        "Показано частину матеріалів. Решту можна переглянути на UIS." to ("Some materials are shown. View the rest in UIS." to "Zobrazená je časť materiálov. Ostatné si môžeš pozrieť v UIS."),
        "Папка предмета в UIS" to ("Course folder in UIS" to "Priečinok predmetu v UIS"),
        "Натисни назву, щоб відкрити. Стрілка зберігає файл у папку Takt з кодом предмета." to
            ("Tap a title to open it. The download button saves it in Takt under the course code." to "Ťuknutím na názov otvoríš súbor. Tlačidlo sťahovania ho uloží do priečinka Takt pod kódom predmetu."),
        "Збережено в папку Takt" to ("Saved in Takt" to "Uložené v priečinku Takt"),
        "Не вдалося відкрити файл. Установи застосунок для цього формату." to ("Could not open the file. Install an app for this format." to "Súbor sa nepodarilo otvoriť. Nainštaluj aplikáciu pre tento formát."),
        "Підключи папку Documents/Takt у налаштуваннях, щоб відкривати й зберігати файли." to ("Connect Documents/Takt in settings to open and save files." to "Na otváranie a ukladanie súborov pripoj v nastaveniach priečinok Documents/Takt."),
        "Збережено" to ("Saved" to "Uložené"),
        "Файли" to ("Files" to "Súbory"),
        "Файл" to ("File" to "Súbor"),
    )
    fun lookup(source: String, language: AppLanguage): String? = entries[source]?.let {
        when (language) { AppLanguage.ENGLISH -> it.first; AppLanguage.SLOVAK -> it.second; else -> source }
    }
}
