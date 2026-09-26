package com.kpyruy.takt.core.ui.i18n

import com.kpyruy.takt.core.model.AppLanguage
import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaktI18nTest {
    @After fun resetLanguage() = TaktI18n.use(AppLanguage.UKRAINIAN)

    @Test fun chosenLanguageTranslatesStaticAndDynamicText() {
        TaktI18n.use(AppLanguage.ENGLISH)
        assertEquals("Class", t("Пара"))
        assertEquals("Lab", t("Лабораторна"))
        assertEquals("Mon", t("Пн"))
        assertEquals("Semester 5", t("5 семестр"))
        assertEquals("12 points earned, up to 8 still available", t("12 балів набрано, до 8 ще доступно"))

        TaktI18n.use(AppLanguage.SLOVAK)
        assertEquals("Skúška", t("Екзамен"))
        assertEquals("Cvičenie", t("Практика"))
        assertEquals("Po", t("Пн"))
        assertEquals("5. semester", t("5 семестр"))
        assertEquals("Získané body: 12, dostupných je ešte 8", t("12 балів набрано, до 8 ще доступно"))
    }

    @Test fun systemLanguageUsesSupportedLocaleAndFallsBackToEnglish() {
        TaktI18n.use(AppLanguage.SYSTEM, Locale.forLanguageTag("sk-SK"))
        assertEquals("Dnes", t("Сьогодні"))
        TaktI18n.use(AppLanguage.SYSTEM, Locale.forLanguageTag("de-DE"))
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
}
