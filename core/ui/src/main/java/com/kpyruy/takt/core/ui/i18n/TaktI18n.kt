package com.kpyruy.takt.core.ui.i18n

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kpyruy.takt.core.model.*
import java.util.Locale

/** Stable resource IDs are independent of wording and the selected UI language. */
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
    internal fun lookup(key: String, language: AppLanguage): String? = staticTranslations[key]?.text(language)

    /** Unknown external diagnostics pass through; application copy must use a registered [UiKey]. */
    fun text(key: String, language: AppLanguage = this.language): String = render(key, language, 0)
    private fun render(value: String, language: AppLanguage, depth: Int): String {
        require(depth < 16) { "Localization message nesting is too deep" }
        val message = decodeUiMessage(value)
        val key = message?.key ?: value
        val template = lookup(key, language) ?: return key
        val arguments = message?.arguments.orEmpty()
        val rendered = arguments.map { argument ->
            if (argument.resource) render(argument.value, language, depth + 1) else argument.value
        }
        return formatTranslation(template, rendered)

    }
}

internal data class LocalizedText(val english: String, val ukrainian: String, val slovak: String) {
    fun text(language: AppLanguage) = when (language) {
        AppLanguage.ENGLISH -> english
        AppLanguage.UKRAINIAN -> ukrainian
        AppLanguage.SLOVAK -> slovak
    }
}
private val placeholder = Regex("__ARG(\\d+)__")
internal fun formatTranslation(template: String, arguments: List<String>): String = placeholder.replace(template) { token ->
    val index = token.groupValues[1].toInt()
    require(index in arguments.indices) { "Missing localization argument $index" }
    arguments[index]
}
fun t(key: String): String = TaktI18n.text(key)
fun tf(key: String, vararg arguments: Any?): String = t(uiMessage(key, *arguments))
fun formatDurationMinutes(minutes: Long): String {
    val hours = minutes / 60
    val remainder = minutes % 60
    return listOfNotNull(hours.takeIf { it > 0 }?.let { "$it ${t(UiKey.UNIT_HOURS_SHORT)}" },
        remainder.takeIf { it > 0 || hours == 0L }?.let { "$it ${t(UiKey.UNIT_MINUTES_SHORT)}" }).joinToString(" ")
}
