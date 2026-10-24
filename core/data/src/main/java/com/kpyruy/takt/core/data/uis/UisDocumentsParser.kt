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
    internal data class FolderPage(val files: List<CourseDocumentFile>, val subfolders: List<Folder>, val pages: List<HttpUrl>, val recognized: Boolean,
        val readLinks: Map<String, HttpUrl> = emptyMap())

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
        val readLinks = linkedMapOf<String, HttpUrl>()
        var recognized = false
        for (table in doc.select("table")) {
            val headers = table.select("tr").firstOrNull { it.select("th").isNotEmpty() }
                ?.select("th")?.map { normalize(it.text()) }.orEmpty()
            val nameColumn = headers.indexOfFirst { it in setOf("name", "nazov", "nazev") }
            if (nameColumn < 0) continue
            val childTable = headers.any { it in setOf("enter folder", "vstup do zlozky", "vstup do slozky") }
            if (childTable || headers.any { it in setOf("attachment", "priloha", "download", "subor", "soubor") }) recognized = true
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
                        val revision = headers.mapIndexedNotNull { index, name ->
                            if (name in setOf("document date", "modifications", "last modified", "datum dokumentu", "zmeny", "datum zmeny", "posledna zmena"))
                                cells.getOrNull(index)?.text()?.trim()?.takeIf { it.isNotBlank() } else null
                        }.joinToString(" | ").take(512).takeIf { it.isNotBlank() }
                        files += CourseDocumentFile(folder.id, target.documentId, title, mime,
                            suggested, cells.getOrNull(sizeColumn)?.text()?.takeIf { it.isNotBlank() }, folder.path, revision = revision)
                        row.select("a[href]").firstNotNullOfOrNull {
                            readLink(it.attr("href"), folder.id, target.documentId, origin)
                        }?.let { readLinks[target.documentId] = it }
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
        return FolderPage(files.distinctBy { it.documentId }, children.distinctBy { it.id }, pages, recognized, readLinks)
    }

    /** Read-page signatures stay in the authenticated client, never in portable metadata. */
    fun readLink(href: String, folderId: String, documentId: String, origin: HttpUrl = base): HttpUrl? {
        val url = origin.resolve(href) ?: return null
        if (url.scheme != origin.scheme || url.host != origin.host || url.port != origin.port ||
            url.username.isNotEmpty() || url.password.isNotEmpty() || url.fragment != null ||
            url.encodedPath != "/auth/dok_server/dokumenty_cteni.pl") return null
        val pairs = (url.encodedQuery ?: return null).split(';', '&').filter { it.isNotEmpty() }.map {
            (decode(it.substringBefore('=')) ?: return null) to (decode(it.substringAfter('=', "")) ?: return null)
        }
        if (pairs.map { it.first }.distinct().size != pairs.size) return null
        val values = pairs.toMap()
        if (values["id"] != folderId || values["dok"] != documentId || values.any { (key, value) -> when (key) {
            "id", "dok" -> false
            "on" -> value != "0"
            "lang" -> value !in setOf("en", "sk", "cz")
            "serializace" -> value.isEmpty() || value.length > 1024 || value.any { it.isISOControl() }
            else -> true
        } }) return null
        return url
    }

    fun documentDetails(html: String, expected: CourseDocumentFile): CourseDocumentFile {
        val fields = Jsoup.parse(html).select("tr").mapNotNull { row ->
            val cells = row.children().filter { it.tagName() == "td" }
            if (cells.size == 2) normalize(cells[0].text()).removeSuffix(":") to cells[1] else null
        }.toMap()
        val attachment = fields["attachment"] ?: fields["priloha"] ?: error("UIS document attachment name missing")
        val name = attachment.textNodes().joinToString("") { it.text() }.substringBefore(" [").trim()
        require(name.isNotBlank() && name.length <= 512 && name != "." && name != ".." &&
            name.none { it == '/' || it == '\\' || it.isISOControl() }) { "Invalid UIS attachment name" }
        val title = (fields["name"] ?: fields["nazov"] ?: fields["nazev"])?.text()?.trim()?.takeIf { it.isNotBlank() }
        return expected.copy(fileName = name, title = title?.take(2000) ?: expected.title)
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
