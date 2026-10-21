package com.kpyruy.takt.core.ui.i18n

import com.kpyruy.takt.core.model.UiKey
import com.kpyruy.takt.core.model.AppLanguage
import com.kpyruy.takt.core.ui.components.CourseIcons
import org.junit.Assert.*
import org.junit.Test

class TourTranslationsTest {
    @Test fun renderedSlovakSampleNamesAndFinalControlNeverFallBackToUkrainian() {
        val original = TaktI18n.language
        try {
            TaktI18n.use(AppLanguage.SLOVAK)
            assertEquals("Fyzika", t(UiKey.TEXT_PHYSICS))
            assertEquals("Dokončiť", t(UiKey.TEXT_FINISH))
        } finally { TaktI18n.use(original) }
    }
    @Test fun everyNewIconHasEnglishAndSlovakSearchableMetadata() {
        for (language in listOf(AppLanguage.ENGLISH, AppLanguage.SLOVAK)) {
            CourseIcons.all.drop(110).forEach {
                val text = TaktI18n.lookup(it.label, language)
                assertNotNull("${it.key}: $language", text)
                assertFalse(text!!.contains(Regex("[А-Яа-яІіЇїЄє]")))
            }
        }
    }
    @Test fun tourControlsAndIconCountAreLocalized() {
        val original = TaktI18n.language
        try {
            TaktI18n.use(AppLanguage.ENGLISH)
            assertEquals("Tour of Takt", t(UiKey.TEXT_TOUR_OF_TAKT))
            assertEquals("180 icons · pick your own", tf(UiKey.FORMAT_D_ICONS_PICK_YOUR_OWN, 180))
            TaktI18n.use(AppLanguage.SLOVAK)
            assertEquals("Tutoriál je dokončený", t(UiKey.TEXT_TUTORIAL_COMPLETED))
            assertEquals("180 ikon · vyber si svoju", tf(UiKey.FORMAT_D_ICONS_PICK_YOUR_OWN, 180))
        } finally { TaktI18n.use(original) }
    }
}
