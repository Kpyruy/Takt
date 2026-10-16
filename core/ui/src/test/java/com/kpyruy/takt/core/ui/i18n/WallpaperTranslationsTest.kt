package com.kpyruy.takt.core.ui.i18n

import com.kpyruy.takt.core.model.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Test

class WallpaperTranslationsTest {
    @Test fun imagePickerAndFailureCopyHaveEnglishAndSlovakTranslations() {
        assertEquals("Choose your photo", WallpaperTranslations.lookup("Обрати своє фото", AppLanguage.ENGLISH))
        assertEquals("Vybrať vlastnú fotografiu", WallpaperTranslations.lookup("Обрати своє фото", AppLanguage.SLOVAK))
        assertEquals("Use built-in background", WallpaperTranslations.lookup("Вбудований фон", AppLanguage.ENGLISH))
        assertEquals("Fotografiu sa nepodarilo otvoriť. Vyber iný obrázok do 24 MB a 64 Mpx.",
            WallpaperTranslations.lookup("Не вдалося відкрити фото. Обери інше зображення до 24 МБ і 64 Мп.", AppLanguage.SLOVAK))
    }
}
