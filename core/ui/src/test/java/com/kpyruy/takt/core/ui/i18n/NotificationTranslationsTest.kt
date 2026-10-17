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
        val keys = ReminderCategory.entries.map { it.label } + listOf("Сповіщення", "Навчальна робота й задачі", "Події й нагадування")
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
        assertEquals("Android permissions", t("Дозволи Android"))
        TaktI18n.use(AppLanguage.SLOVAK)
        assertEquals("Lokálne pripomienky", t("Локальні нагадування"))
    }
}
