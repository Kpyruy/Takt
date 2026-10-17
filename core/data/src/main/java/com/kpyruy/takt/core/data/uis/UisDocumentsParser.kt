package com.kpyruy.takt.core.data.uis

import com.kpyruy.takt.core.model.CourseDocumentFile
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.net.URLDecoder
import java.util.Locale

/** Parses the already-present hidden document tree, without changing UIS tree preferences. */
internal object UisDocumentsParser {
    private val base = "https://is.stuba.sk/auth/dok_server/".toHttpUrl()
    internal data class Folder(val id: String, val label: String, val path: String, val code: String? = null)
    internal data class Link(val id: String, val documentId: String?, val url: HttpUrl, val parameters: Map<String, String>)
    internal data class FolderPage(val files: List<CourseDocumentFile>, val subfolders: List<Folder>, val pages: List<HttpUrl>)

    fun courseFolders(html: String, activeCodes: Set<String>, origin: HttpUrl = base): List<Folder> {
        val doc = Jsoup.parse(html)
        return doc.select("a[href]").mapNotNull { anchor ->
            val label = anchor.text().replace('\u00a0', ' ').trim()
            val code = label.substringBefore(' ')
            if (code !in activeCodes) return@mapNotNull null
            val link = link(anchor.attr("href"), origin) ?: return@mapNotNull null
            if (link.documentId != null || link.parameters.keys.any { it !in setOf("id", "ds", "lang", "on") }) return@mapNotNull null
            val parents = anchor.parents().filter { it.tagName() == "li" }.reversed().mapNotNull { li ->
                li.children().firstOrNull { it.hasClass("node-container") }
                    ?.selectFirst(".node-label a[href]")?.text()?.trim()
            }.filter { it != label }
            Folder(link.id, label, (parents + label).joinToString(" / "), code)
        }.distinctBy { it.id }
    }

    fun folder(html: String, folder: Folder, origin: HttpUrl = base): FolderPage {
        val doc = Jsoup.parse(html)
        val files = mutableListOf<CourseDocumentFile>()
        val children = mutableListOf<Folder>()
        for (table in doc.select("table")) {
            val headers = table.select("tr").firstOrNull { it.select("th").isNotEmpty() }
                ?.select("th")?.map { normalize(it.text()) }.orEmpty()
            val nameColumn = headers.indexOfFirst { it in setOf("name", "nazov", "nazev") }
            if (nameColumn < 0) continue
            val childTable = headers.any { it in setOf("enter folder", "vstup do zlozky", "vstup do slozky") }
            for (row in table.select("tr")) {
                val cells = row.children().filter { it.tagName() == "td" }
                if (cells.size <= nameColumn) continue
                val title = cells[nameColumn].text().replace('\u00a0', ' ').trim()
                for (anchor in row.select("a[href]")) {
                    val target = link(anchor.attr("href"), origin) ?: continue
                    if (target.documentId != null && target.id == folder.id && title.isNotBlank()) {
                        val ext = anchor.selectFirst("[data-mime]")?.attr("data-mime")?.lowercase(Locale.ROOT)
                        val mime = mimeType(ext)
                        val suggested = anchor.attr("download").takeIf { it.isNotBlank() }
                            ?: (title + extension(mime).takeUnless { title.lowercase(Locale.ROOT).endsWith(it) }.orEmpty())
                        val sizeColumn = headers.indexOfFirst { it in setOf("size", "velkost", "velikost") }
                        files += CourseDocumentFile(folder.id, target.documentId, title, mime,
                            suggested, cells.getOrNull(sizeColumn)?.text()?.takeIf { it.isNotBlank() }, folder.path)
                    } else if (childTable && target.documentId == null && target.id != folder.id &&
                        target.parameters.keys.all { it in setOf("id", "ds", "lang", "on") }) {
                        children += Folder(target.id, title, "${folder.path} / $title")
                    }
                }
            }
        }
        val pages = doc.select("a[href]").mapNotNull { anchor ->
            val target = link(anchor.attr("href"), origin) ?: return@mapNotNull null
            if (target.id != folder.id || target.documentId != null ||
                target.parameters.keys.none { it in pageParameters }) return@mapNotNull null
            target.url
        }.distinct()
        return FolderPage(files.distinctBy { it.documentId }, children.distinctBy { it.id }, pages)
    }

    private val pageParameters = setOf("strana", "page", "offset", "od", "pos", "zacatek")
    fun link(href: String, origin: HttpUrl = base): Link? {
        val url = origin.resolve(href) ?: return null
        if (url.scheme != origin.scheme || url.host != origin.host || url.port != origin.port ||
            url.username.isNotEmpty() || url.password.isNotEmpty() || url.fragment != null ||
            url.encodedPath != "/auth/dok_server/slozka.pl") return null
        // Production origin is HTTPS; tests use a local MockWebServer origin.
        if (origin.host == "is.stuba.sk" && url.scheme != "https") return null
        val parameters = linkedMapOf<String, String>()
        for (part in (url.encodedQuery ?: return null).split(';', '&').filter { it.isNotEmpty() }) {
            val key = decode(part.substringBefore('=')) ?: return null
            val value = decode(part.substringAfter('=', "")) ?: return null
            if (parameters.put(key, value) != null) return null
        }
        val id = parameters["id"]?.takeIf { it.matches(Regex("[1-9][0-9]*")) } ?: return null
        val download = parameters["download"]
        if (download != null && !download.matches(Regex("[1-9][0-9]*"))) return null
        if (parameters.any { (key, value) -> when (key) {
                "id", "download" -> false
                "lang" -> value !in setOf("en", "sk", "cz")
                "ds", "on" -> value !in setOf("0", "1")
                "z" -> download == null || value != "1"
                in pageParameters -> !value.matches(Regex("[0-9]{1,8}"))
                else -> true
            } }) return null
        if (download != null && parameters.keys.any { it !in setOf("id", "download", "lang", "ds", "on", "z") }) return null
        return Link(id, download, url, parameters)
    }
    private fun decode(value: String): String? = runCatching { URLDecoder.decode(value, "UTF-8") }.getOrNull()
    private fun normalize(value: String) = java.text.Normalizer.normalize(value.lowercase(Locale.ROOT), java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").trim()
    private fun mimeType(ext: String?) = when (ext) {
        "pdf", "application/pdf" -> "application/pdf"
        "doc", "application/msword" -> "application/msword"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "ppt" -> "application/vnd.ms-powerpoint"
        "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        "xls" -> "application/vnd.ms-excel"
        "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        "zip" -> "application/zip"
        "txt" -> "text/plain"
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        else -> "application/octet-stream"
    }
    internal fun extension(mime: String): String = when (mime) {
        "application/pdf" -> ".pdf"
        "application/msword" -> ".doc"
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> ".docx"
        "application/vnd.ms-powerpoint" -> ".ppt"
        "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> ".pptx"
        "application/vnd.ms-excel" -> ".xls"
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> ".xlsx"
        "application/zip" -> ".zip"
        "text/plain" -> ".txt"
        "image/png" -> ".png"
        "image/jpeg" -> ".jpg"
        else -> ""
    }
}
