package com.kpyruy.takt.core.ui.i18n

import com.kpyruy.takt.core.model.AppLanguage
import com.kpyruy.takt.core.model.*
import org.junit.Assert.*
import org.junit.Test

class StableLocalizationIdsTest {
    @Test fun registryAndCatalogHaveExactlyTheSameUniqueIdsAndPlaceholders() {
        val ids = UiKey::class.java.declaredFields.filter { it.type == String::class.java }
            .map { it.get(null) as String }
        assertEquals(ids.size, ids.toSet().size)
        assertEquals(ids.toSet(), staticTranslations.keys)
        val tokens = Regex("__ARG\\d+__")
        staticTranslations.forEach { (id, values) ->
            assertTrue(id, id.matches(Regex("[a-z][a-z0-9_.]*")))
            val expected = tokens.findAll(values.english).map { it.value }.toSet()
            AppLanguage.entries.forEach { language ->
                val text = values.text(language)
                assertTrue("$id/$language", text.isNotBlank())
                assertEquals("$id/$language", expected, tokens.findAll(text).map { it.value }.toSet())
            }
        }
    }
    @Test fun savedMessageRendersAgainInTheNewLanguageWithoutReinterpretingUserArguments() {
        val message = uiMessage(UiKey.FORMAT_QUANTITY, "__ARG1__", uiArg(UiKey.QUANTITY_CLASS_MANY))
        assertEquals("__ARG1__ classes", TaktI18n.text(message, AppLanguage.ENGLISH))
        assertEquals("__ARG1__ hodín", TaktI18n.text(message, AppLanguage.SLOVAK))
        val userText = uiMessage(UiKey.TEXT_TEST)
        val encoded = uiMessage(UiKey.FORMAT_NOTIFICATION_TITLE, uiArg(UiKey.TEXT_EVENT), userText)
        assertTrue(TaktI18n.text(encoded, AppLanguage.ENGLISH).endsWith(userText))
        assertEquals(message, uiMessage(message))
    }
    @Test fun messageCodecPreservesUnicodeEmptyArgumentsAndSeparators() {
        val message = uiMessage(UiKey.FORMAT_QUANTITY, "a:b/\\\nфізика", "")
        assertEquals(listOf("a:b/\\\nфізика", ""), decodeUiMessage(message)!!.arguments.map { it.value })
        assertNull(decodeUiMessage("@takt.ui/%%:sbad"))
        assertNull(decodeUiMessage("@takt.ui/YQ:xYg"))
        assertNull(decodeUiMessage("ordinary user text"))
    }
    @Test fun stableIdsResolveThroughSeparateLanguageValues() {
        assertEquals("Maximum", TaktI18n.lookup("work.maximum_points", AppLanguage.ENGLISH))
        assertEquals("Максимум", TaktI18n.lookup("work.maximum_points", AppLanguage.UKRAINIAN))
        assertEquals("Maximum", TaktI18n.lookup("work.maximum_points", AppLanguage.SLOVAK))
    }
    @Test fun displaySentencesAreNotLocalizationKeys() {
        assertNull(TaktI18n.lookup("Maximum", AppLanguage.SLOVAK))
        assertNull(TaktI18n.lookup("Add a task", AppLanguage.UKRAINIAN))
    }
}
