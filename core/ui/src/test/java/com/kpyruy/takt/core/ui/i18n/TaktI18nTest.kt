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

    @Test fun semesterChoicesTranslateTheSeasonWithoutChangingPlainSemesterLabels() {
        TaktI18n.use(AppLanguage.ENGLISH)
        assertEquals("Semester 5 · winter", t("5 семестр · зимовий"))
        assertEquals("Semester 6 · summer", t("6 семестр · літній"))
        assertEquals("Semester 5", t("5 семестр"))
        assertEquals("All semesters", t("Усі семестри"))

        TaktI18n.use(AppLanguage.SLOVAK)
        assertEquals("5. semester · zimný", t("5 семестр · зимовий"))
        assertEquals("6. semester · letný", t("6 семестр · літній"))
        assertEquals("5. semester", t("5 семестр"))
        assertEquals("Všetky semestre", t("Усі семестри"))

        val note = "Зимовий план курсу TPAR_6B"
        listOf(AppLanguage.ENGLISH, AppLanguage.SLOVAK).forEach { language ->
            TaktI18n.use(language)
            assertEquals(note, t(note))
            assertEquals("TPAR_6B", t("TPAR_6B"))
        }
    }

    @Test fun gradingInstructionsAndMinimumPointChipsUseTheChosenLanguage() {
        TaktI18n.use(AppLanguage.ENGLISH)
        assertEquals("Counts toward the final grade", t("Входить у підсумок"))
        assertEquals("Minimum percentage", t("Мінімальний відсоток"))
        assertEquals("Below the E threshold", t("Нижче межі E"))
        assertEquals("Thresholds apply to points that count toward the final grade.",
            t("Пороги застосовуються до балів, що входять у підсумок."))
        assertEquals("Minimum 5 pts", t("Мінімум ") + "5" + t(" б."))
        assertEquals("pts", t("б."))

        TaktI18n.use(AppLanguage.SLOVAK)
        assertEquals("Započítava sa do výslednej známky", t("Входить у підсумок"))
        assertEquals("Minimálne percento", t("Мінімальний відсоток"))
        assertEquals("Pod hranicou E", t("Нижче межі E"))
        assertEquals("Hranice sa vzťahujú na body započítané do výslednej známky.",
            t("Пороги застосовуються до балів, що входять у підсумок."))
        assertEquals("Minimum 5 b.", t("Мінімум ") + "5" + t(" б."))
        assertEquals("b.", t("б."))
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
