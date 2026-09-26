package com.kpyruy.takt.core.ui.i18n

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kpyruy.takt.core.model.AppLanguage
import java.util.Locale

/** UI language is observable so switching it refreshes already open Compose screens. */
object TaktI18n {
    var language: AppLanguage by mutableStateOf(AppLanguage.UKRAINIAN)
        private set

    fun use(preference: AppLanguage, systemLocale: Locale = Locale.getDefault()) {
        language = when (preference) {
            AppLanguage.SYSTEM -> when (systemLocale.language) {
                "uk" -> AppLanguage.UKRAINIAN
                "sk" -> AppLanguage.SLOVAK
                else -> AppLanguage.ENGLISH
            }
            else -> preference
        }
    }

    val locale: Locale
        get() = when (language) {
            AppLanguage.SLOVAK -> Locale.forLanguageTag("sk")
            AppLanguage.ENGLISH -> Locale.ENGLISH
            else -> Locale.forLanguageTag("uk")
        }

    fun text(ukrainian: String): String = when (language) {
        AppLanguage.ENGLISH -> enTranslations[ukrainian]
            ?: dynamicTranslations.firstNotNullOfOrNull { it.translate(ukrainian, AppLanguage.ENGLISH) }
            ?: ukrainian
        AppLanguage.SLOVAK -> skTranslations[ukrainian]
            ?: dynamicTranslations.firstNotNullOfOrNull { it.translate(ukrainian, AppLanguage.SLOVAK) }
            ?: ukrainian
        else -> ukrainian
    }
}

private val placeholder = Regex("__ARG(\\d+)__")

internal class DynamicTranslation(
    source: String,
    private val english: String,
    private val slovak: String,
) {
    private val pattern = Regex(
        "^" + placeholder.split(source).joinToString("(.*?)") { Regex.escape(it) } + "$",
        RegexOption.DOT_MATCHES_ALL,
    )

    fun translate(value: String, language: AppLanguage): String? {
        val match = pattern.matchEntire(value) ?: return null
        val translated = if (language == AppLanguage.SLOVAK) slovak else english
        return placeholder.replace(translated) { marker ->
            match.groupValues[marker.groupValues[1].toInt() + 1]
        }
    }
}

fun t(ukrainian: String): String = TaktI18n.text(ukrainian)

fun tf(ukrainianFormat: String, vararg arguments: Any?): String =
    String.format(TaktI18n.locale, t(ukrainianFormat), *arguments)
