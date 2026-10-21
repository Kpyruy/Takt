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
        assertEquals("Timetable", t("Timetable"))
        assertEquals("Class", t("Class"))
        assertEquals("Lab", t("Lab"))
        assertEquals("Mon", t("Mon"))
        assertEquals("Semester 5", t("Semester 5"))
        assertEquals("12 points earned, up to 8 still available", t("12 points earned, up to 8 still available"))

        TaktI18n.use(AppLanguage.SLOVAK)
        assertEquals("Rozvrh", t("Timetable"))
        assertEquals("Skúška", t("Exam"))
        assertEquals("Cvičenie", t("Practical"))
        assertEquals("Po", t("Mon"))
        assertEquals("5. semester", t("Semester 5"))
        assertEquals("Získané body: 12, dostupných je ešte 8", t("12 points earned, up to 8 still available"))
    }

    @Test fun newInstallDefaultsToEnglish() {
        assertEquals(AppLanguage.ENGLISH, AppSettings().language)
        TaktI18n.use(AppSettings().language)
        assertEquals("Today", t("Today"))
    }

    @Test fun userContentIsLeftUntouched() {
        TaktI18n.use(AppLanguage.ENGLISH)
        assertEquals("TPAR_6B", t("TPAR_6B"))
    }

    @Test fun semesterChoicesTranslateTheSeasonWithoutChangingPlainSemesterLabels() {
        TaktI18n.use(AppLanguage.ENGLISH)
        assertEquals("Semester 5 · winter", t("Semester 5 · winter"))
        assertEquals("Semester 6 · summer", t("Semester 6 · summer"))
        assertEquals("Semester 5", t("Semester 5"))
        assertEquals("All semesters", t("All semesters"))

        TaktI18n.use(AppLanguage.SLOVAK)
        assertEquals("5. semester · zimný", t("Semester 5 · winter"))
        assertEquals("6. semester · letný", t("Semester 6 · summer"))
        assertEquals("5. semester", t("Semester 5"))
        assertEquals("Všetky semestre", t("All semesters"))

        val note = "Зимовий план курсу TPAR_6B"
        listOf(AppLanguage.ENGLISH, AppLanguage.SLOVAK).forEach { language ->
            TaktI18n.use(language)
            assertEquals(note, t(note))
            assertEquals("TPAR_6B", t("TPAR_6B"))
        }
    }

    @Test fun gradingInstructionsAndMinimumPointChipsUseTheChosenLanguage() {
        TaktI18n.use(AppLanguage.ENGLISH)
        assertEquals("Counts toward the final grade", t("Counts toward the final grade"))
        assertEquals("Minimum percentage", t("Minimum percentage"))
        assertEquals("Below the E threshold", t("Below the E threshold"))
        assertEquals("Thresholds apply to points that count toward the final grade.",
            t("Thresholds apply to points that count toward the final grade."))
        assertEquals("Minimum 5 pts", t("Minimum ") + "5" + t(" pts"))
        assertEquals("pts", t("pts"))

        TaktI18n.use(AppLanguage.SLOVAK)
        assertEquals("Započítava sa do výslednej známky", t("Counts toward the final grade"))
        assertEquals("Minimálne percento", t("Minimum percentage"))
        assertEquals("Pod hranicou E", t("Below the E threshold"))
        assertEquals("Hranice sa vzťahujú na body započítané do výslednej známky.",
            t("Thresholds apply to points that count toward the final grade."))
        assertEquals("Minimum 5 b.", t("Minimum ") + "5" + t(" pts"))
        assertEquals("b.", t("pts"))
    }

    @Test fun staticCatalogsCoverTheSamePhrases() {
        assertTrue(staticTranslations.isNotEmpty())
        staticTranslations.forEach { (source, localized) ->
            assertTrue("Non-English source key: $source", source.none { it in '\u0400'..'\u04ff' })
            assertTrue("Missing Ukrainian translation for $source", localized.ukrainian.isNotBlank())
            assertTrue("Missing Slovak translation for $source", localized.slovak.isNotBlank())
            assertTrue("Cyrillic in English translation for $source",
                localized.text(source, AppLanguage.ENGLISH).none { it in '\u0400'..'\u04ff' })
            assertTrue("Cyrillic in Slovak translation for $source",
                localized.slovak.none { it in '\u0400'..'\u04ff' })
        }
    }

    @Test fun breakDurationIncludesLocalizedHoursAndOmitsZeroMinutes() {
        TaktI18n.use(AppLanguage.UKRAINIAN)
        assertEquals("5 год 10 хв", formatDurationMinutes(310))
        TaktI18n.use(AppLanguage.ENGLISH)
        assertEquals("5 h 10 min", formatDurationMinutes(310))
        TaktI18n.use(AppLanguage.SLOVAK)
        assertEquals("5 h", formatDurationMinutes(300))
    }
    @Test fun englishSourceSupportsUkrainianAndKeepsReorderedArgumentsBoundToTheirNumbers() {
        TaktI18n.use(AppLanguage.UKRAINIAN)
        assertEquals("Розклад", t("Timetable"))
        assertEquals("5 семестр · зимовий", t("Semester 5 · winter"))
        val translation = DynamicTranslation(
            "Item __ARG1__: __ARG0__",
            "__ARG0__ для __ARG1__",
            "__ARG0__ pre __ARG1__",
        )
        assertEquals("value pre name", translation.translate("Item name: value", AppLanguage.SLOVAK))
    }

    @Test fun specificDynamicTemplatesWinOverGenericOnes() {
        TaktI18n.use(AppLanguage.SLOVAK)
        assertEquals("5. semester · zimný", t("Semester 5 · winter"))
        assertEquals("Získané body: 12, dostupných je ešte 8", t("12 points earned, up to 8 still available"))
        assertEquals("Dokončené: 2 z 4", t("2 of 4 completed"))
    }
}
