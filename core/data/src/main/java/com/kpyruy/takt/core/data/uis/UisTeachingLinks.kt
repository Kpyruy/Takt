package com.kpyruy.takt.core.data.uis

import java.net.URI

internal object UisTeachingLinks {
    private val base = URI("https://is.stuba.sk/auth/katalog/syllabus.pl")
    fun personId(href: String): String? = id(href, "/auth/lide/clovek.pl", "id")
    fun subjectId(href: String): String? = id(href, "/auth/katalog/syllabus.pl", "predmet")
    fun validId(id: String) = id.matches(Regex("[1-9][0-9]{0,11}"))
    private fun id(href: String, path: String, field: String): String? = runCatching {
        val uri = base.resolve(href)
        if (uri.scheme != "https" || !uri.host.equals("is.stuba.sk", true) || uri.userInfo != null ||
            uri.port !in listOf(-1, 443) || uri.path != path) return null
        val values = uri.rawQuery.orEmpty().split(';', '&').filter { it.startsWith("$field=") }
        values.singleOrNull()?.substringAfter('=')?.takeIf(::validId)
    }.getOrNull()
}
