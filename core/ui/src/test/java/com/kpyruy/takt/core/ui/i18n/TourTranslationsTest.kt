package com.kpyruy.takt.core.ui.i18n

import com.kpyruy.takt.core.model.AppLanguage
import com.kpyruy.takt.core.ui.components.CourseIcons
import org.junit.Assert.*
import org.junit.Test

class TourTranslationsTest {
    @Test fun renderedSlovakSampleNamesAndFinalControlNeverFallBackToUkrainian() {
        val original = TaktI18n.language
        try {
            TaktI18n.use(AppLanguage.SLOVAK)
            assertEquals("Fyzika", t("Physics"))
            assertEquals("Dokončiť", t("Finish"))
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
            assertEquals("Tour of Takt", t("Tour of Takt"))
            assertEquals("180 icons · pick your own", tf("%d icons · pick your own", 180))
            TaktI18n.use(AppLanguage.SLOVAK)
            assertEquals("Tutoriál je dokončený", t("Tutorial completed"))
            assertEquals("180 ikon · vyber si svoju", tf("%d icons · pick your own", 180))
        } finally { TaktI18n.use(original) }
    }
}
