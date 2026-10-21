package com.kpyruy.takt.core.ui.i18n

import com.kpyruy.takt.core.model.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Test

class WallpaperTranslationsTest {
    @Test fun imagePickerAndFailureCopyHaveEnglishAndSlovakTranslations() {
        assertEquals("Choose your photo", TaktI18n.lookup("Choose your photo", AppLanguage.ENGLISH))
        assertEquals("Vybrať vlastnú fotografiu", TaktI18n.lookup("Choose your photo", AppLanguage.SLOVAK))
        assertEquals("Use built-in background", TaktI18n.lookup("Use built-in background", AppLanguage.ENGLISH))
        assertEquals("Fotografiu sa nepodarilo otvoriť. Vyber iný obrázok do 24 MB a 64 Mpx.",
            TaktI18n.lookup("Could not open this photo. Choose another image up to 24 MB and 64 MP.", AppLanguage.SLOVAK))
    }
}
