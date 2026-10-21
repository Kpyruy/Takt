package com.kpyruy.takt.core.ui.i18n

import com.kpyruy.takt.core.model.UiKey
import com.kpyruy.takt.core.model.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Test

class WallpaperTranslationsTest {
    @Test fun imagePickerAndFailureCopyHaveEnglishAndSlovakTranslations() {
        assertEquals("Choose your photo", TaktI18n.lookup(UiKey.TEXT_CHOOSE_YOUR_PHOTO, AppLanguage.ENGLISH))
        assertEquals("Vybrať vlastnú fotografiu", TaktI18n.lookup(UiKey.TEXT_CHOOSE_YOUR_PHOTO, AppLanguage.SLOVAK))
        assertEquals("Use built-in background", TaktI18n.lookup(UiKey.TEXT_USE_BUILT_IN_BACKGROUND, AppLanguage.ENGLISH))
        assertEquals("Fotografiu sa nepodarilo otvoriť. Vyber iný obrázok do 24 MB a 64 Mpx.",
            TaktI18n.lookup(UiKey.TEXT_COULD_NOT_OPEN_THIS_PHOTO_CHOOSE_ANOTHER_IMAGE_UP_TO_24_MB_AND_64_MP, AppLanguage.SLOVAK))
    }
}
