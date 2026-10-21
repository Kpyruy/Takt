package com.kpyruy.takt.core.ui.i18n

import com.kpyruy.takt.core.model.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Test

class QuantityTranslationsTest {
    @Test fun calendarCountsUseTheSelectedLanguageAndItsPluralRules() {
        assertEquals("1 class", formatLessonCount(1, AppLanguage.ENGLISH))
        assertEquals("3 classes", formatLessonCount(3, AppLanguage.ENGLISH))
        assertEquals("1 hodina", formatLessonCount(1, AppLanguage.SLOVAK))
        assertEquals("3 hodiny", formatLessonCount(3, AppLanguage.SLOVAK))
        assertEquals("5 hodín", formatLessonCount(5, AppLanguage.SLOVAK))
        assertEquals("1 пара", formatLessonCount(1, AppLanguage.UKRAINIAN))
        assertEquals("3 пари", formatLessonCount(3, AppLanguage.UKRAINIAN))
        assertEquals("11 пар", formatLessonCount(11, AppLanguage.UKRAINIAN))
        assertEquals("21 робота", formatWorkCount(21, AppLanguage.UKRAINIAN))
        assertEquals("22 роботи", formatWorkCount(22, AppLanguage.UKRAINIAN))
        assertEquals("25 робіт", formatWorkCount(25, AppLanguage.UKRAINIAN))
    }
}
