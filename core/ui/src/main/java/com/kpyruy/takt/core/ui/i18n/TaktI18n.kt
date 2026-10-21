package com.kpyruy.takt.core.ui.i18n

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kpyruy.takt.core.model.AppLanguage
import java.util.Locale

/** UI language is observable so switching it refreshes already open Compose screens. */
object TaktI18n {
    var language: AppLanguage by mutableStateOf(AppLanguage.ENGLISH)
        private set

    fun use(preference: AppLanguage) { language = preference }

    val locale: Locale get() = locale(language)

    fun locale(language: AppLanguage): Locale = when (language) {
        AppLanguage.SLOVAK -> Locale.forLanguageTag("sk")
        AppLanguage.ENGLISH -> Locale.ENGLISH
        AppLanguage.UKRAINIAN -> Locale.forLanguageTag("uk")
    }

    internal fun lookup(source: String, language: AppLanguage): String? =
        staticTranslations[source]?.text(source, language)
            ?: dynamicTranslations.firstNotNullOfOrNull { it.translate(source, language) }

    /** Explicit language is safe for background work and never changes Compose state. */
    fun text(source: String, language: AppLanguage = this.language): String = lookup(source, language) ?: source
}

internal data class LocalizedText(
    val ukrainian: String,
    val slovak: String,
    val english: String? = null,
) {
    fun text(source: String, language: AppLanguage): String = when (language) {
        AppLanguage.UKRAINIAN -> ukrainian
        AppLanguage.SLOVAK -> slovak
        AppLanguage.ENGLISH -> english ?: source
    }
}

private val placeholder = Regex("__ARG(\\d+)__")

internal class DynamicTranslation(
    private val source: String,
    private val ukrainian: String,
    private val slovak: String,
) {
    internal val specificity: Int = placeholder.replace(source, "").length
    private val argumentIndices = placeholder.findAll(source).map { it.groupValues[1].toInt() }.toList()
    private val pattern = Regex(
        "^" + placeholder.split(source).joinToString("(.*?)") { Regex.escape(it) } + "$",
        RegexOption.DOT_MATCHES_ALL,
    )

    fun translate(value: String, language: AppLanguage): String? {
        val match = pattern.matchEntire(value) ?: return null
        val translated = when (language) {
            AppLanguage.UKRAINIAN -> ukrainian
            AppLanguage.SLOVAK -> slovak
            AppLanguage.ENGLISH -> source
        }
        return placeholder.replace(translated) { marker ->
            val group = argumentIndices.indexOf(marker.groupValues[1].toInt()) + 1
            match.groupValues[group]
        }
    }
}

fun t(source: String): String = TaktI18n.text(source)

fun formatDurationMinutes(minutes: Long): String {
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    return listOfNotNull(
        hours.takeIf { it > 0 }?.let { "$it ${t("h")}" },
        remainingMinutes.takeIf { it > 0 || hours == 0L }?.let { "$it ${t("min")}" },
    ).joinToString(" ")
}

fun tf(sourceFormat: String, vararg arguments: Any?): String =
    String.format(TaktI18n.locale, t(sourceFormat), *arguments)
