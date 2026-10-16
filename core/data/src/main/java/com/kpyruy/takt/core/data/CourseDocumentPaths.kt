package com.kpyruy.takt.core.data

internal object CourseDocumentPaths {
    fun courseFolder(code: String) = code.replace(Regex("[^A-Za-z0-9_.-]"), "_")
        .trim('.', '_', '-').take(60).ifBlank { "course" }
    fun downloadsPath(code: String) = "Download/Takt/${courseFolder(code)}/"
    fun fileName(name: String): String {
        val safe = name.filterNot { it == '/' || it == '\\' || it.isISOControl() }.trim().trimStart('.')
        if (safe.isBlank()) return "document"
        if (safe.length <= 150) return safe
        val extension = safe.substringAfterLast('.', "").takeIf { it.matches(Regex("[A-Za-z0-9]{1,12}")) }
        return if (extension == null) safe.take(150) else safe.take(149 - extension.length) + "." + extension
    }
}
