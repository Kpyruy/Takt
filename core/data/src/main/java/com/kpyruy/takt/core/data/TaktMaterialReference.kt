package com.kpyruy.takt.core.data

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/** Stable encoded references into Documents/Takt. */
internal object TaktMaterialReference {
    data class Path(val folderName: String, val fileName: String)

    fun encode(folderName: String, fileName: String): String =
        "takt://v2/${segment(folderName)}/${segment(fileName)}"

    fun decode(reference: String): Path? {
        if (!reference.startsWith("takt://v2/")) return null
        val parts = reference.removePrefix("takt://v2/").split('/', limit = 3)
        if (parts.size != 2) return null
        return runCatching {
            Path(URLDecoder.decode(parts[0], StandardCharsets.UTF_8.name()),
                URLDecoder.decode(parts[1], StandardCharsets.UTF_8.name()))
        }.getOrNull()?.takeIf(::valid)
    }

    private fun segment(value: String) = URLEncoder.encode(value, StandardCharsets.UTF_8.name())
    private fun valid(path: Path) = path.folderName.isNotBlank() && path.fileName.isNotBlank() &&
        '/' !in path.folderName && '/' !in path.fileName
}
