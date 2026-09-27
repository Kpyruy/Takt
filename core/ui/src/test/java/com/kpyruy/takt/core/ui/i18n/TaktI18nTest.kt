package com.kpyruy.takt.core.ui.i18n

import com.kpyruy.takt.core.model.AppLanguage
import com.kpyruy.takt.core.model.AppSettings
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaktI18nTest {
    @After fun resetLanguage() = TaktI18n.use(AppLanguage.ENGLISH)

    @Test fun chosenLanguageTranslatesStaticAndDynamicText() {
        TaktI18n.use(AppLanguage.ENGLISH)
        assertEquals("Timetable", t("Розклад"))
        assertEquals("Class", t("Пара"))
        assertEquals("Lab", t("Лабораторна"))
        assertEquals("Mon", t("Пн"))
        assertEquals("Semester 5", t("5 семестр"))
        assertEquals("12 points earned, up to 8 still available", t("12 балів набрано, до 8 ще доступно"))

        TaktI18n.use(AppLanguage.SLOVAK)
        assertEquals("Rozvrh", t("Розклад"))
        assertEquals("Skúška", t("Екзамен"))
        assertEquals("Cvičenie", t("Практика"))
        assertEquals("Po", t("Пн"))
        assertEquals("5. semester", t("5 семестр"))
        assertEquals("Získané body: 12, dostupných je ešte 8", t("12 балів набрано, до 8 ще доступно"))
    }

    @Test fun newInstallDefaultsToEnglish() {
        assertEquals(AppLanguage.ENGLISH, AppSettings().language)
        TaktI18n.use(AppSettings().language)
        assertEquals("Today", t("Сьогодні"))
    }

    @Test fun userContentIsLeftUntouched() {
        TaktI18n.use(AppLanguage.ENGLISH)
        assertEquals("TPAR_6B", t("TPAR_6B"))
    }

    @Test fun staticCatalogsCoverTheSamePhrases() {
        assertTrue(enTranslations.isNotEmpty())
        assertEquals(enTranslations.keys, skTranslations.keys)
    }

    @Test fun breakDurationIncludesLocalizedHoursAndOmitsZeroMinutes() {
        TaktI18n.use(AppLanguage.UKRAINIAN)
        assertEquals("5 год 10 хв", formatDurationMinutes(310))
        TaktI18n.use(AppLanguage.ENGLISH)
        assertEquals("5 h 10 min", formatDurationMinutes(310))
        TaktI18n.use(AppLanguage.SLOVAK)
        assertEquals("5 h", formatDurationMinutes(300))
    }
}
