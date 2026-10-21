package com.kpyruy.takt.core.ui.i18n

import com.kpyruy.takt.core.model.uiMessage
import com.kpyruy.takt.core.model.UiKey
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
        assertEquals("Timetable", t(UiKey.TEXT_TIMETABLE))
        assertEquals("Class", t(UiKey.TEXT_CLASS))
        assertEquals("Lab", t(UiKey.TEXT_LAB))
        assertEquals("Mon", t(UiKey.TEXT_MON))
        assertEquals("Semester 5", t(uiMessage(UiKey.FORMAT_SEMESTER_VALUE, "5")))
        assertEquals("12 points earned, up to 8 still available", t(uiMessage(UiKey.FORMAT_VALUE_POINTS_EARNED_UP_TO_VALUE_STILL_AVAILABLE, "12", "8")))

        TaktI18n.use(AppLanguage.SLOVAK)
        assertEquals("Rozvrh", t(UiKey.TEXT_TIMETABLE))
        assertEquals("Skúška", t(UiKey.TEXT_EXAM))
        assertEquals("Cvičenie", t(UiKey.TEXT_PRACTICAL))
        assertEquals("Po", t(UiKey.TEXT_MON))
        assertEquals("5. semester", t(uiMessage(UiKey.FORMAT_SEMESTER_VALUE, "5")))
        assertEquals("Získané body: 12, dostupných je ešte 8", t(uiMessage(UiKey.FORMAT_VALUE_POINTS_EARNED_UP_TO_VALUE_STILL_AVAILABLE, "12", "8")))
    }

    @Test fun newInstallDefaultsToEnglish() {
        assertEquals(AppLanguage.ENGLISH, AppSettings().language)
        TaktI18n.use(AppSettings().language)
        assertEquals("Today", t(UiKey.TEXT_TODAY))
    }

    @Test fun userContentIsLeftUntouched() {
        TaktI18n.use(AppLanguage.ENGLISH)
        assertEquals("TPAR_6B", t("TPAR_6B"))
    }

    @Test fun semesterChoicesTranslateTheSeasonWithoutChangingPlainSemesterLabels() {
        TaktI18n.use(AppLanguage.ENGLISH)
        assertEquals("Semester 5 · winter", t(uiMessage(UiKey.FORMAT_SEMESTER_VALUE_WINTER, "5")))
        assertEquals("Semester 6 · summer", t(uiMessage(UiKey.FORMAT_SEMESTER_VALUE_SUMMER, "6")))
        assertEquals("Semester 5", t(uiMessage(UiKey.FORMAT_SEMESTER_VALUE, "5")))
        assertEquals("All semesters", t(UiKey.TEXT_ALL_SEMESTERS))

        TaktI18n.use(AppLanguage.SLOVAK)
        assertEquals("5. semester · zimný", t(uiMessage(UiKey.FORMAT_SEMESTER_VALUE_WINTER, "5")))
        assertEquals("6. semester · letný", t(uiMessage(UiKey.FORMAT_SEMESTER_VALUE_SUMMER, "6")))
        assertEquals("5. semester", t(uiMessage(UiKey.FORMAT_SEMESTER_VALUE, "5")))
        assertEquals("Všetky semestre", t(UiKey.TEXT_ALL_SEMESTERS))

        val note = "Зимовий план курсу TPAR_6B"
        listOf(AppLanguage.ENGLISH, AppLanguage.SLOVAK).forEach { language ->
            TaktI18n.use(language)
            assertEquals(note, t(note))
            assertEquals("TPAR_6B", t("TPAR_6B"))
        }
    }

    @Test fun gradingInstructionsAndMinimumPointChipsUseTheChosenLanguage() {
        TaktI18n.use(AppLanguage.ENGLISH)
        assertEquals("Counts toward the final grade", t(UiKey.TEXT_COUNTS_TOWARD_THE_FINAL_GRADE))
        assertEquals("Minimum percentage", t(UiKey.TEXT_MINIMUM_PERCENTAGE))
        assertEquals("Below the E threshold", t(UiKey.TEXT_BELOW_THE_E_THRESHOLD))
        assertEquals("Thresholds apply to points that count toward the final grade.",
            t(UiKey.TEXT_THRESHOLDS_APPLY_TO_POINTS_THAT_COUNT_TOWARD_THE_FINAL_GRADE))
        assertEquals("Minimum 5 pts", t(UiKey.TEXT_MINIMUM_3E72FBEC) + "5" + t(UiKey.TEXT_PTS_77846466))
        assertEquals("pts", t(UiKey.TEXT_PTS))

        TaktI18n.use(AppLanguage.SLOVAK)
        assertEquals("Započítava sa do výslednej známky", t(UiKey.TEXT_COUNTS_TOWARD_THE_FINAL_GRADE))
        assertEquals("Minimálne percento", t(UiKey.TEXT_MINIMUM_PERCENTAGE))
        assertEquals("Pod hranicou E", t(UiKey.TEXT_BELOW_THE_E_THRESHOLD))
        assertEquals("Hranice sa vzťahujú na body započítané do výslednej známky.",
            t(UiKey.TEXT_THRESHOLDS_APPLY_TO_POINTS_THAT_COUNT_TOWARD_THE_FINAL_GRADE))
        assertEquals("Minimum 5 b.", t(UiKey.TEXT_MINIMUM_3E72FBEC) + "5" + t(UiKey.TEXT_PTS_77846466))
        assertEquals("b.", t(UiKey.TEXT_PTS))
    }

    @Test fun staticCatalogsCoverTheSamePhrases() {
        assertTrue(staticTranslations.isNotEmpty())
        staticTranslations.forEach { (source, localized) ->
            assertTrue("Non-English source key: $source", source.none { it in '\u0400'..'\u04ff' })
            assertTrue("Missing Ukrainian translation for $source", localized.ukrainian.isNotBlank())
            assertTrue("Missing Slovak translation for $source", localized.slovak.isNotBlank())
            assertTrue("Cyrillic in English translation for $source",
                localized.text(AppLanguage.ENGLISH).none { it in '\u0400'..'\u04ff' })
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
        assertEquals("Розклад", t(UiKey.TEXT_TIMETABLE))
        assertEquals("5 семестр · зимовий", t(uiMessage(UiKey.FORMAT_SEMESTER_VALUE_WINTER, "5")))
        val translatedTemplate = LocalizedText("Item __ARG1__: __ARG0__", "__ARG0__ для __ARG1__", "__ARG0__ pre __ARG1__")
        assertEquals("value pre name", formatTranslation(translatedTemplate.text(AppLanguage.SLOVAK), listOf("value", "name")))
    }

    @Test fun specificDynamicTemplatesWinOverGenericOnes() {
        TaktI18n.use(AppLanguage.SLOVAK)
        assertEquals("5. semester · zimný", t(uiMessage(UiKey.FORMAT_SEMESTER_VALUE_WINTER, "5")))
        assertEquals("Získané body: 12, dostupných je ešte 8", t(uiMessage(UiKey.FORMAT_VALUE_POINTS_EARNED_UP_TO_VALUE_STILL_AVAILABLE, "12", "8")))
        assertEquals("Dokončené: 2 z 4", t(uiMessage(UiKey.FORMAT_VALUE_OF_VALUE_COMPLETED, "2", "4")))
    }
}
