package com.kpyruy.takt.core.model

import java.util.Base64

/** String-backed message references survive saved state without storing a rendered UI language. */
data class UiMessageArgument(val value: String, val resource: Boolean = false)
data class UiMessageValue(val key: String, val arguments: List<UiMessageArgument>)
private const val UI_MESSAGE_PREFIX = "@takt.ui/"
private fun encodePart(value: String) = Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(Charsets.UTF_8))
private fun decodePart(value: String) = String(Base64.getUrlDecoder().decode(value), Charsets.UTF_8)

fun uiMessage(key: String, vararg arguments: Any?): String {
    if (key.startsWith(UI_MESSAGE_PREFIX) && arguments.isEmpty()) return key
    return UI_MESSAGE_PREFIX + encodePart(key) + arguments.joinToString("") { value ->
        val argument = value as? UiMessageArgument ?: UiMessageArgument(value.toString())
        ":" + (if (argument.resource) "m" else "s") + encodePart(argument.value)
    }
}

/** Explicitly mark a nested resource; ordinary user-written arguments are never interpreted as keys. */
fun uiArg(key: String, vararg arguments: Any?) = UiMessageArgument(uiMessage(key, *arguments), resource = true)

fun decodeUiMessage(value: String): UiMessageValue? {
    if (!value.startsWith(UI_MESSAGE_PREFIX)) return null
    return runCatching {
        val pieces = value.removePrefix(UI_MESSAGE_PREFIX).split(':')
        UiMessageValue(decodePart(pieces.first()), pieces.drop(1).map { part ->
            require(part.isNotEmpty() && part[0] in "sm")
            UiMessageArgument(decodePart(part.drop(1)), part[0] == 'm')
        })
    }.getOrNull()
}
