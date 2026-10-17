package com.kpyruy.takt.core.ui.i18n

import com.kpyruy.takt.core.model.AppLanguage

internal object WallpaperTranslations {
    private val entries = mapOf(
        "Стандартний фон" to ("Built-in background" to "Vstavané pozadie"),
        "Фон" to ("Background" to "Pozadie"),
        "Твій простір" to ("Your space" to "Tvoj priestor"),
        "Твій день у власному настрої" to ("Your day, your atmosphere" to "Tvoj deň, tvoja atmosféra"),
        "Фото зберігається на цьому пристрої. Зміни набудуть чинності після застосування." to
            ("The photo is saved on this device. Apply to use your changes." to "Fotografia sa ukladá v tomto zariadení. Zmeny sa prejavia po použití."),
        "Підготовка фото…" to ("Preparing photo…" to "Pripravuje sa fotografia…"),
        "Обрати своє фото" to ("Choose your photo" to "Vybrať vlastnú fotografiu"),
        "Вбудований фон" to ("Use built-in background" to "Použiť vstavané pozadie"),
        "Не вдалося відкрити фото. Обери інше зображення до 24 МБ і 64 Мп." to
            ("Could not open this photo. Choose another image up to 24 MB and 64 MP." to "Fotografiu sa nepodarilo otvoriť. Vyber iný obrázok do 24 MB a 64 Mpx."),
    )
    fun lookup(source: String, language: AppLanguage): String? = entries[source]?.let {
        when (language) { AppLanguage.ENGLISH -> it.first; AppLanguage.SLOVAK -> it.second; else -> source }
    }
}
