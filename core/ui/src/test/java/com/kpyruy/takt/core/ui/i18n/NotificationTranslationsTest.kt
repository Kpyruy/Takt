package com.kpyruy.takt.core.ui.i18n

import com.kpyruy.takt.core.model.*
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test
import org.junit.After

class NotificationTranslationsTest {
    private val originalLanguage = TaktI18n.language
    @After fun restoreUiLanguage() { TaktI18n.use(originalLanguage) }
    @Test fun backgroundLanguageDoesNotMutateGlobalUiLanguage() {
        TaktI18n.use(AppLanguage.UKRAINIAN)
        assertEquals("Test: Algebra", NotificationTranslations.title(ReminderCategory.TEST, "Algebra", AppLanguage.ENGLISH))
        assertEquals("Udalosť: Chess", NotificationTranslations.title(ReminderCategory.EVENT, "Chess", AppLanguage.SLOVAK))
        assertEquals(AppLanguage.UKRAINIAN, TaktI18n.language)
    }
    @Test fun allCategoriesAndChannelsHaveEnglishAndSlovakText() {
        val keys = ReminderCategory.entries.map { it.label } + listOf("Notifications", "Academic work and tasks", "Events and reminders")
        keys.forEach { key ->
            assertNotNull(NotificationTranslations.lookup(key, AppLanguage.ENGLISH))
            assertNotNull(NotificationTranslations.lookup(key, AppLanguage.SLOVAK))
        }
        val body = NotificationTranslations.body(Instant.parse("2026-10-22T07:00:00Z"), true,
            "Algebra", AppLanguage.SLOVAK, ZoneId.of("Europe/Bratislava"))
        assertTrue(body.startsWith("Algebra · Termín:"))
        assertFalse(body.contains("09:00")) // Date-only work must not acquire an invented due-time.
        assertTrue(NotificationTranslations.body(Instant.parse("2026-10-22T07:00:00Z"), false,
            "Algebra", AppLanguage.SLOVAK, ZoneId.of("Europe/Bratislava")).endsWith("09:00"))
    }
    @Test fun newUiStringsAreRegistered() {
        TaktI18n.use(AppLanguage.ENGLISH)
        assertEquals("Android permissions", t("Android permissions"))
        TaktI18n.use(AppLanguage.SLOVAK)
        assertEquals("Lokálne pripomienky", t("Local reminders"))
    }
    @Test fun selectedBackgroundLanguageControlsLessonPrefixAndDateDespiteUiAndDeviceLanguage() {
        val deviceLocale = java.util.Locale.getDefault()
        try {
            java.util.Locale.setDefault(java.util.Locale.forLanguageTag("uk"))
            TaktI18n.use(AppLanguage.UKRAINIAN)
            val instant = Instant.parse("2026-10-22T07:00:00Z")
            val zone = ZoneId.of("Europe/Bratislava")
            for ((language, prefix) in listOf(
                AppLanguage.ENGLISH to "Lesson: ",
                AppLanguage.SLOVAK to "Vyučovanie: ",
            )) {
                assertEquals(prefix + "Algebra", NotificationTranslations.title(ReminderCategory.LESSON, "Algebra", language))
                val body = NotificationTranslations.body(instant, false, null, language, zone)
                assertTrue(body, body.endsWith("09:00"))
                assertFalse(body, body.any { it in '\u0400'..'\u04ff' })
                assertTrue(body, body.startsWith(if (language == AppLanguage.ENGLISH) "Starts: " else "Začiatok: "))
            }
            assertEquals(AppLanguage.UKRAINIAN, TaktI18n.language)
        } finally {
            java.util.Locale.setDefault(deviceLocale)
        }
    }

    @Test fun ukrainianSelectionAndUserWrittenTitlesStayUkrainian() {
        assertEquals("Заняття: Algebra", NotificationTranslations.title(ReminderCategory.LESSON, "Algebra", AppLanguage.UKRAINIAN))
        assertEquals("Lesson: Моя назва", NotificationTranslations.title(ReminderCategory.LESSON, "Моя назва", AppLanguage.ENGLISH))
        assertEquals("Pripomienka: Chess", NotificationTranslations.title(ReminderCategory.REMINDER, "Chess", AppLanguage.SLOVAK))
    }
}
