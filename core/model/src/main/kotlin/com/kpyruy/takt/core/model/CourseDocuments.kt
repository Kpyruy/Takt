package com.kpyruy.takt.core.model

/** Cached UIS metadata; background downloads only refresh copies the user already saved. */
data class CourseDocumentFile(
    val folderId: String,
    val documentId: String,
    val title: String,
    val mimeType: String,
    val fileName: String? = null,
    val sizeLabel: String? = null,
    val folderPath: String? = null,
    val downloadedFileName: String? = null,
    val revision: String? = null,
    val downloadedRevision: String? = null,
) {
    init {
        require(folderId.matches(Regex("[1-9][0-9]*")))
        require(documentId.matches(Regex("[1-9][0-9]*")))
    }
    val downloadUrl: String get() = "https://is.stuba.sk/auth/dok_server/slozka.pl?id=$folderId;download=$documentId"
    val contentRevision: String? get() = revision?.takeIf { it.isNotBlank() }
        ?: sizeLabel?.takeIf { it.isNotBlank() }?.let { "size:$it" }
    val hasDownloadUpdate: Boolean get() = downloadedRevision != null && contentRevision != null && downloadedRevision != contentRevision
}

/** A discovered course root; only its validated id is used to construct UIS requests. */
data class CourseDocumentFolder(val id: String, val label: String, val path: String) {
    init { require(id.matches(Regex("[1-9][0-9]*"))) }
}

data class CourseDocumentListing(
    val files: List<CourseDocumentFile>,
    val foldersFound: Boolean,
    val folderUrl: String? = null,
    val truncated: Boolean = false,
    val roots: List<CourseDocumentFolder> = emptyList(),
)

data class DownloadedCourseDocument(val fileName: String, val mimeType: String, val byteCount: Long)

/** Compares remote metadata; a saved local filename is not a teacher edit. */
data class CourseDocumentChanges(
    val added: List<CourseDocumentFile> = emptyList(),
    val updated: List<CourseDocumentFile> = emptyList(),
    val removed: List<CourseDocumentFile> = emptyList(),
) {
    val isEmpty get() = added.isEmpty() && updated.isEmpty() && removed.isEmpty()
    companion object {
        fun between(old: CourseDocumentListing, fresh: CourseDocumentListing, changedBytes: Set<String> = emptySet()): CourseDocumentChanges {
            fun key(file: CourseDocumentFile) = "${file.folderId}:${file.documentId}"
            val before = old.files.associateBy(::key); val after = fresh.files.associateBy(::key)
            return CourseDocumentChanges(fresh.files.filter { key(it) !in before }, fresh.files.filter { file ->
                before[key(file)]?.let { it.copy(downloadedFileName = null, downloadedRevision = null) != file.copy(downloadedFileName = null, downloadedRevision = null) || key(file) in changedBytes } ?: false
            }, old.files.filter { key(it) !in after })
        }
    }
}
