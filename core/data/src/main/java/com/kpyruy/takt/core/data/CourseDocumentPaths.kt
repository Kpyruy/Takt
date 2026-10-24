package com.kpyruy.takt.core.data

internal object CourseDocumentPaths {
    fun courseFolder(code: String) = code.replace(Regex("[^A-Za-z0-9_.-]"), "_")
        .trim('.', '_', '-').take(60).ifBlank { "course" }
    fun fileName(name: String): String {
        val safe = name.filterNot { it == '/' || it == '\\' || it.isISOControl() }.trim().trimStart('.')
        if (safe.isBlank()) return "document"
        if (safe.length <= 150 && safe.toByteArray(Charsets.UTF_8).size <= 180) return safe
        val extension = safe.substringAfterLast('.', "").takeIf { it.matches(Regex("[A-Za-z0-9]{1,12}")) }
        val suffix = extension?.let { ".$it" }.orEmpty()
        var stem = (if (extension == null) safe else safe.dropLast(suffix.length)).take(150 - suffix.length)
        if (stem.lastOrNull()?.isHighSurrogate() == true) stem = stem.dropLast(1)
        while ((stem + suffix).toByteArray(Charsets.UTF_8).size > 180) {
            stem = stem.substring(0, stem.offsetByCodePoints(stem.length, -1))
        }
        return stem + suffix
    }
}
